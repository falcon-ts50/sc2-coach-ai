#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any

IGNORED_UNITS = {
    "Larva", "Egg", "Broodling", "MULE", "MineralField", "MineralField750",
    "BeaconArmy", "BeaconDefend", "BeaconAttack", "BeaconHarass", "BeaconIdle",
    "BeaconAuto", "BeaconDetect", "BeaconScout", "BeaconClaim", "BeaconExpand",
    "BeaconRally", "BeaconCustom1", "BeaconCustom2", "BeaconCustom3", "BeaconCustom4",
    "CreepTumor", "CreepTumorBurrowed", "CreepTumorQueen", "XelNagaTower",
}
WORKERS = {"SCV", "Drone", "Probe"}
STATIC_DEFENSE = {
    "MissileTurret", "SpineCrawler", "SporeCrawler", "PhotonCannon",
    "PlanetaryFortress", "Bunker", "ShieldBattery",
}


def clock(seconds: float) -> str:
    minutes, secs = divmod(int(seconds), 60)
    return f"{minutes:02d}:{secs:02d}"


def army_losses(stat: dict[str, Any]) -> float:
    return float(stat.get("minerals_lost_army", 0)) + float(stat.get("vespene_lost_army", 0))


def nearest(stats: list[dict[str, Any]], time: float) -> dict[str, Any]:
    return min(stats, key=lambda item: abs(float(item.get("time", 0)) - time)) if stats else {}


def death_events(data: dict[str, Any]) -> list[dict[str, Any]]:
    return [event for event in data.get("timeline", []) if event.get("event") == "UnitDiedEvent"]


def meaningful_death(event: dict[str, Any]) -> bool:
    unit = str(event.get("unit", "Unknown"))
    victim = event.get("victim")
    return victim not in (None, "None") and unit not in IGNORED_UNITS


def split_long_group(group: list[dict[str, Any]], max_duration: float) -> list[list[dict[str, Any]]]:
    if not group:
        return []
    chunks: list[list[dict[str, Any]]] = []
    current = [group[0]]
    chunk_start = float(group[0].get("time", 0))
    for event in group[1:]:
        time = float(event.get("time", 0))
        if time - chunk_start > max_duration:
            chunks.append(current)
            current = [event]
            chunk_start = time
        else:
            current.append(event)
    chunks.append(current)
    return chunks


def candidate_group(group: list[dict[str, Any]], minimum_deaths: int) -> bool:
    if len(group) < minimum_deaths:
        return False
    units = [str(event.get("unit", "Unknown")) for event in group]
    combat_deaths = sum(unit not in WORKERS and unit not in STATIC_DEFENSE for unit in units)
    worker_deaths = sum(unit in WORKERS for unit in units)
    static_deaths = sum(unit in STATIC_DEFENSE for unit in units)
    return combat_deaths >= 2 or worker_deaths >= 3 or static_deaths >= 2


def cluster_deaths(
    events: list[dict[str, Any]],
    max_gap: float = 14.0,
    padding: float = 6.0,
    minimum_deaths: int = 3,
    max_duration: float = 95.0,
) -> list[tuple[float, float, list[dict[str, Any]]]]:
    filtered = sorted((event for event in events if meaningful_death(event)), key=lambda event: float(event.get("time", 0)))
    if not filtered:
        return []
    groups: list[list[dict[str, Any]]] = [[filtered[0]]]
    for event in filtered[1:]:
        if float(event.get("time", 0)) - float(groups[-1][-1].get("time", 0)) <= max_gap:
            groups[-1].append(event)
        else:
            groups.append([event])

    windows: list[tuple[float, float, list[dict[str, Any]]]] = []
    for group in groups:
        for chunk in split_long_group(group, max_duration):
            if not candidate_group(chunk, minimum_deaths):
                continue
            start = max(0.0, float(chunk[0].get("time", 0)) - padding)
            end = float(chunk[-1].get("time", 0)) + padding
            windows.append((start, end, chunk))
    return windows


def classify_trade(team_deltas: dict[str, float], focus_team: str) -> str:
    opponents = [team for team in team_deltas if team != focus_team]
    if not opponents:
        return "unknown"
    focus = team_deltas.get(focus_team, 0)
    enemy = sum(team_deltas[team] for team in opponents)
    margin = focus - enemy
    if margin >= 1500:
        return "catastrophic_loss"
    if margin >= 500:
        return "lost"
    if margin <= -1500:
        return "decisive_win"
    if margin <= -500:
        return "won"
    return "even"


def loss_categories(events: list[dict[str, Any]]) -> dict[str, dict[str, int]]:
    result: dict[str, Counter[str]] = {}
    for event in events:
        victim = str(event.get("victim"))
        unit = str(event.get("unit", "Unknown"))
        category = "worker" if unit in WORKERS else "static_defense" if unit in STATIC_DEFENSE else "army"
        result.setdefault(victim, Counter())[category] += 1
    return {name: dict(counter) for name, counter in result.items()}


def build_battles(data: dict[str, Any], focus_player: str) -> dict[str, Any]:
    players = {player["name"]: player for player in data.get("players", [])}
    focus = players.get(focus_player)
    if not focus:
        raise SystemExit(f"Player not found: {focus_player}")
    focus_team = str(focus.get("team"))
    windows = cluster_deaths(death_events(data))
    battles: list[dict[str, Any]] = []
    for index, (start, end, events) in enumerate(windows, 1):
        player_deltas: dict[str, float] = {}
        team_deltas: dict[str, float] = {}
        units_lost: dict[str, Counter[str]] = {}
        for name, player in players.items():
            before = nearest(player.get("stats", []), start)
            after = nearest(player.get("stats", []), end)
            delta = max(0.0, army_losses(after) - army_losses(before))
            player_deltas[name] = delta
            team = str(player.get("team"))
            team_deltas[team] = team_deltas.get(team, 0.0) + delta
        for event in events:
            victim = str(event.get("victim"))
            units_lost.setdefault(victim, Counter())[str(event.get("unit", "Unknown"))] += 1
        battles.append({
            "id": index,
            "start": round(start, 2),
            "end": round(end, 2),
            "start_clock": clock(start),
            "end_clock": clock(end),
            "duration": round(end - start, 2),
            "death_count": len(events),
            "player_loss_delta": player_deltas,
            "team_loss_delta": team_deltas,
            "units_lost": {name: dict(counter) for name, counter in units_lost.items()},
            "loss_categories": loss_categories(events),
            "classification": classify_trade(team_deltas, focus_team),
        })
    return {
        "schema_version": "0.3.1",
        "focus_player": focus_player,
        "focus_team": focus_team,
        "detector": {"max_gap": 14.0, "max_duration": 95.0, "ignored_units": sorted(IGNORED_UNITS)},
        "battles": battles,
    }


def write_markdown(report: dict[str, Any], path: Path) -> None:
    lines = ["# SC2 Coach — battle report", ""]
    for battle in report["battles"]:
        lines += [
            f"## Battle #{battle['id']} — {battle['start_clock']}–{battle['end_clock']}", "",
            f"- Classification: **{battle['classification']}**",
            f"- Meaningful death events: **{battle['death_count']}**",
            f"- Duration: **{int(battle['duration'])} s**",
            "- Estimated army-loss delta by player:",
        ]
        for name, value in sorted(battle["player_loss_delta"].items(), key=lambda item: item[1], reverse=True):
            lines.append(f"  - {name}: {int(value)} resources")
        lines.append("- Loss profile:")
        for name, categories in battle["loss_categories"].items():
            summary = ", ".join(f"{key}={value}" for key, value in sorted(categories.items()))
            lines.append(f"  - {name}: {summary}")
        lines.append("")
    if not report["battles"]:
        lines.append("No battle windows met the configured thresholds.")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Detect battle windows from replay_analysis.json")
    parser.add_argument("json", type=Path)
    parser.add_argument("--player", required=True)
    parser.add_argument("--out", type=Path, default=Path("out"))
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    data = json.loads(args.json.read_text(encoding="utf-8"))
    report = build_battles(data, args.player)
    json_path = args.out / "battle_analysis.json"
    md_path = args.out / "battle_report.md"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(report, md_path)
    print(json_path)
    print(md_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
