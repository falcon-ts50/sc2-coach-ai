#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from bisect import bisect_right
from pathlib import Path
from statistics import mean
from typing import Any

SCHEMA_VERSION = "0.6.0"
CHECKPOINT_SECONDS = 60
WEIGHTS = {
    "economy": 0.30,
    "army": 0.35,
    "efficiency": 0.20,
    "development": 0.15,
}


def clock(seconds: float) -> str:
    value = max(0, int(seconds))
    return f"{value // 60:02d}:{value % 60:02d}"


def number(value: Any) -> float:
    try:
        return float(value or 0)
    except (TypeError, ValueError):
        return 0.0


def stat_value(stat: dict[str, Any], key: str) -> float:
    return number(stat.get(key))


def latest_stat(stats: list[dict[str, Any]], times: list[float], at: float) -> dict[str, Any] | None:
    index = bisect_right(times, at) - 1
    return stats[index] if index >= 0 else None


def minmax(values: dict[str, float]) -> dict[str, float]:
    if not values:
        return {}
    low, high = min(values.values()), max(values.values())
    if high - low < 1e-9:
        return {name: 0.5 for name in values}
    return {name: (value - low) / (high - low) for name, value in values.items()}


def player_snapshot(stat: dict[str, Any]) -> dict[str, float]:
    minerals_rate = stat_value(stat, "minerals_collection_rate")
    gas_rate = stat_value(stat, "vespene_collection_rate")
    army_value = stat_value(stat, "minerals_used_current_army") + stat_value(stat, "vespene_used_current_army")
    army_losses = stat_value(stat, "minerals_lost_army") + stat_value(stat, "vespene_lost_army")
    workers = stat_value(stat, "workers_active_count")
    supply_used = stat_value(stat, "food_used")
    supply_cap = max(supply_used, stat_value(stat, "food_made"))
    supply_saturation = supply_used / supply_cap if supply_cap > 0 else 0.0
    return {
        "workers": workers,
        "income_rate": minerals_rate + 1.25 * gas_rate,
        "army_value": army_value,
        "army_losses": army_losses,
        "supply_saturation": supply_saturation,
    }


def development_score(build: dict[str, Any] | None, at: float) -> float:
    if not build:
        return 0.0
    events = [item for item in build.get("events", []) if number(item.get("time")) <= at]
    completed_structures = sum(1 for item in events if item.get("category") == "structure" and item.get("phase") == "complete")
    upgrades = sum(1 for item in events if item.get("category") == "upgrade")
    key_units = sum(1 for item in events if item.get("category") == "key_unit")
    # Counts are deliberately race-agnostic. They indicate breadth of completed development,
    # not direct equivalence between specific Terran, Zerg and Protoss objects.
    return completed_structures + 1.5 * upgrades + 1.25 * key_units


def compare_match(data: dict[str, Any], builds: dict[str, dict[str, Any]] | None = None) -> dict[str, Any]:
    players = [p for p in data.get("players", []) if p.get("stats")]
    if len(players) < 2:
        raise ValueError("At least two players with statistics are required")

    prepared: dict[str, tuple[dict[str, Any], list[dict[str, Any]], list[float]]] = {}
    for player in players:
        stats = sorted(player.get("stats", []), key=lambda item: number(item.get("time")))
        prepared[str(player.get("name"))] = (player, stats, [number(item.get("time")) for item in stats])

    game_seconds = number(data.get("replay", {}).get("game_seconds"))
    if game_seconds <= 0:
        game_seconds = max(times[-1] for _, _, times in prepared.values() if times)
    checkpoints = list(range(CHECKPOINT_SECONDS, int(game_seconds) + 1, CHECKPOINT_SECONDS))
    if not checkpoints or checkpoints[-1] < game_seconds - 20:
        checkpoints.append(int(game_seconds))

    histories: dict[str, list[dict[str, Any]]] = {name: [] for name in prepared}
    cumulative: dict[str, dict[str, list[float]]] = {
        name: {dimension: [] for dimension in WEIGHTS} for name in prepared
    }

    for at in checkpoints:
        snapshots: dict[str, dict[str, float]] = {}
        for name, (_, stats, times) in prepared.items():
            stat = latest_stat(stats, times, at)
            if stat is not None:
                snapshots[name] = player_snapshot(stat)
        if len(snapshots) < 2:
            continue

        economy_raw = {name: snap["workers"] * 0.45 + snap["income_rate"] * 0.55 / 100 for name, snap in snapshots.items()}
        army_raw = {name: snap["army_value"] for name, snap in snapshots.items()}
        efficiency_raw = {
            name: snap["army_value"] / max(500.0, snap["army_losses"] + snap["army_value"])
            for name, snap in snapshots.items()
        }
        development_raw = {
            name: development_score((builds or {}).get(name), at) + snapshots[name]["supply_saturation"]
            for name in snapshots
        }
        normalized = {
            "economy": minmax(economy_raw),
            "army": minmax(army_raw),
            "efficiency": minmax(efficiency_raw),
            "development": minmax(development_raw),
        }

        for name, snap in snapshots.items():
            dimensions = {dimension: round(normalized[dimension][name] * 100, 1) for dimension in WEIGHTS}
            score = sum(dimensions[dimension] * WEIGHTS[dimension] for dimension in WEIGHTS)
            for dimension, value in dimensions.items():
                cumulative[name][dimension].append(value)
            histories[name].append({
                "time": at,
                "clock": clock(at),
                "score": round(score, 1),
                "dimensions": dimensions,
                "raw": {key: round(value, 2) for key, value in snap.items()},
            })

    ranking: list[dict[str, Any]] = []
    for name, (player, _, _) in prepared.items():
        dimensions = {
            dimension: round(mean(values), 1) if values else 0.0
            for dimension, values in cumulative[name].items()
        }
        overall = round(sum(dimensions[d] * WEIGHTS[d] for d in WEIGHTS), 1)
        ranking.append({
            "player": name,
            "race": player.get("race"),
            "team": player.get("team"),
            "result": player.get("result"),
            "score": overall,
            "dimensions": dimensions,
            "checkpoint_count": len(histories[name]),
        })
    ranking.sort(key=lambda item: (-item["score"], str(item["player"])))
    for index, item in enumerate(ranking, 1):
        item["rank"] = index

    leader = ranking[0]
    runner_up = ranking[1]
    gap = round(leader["score"] - runner_up["score"], 1)
    confidence = "high" if len(checkpoints) >= 8 and gap >= 8 else "medium" if len(checkpoints) >= 4 and gap >= 3 else "low"

    return {
        "schema_version": SCHEMA_VERSION,
        "method": "within_match_race_aware_relative_scoring",
        "normalization": {
            "description": "Players are normalized against opponents at synchronized checkpoints using race-neutral outcomes. Specific structures and unit costs are not directly equated across races.",
            "checkpoint_seconds": CHECKPOINT_SECONDS,
            "weights": WEIGHTS,
        },
        "replay": data.get("replay", {}),
        "leader": {
            "player": leader["player"],
            "score": leader["score"],
            "gap_to_second": gap,
            "confidence": confidence,
        },
        "ranking": ranking,
        "timelines": histories,
        "caveats": [
            "The score identifies the strongest measured in-match performance, not the morally or strategically correct player.",
            "Support roles, deliberate sacrifices, scouting value and positional control are only partially represented.",
            "A low-confidence lead should be reported as a close match rather than a definitive leader.",
        ],
    }


def write_markdown(model: dict[str, Any], path: Path) -> None:
    leader = model["leader"]
    lines = [
        "# SC2 Coach — in-match player comparison", "",
        f"- Measured leader: **{leader['player']}**",
        f"- Score: **{leader['score']}**",
        f"- Gap to second: **{leader['gap_to_second']}**",
        f"- Confidence: **{leader['confidence']}**", "",
        "## Ranking", "",
        "| Rank | Player | Race | Team | Score | Economy | Army | Efficiency | Development |",
        "|---:|---|---|---:|---:|---:|---:|---:|---:|",
    ]
    for item in model["ranking"]:
        d = item["dimensions"]
        lines.append(
            f"| {item['rank']} | {item['player']} | {item.get('race')} | {item.get('team')} | {item['score']} | "
            f"{d['economy']} | {d['army']} | {d['efficiency']} | {d['development']} |"
        )
    lines += ["", "## Interpretation limits", ""]
    lines.extend(f"- {item}" for item in model["caveats"])
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare all replay participants using race-aware within-match normalization")
    parser.add_argument("replay_json", type=Path)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    data = json.loads(args.replay_json.read_text(encoding="utf-8"))
    builds: dict[str, dict[str, Any]] = {}
    build_path = args.out / "build_orders_all.json"
    if build_path.exists():
        raw = json.loads(build_path.read_text(encoding="utf-8"))
        builds = {str(item.get("focus_player")): item for item in raw.get("players", [])}
    model = compare_match(data, builds)
    json_path = args.out / "match_comparison.json"
    md_path = args.out / "match_comparison.md"
    json_path.write_text(json.dumps(model, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(model, md_path)
    print(json_path)
    print(md_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
