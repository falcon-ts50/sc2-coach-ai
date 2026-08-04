#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any

from battles import (
    IGNORED_UNITS,
    STATIC_DEFENSE,
    WORKERS,
    army_losses,
    classify_trade,
    clock,
    nearest,
)

ECONOMY_STRUCTURES = {
    "CommandCenter", "OrbitalCommand", "Hatchery", "Lair", "Hive", "Nexus",
    "Refinery", "Extractor", "Assimilator", "SupplyDepot", "Overlord",
    "Barracks", "Factory", "Starport", "BarracksReactor", "BarracksTechLab",
    "FactoryReactor", "FactoryTechLab", "StarportReactor", "StarportTechLab",
    "EngineeringBay", "Armory", "FusionCore", "SpawningPool", "RoachWarren",
    "HydraliskDen", "Spire", "EvolutionChamber", "Gateway", "CyberneticsCore",
    "RoboticsFacility", "Stargate", "Forge",
}
ZERG_MORPH_STRUCTURES = {
    "Hatchery", "Extractor", "SpawningPool", "RoachWarren", "HydraliskDen",
    "Spire", "EvolutionChamber", "BanelingNest", "InfestationPit", "NydusNetwork",
    "SpineCrawler", "SporeCrawler",
}


def meaningful_events(data: dict[str, Any]) -> list[dict[str, Any]]:
    timeline = data.get("timeline", [])
    morph_starts: dict[str, list[float]] = {}
    for event in timeline:
        if event.get("event") in {"UnitInitEvent", "UnitDoneEvent"} and event.get("unit") in ZERG_MORPH_STRUCTURES:
            morph_starts.setdefault(str(event.get("player")), []).append(float(event.get("time", 0)))

    result: list[dict[str, Any]] = []
    for event in timeline:
        if event.get("event") != "UnitDiedEvent":
            continue
        unit = str(event.get("unit", "Unknown"))
        victim = str(event.get("victim"))
        if victim in {"None", "null"} or unit in IGNORED_UNITS:
            continue
        if unit == "Drone" and any(abs(float(event.get("time", 0)) - t) <= 1.5 for t in morph_starts.get(victim, [])):
            continue
        result.append(event)
    return sorted(result, key=lambda e: float(e.get("time", 0)))


def category(unit: str) -> str:
    if unit in WORKERS:
        return "worker"
    if unit in STATIC_DEFENSE:
        return "static_defense"
    if unit in ECONOMY_STRUCTURES:
        return "economy_structure"
    return "army"


def is_candidate(events: list[dict[str, Any]]) -> bool:
    counts = Counter(category(str(e.get("unit", "Unknown"))) for e in events)
    return counts["army"] >= 2 or counts["worker"] >= 3 or counts["static_defense"] >= 2


def core_groups(events: list[dict[str, Any]], max_gap: float = 14.0, max_core_duration: float = 83.0) -> list[list[dict[str, Any]]]:
    if not events:
        return []
    groups: list[list[dict[str, Any]]] = [[events[0]]]
    start = float(events[0].get("time", 0))
    for event in events[1:]:
        t = float(event.get("time", 0))
        last = float(groups[-1][-1].get("time", 0))
        if t - last > max_gap or t - start > max_core_duration:
            groups.append([event])
            start = t
        else:
            groups[-1].append(event)
    return [g for g in groups if is_candidate(g)]


def windows(events: list[dict[str, Any]], padding: float = 6.0) -> list[tuple[float, float, list[dict[str, Any]]]]:
    raw = []
    for group in core_groups(events):
        raw.append([
            max(0.0, float(group[0].get("time", 0)) - padding),
            float(group[-1].get("time", 0)) + padding,
            group,
        ])
    for i in range(len(raw) - 1):
        if raw[i][1] > raw[i + 1][0]:
            midpoint = (float(raw[i][-1][-1].get("time", 0)) + float(raw[i + 1][-1][0].get("time", 0))) / 2
            raw[i][1] = midpoint
            raw[i + 1][0] = midpoint
    return [(float(a), float(b), g) for a, b, g in raw]


def build(data: dict[str, Any], focus_player: str) -> dict[str, Any]:
    players = {p["name"]: p for p in data.get("players", [])}
    if focus_player not in players:
        raise SystemExit(f"Player not found: {focus_player}")
    focus_team = str(players[focus_player].get("team"))
    output = []
    for idx, (start, end, events) in enumerate(windows(meaningful_events(data)), 1):
        player_deltas: dict[str, float] = {}
        team_deltas: dict[str, float] = {}
        units_lost: dict[str, Counter[str]] = {}
        categories: dict[str, Counter[str]] = {}
        for name, player in players.items():
            before = nearest(player.get("stats", []), start)
            after = nearest(player.get("stats", []), end)
            delta = max(0.0, army_losses(after) - army_losses(before))
            player_deltas[name] = delta
            team = str(player.get("team"))
            team_deltas[team] = team_deltas.get(team, 0.0) + delta
        for event in events:
            victim = str(event.get("victim"))
            unit = str(event.get("unit", "Unknown"))
            units_lost.setdefault(victim, Counter())[unit] += 1
            categories.setdefault(victim, Counter())[category(unit)] += 1
        output.append({
            "id": idx,
            "start": round(start, 2), "end": round(end, 2),
            "start_clock": clock(start), "end_clock": clock(end),
            "duration": round(end - start, 2), "death_count": len(events),
            "player_loss_delta": player_deltas, "team_loss_delta": team_deltas,
            "units_lost": {k: dict(v) for k, v in units_lost.items()},
            "loss_categories": {k: dict(v) for k, v in categories.items()},
            "classification": classify_trade(team_deltas, focus_team),
        })
    return {
        "schema_version": "0.3.2",
        "focus_player": focus_player,
        "focus_team": focus_team,
        "detector": {"max_gap": 14.0, "max_window_duration": 95.0, "morph_filter_seconds": 1.5},
        "battles": output,
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
        for name, value in sorted(battle["player_loss_delta"].items(), key=lambda x: x[1], reverse=True):
            lines.append(f"  - {name}: {int(value)} resources")
        lines.append("- Loss profile:")
        for name, values in battle["loss_categories"].items():
            lines.append(f"  - {name}: " + ", ".join(f"{k}={v}" for k, v in sorted(values.items())))
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("json", type=Path)
    parser.add_argument("--player", required=True)
    parser.add_argument("--out", type=Path, default=Path("out"))
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    report = build(json.loads(args.json.read_text(encoding="utf-8")), args.player)
    (args.out / "battle_analysis.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(report, args.out / "battle_report.md")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
