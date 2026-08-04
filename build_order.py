#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

SCHEMA_VERSION = "0.6.0"

WORKERS = {"SCV", "Drone", "Probe", "MULE"}
SUPPLY = {"SupplyDepot", "Overlord", "Overseer", "Pylon"}
STRUCTURE_HINTS = (
    "CommandCenter", "OrbitalCommand", "PlanetaryFortress", "Barracks", "Factory", "Starport",
    "EngineeringBay", "Armory", "FusionCore", "GhostAcademy", "Bunker", "MissileTurret", "SensorTower",
    "Hatchery", "Lair", "Hive", "SpawningPool", "RoachWarren", "BanelingNest", "HydraliskDen",
    "Spire", "GreaterSpire", "EvolutionChamber", "Extractor", "SpineCrawler", "SporeCrawler",
    "Nexus", "Gateway", "WarpGate", "CyberneticsCore", "RoboticsFacility", "Stargate", "TwilightCouncil",
    "TemplarArchive", "DarkShrine", "FleetBeacon", "Forge", "Assimilator", "PhotonCannon", "ShieldBattery",
)
KEY_UNITS = {
    "Battlecruiser", "Thor", "Tank", "SiegeTank", "Liberator", "Medivac", "Banshee", "Raven",
    "Mutalisk", "Ultralisk", "BroodLord", "Lurker", "Infestor", "Viper",
    "Colossus", "Disruptor", "HighTemplar", "DarkTemplar", "Carrier", "Tempest", "Oracle",
}
WORKER_THRESHOLDS = (20, 30, 40, 50, 60, 70, 80)
SUPPLY_THRESHOLDS = (50, 100, 150, 200)


def clock(seconds: float) -> str:
    value = max(0, int(seconds))
    return f"{value // 60:02d}:{value % 60:02d}"


def is_structure(name: str) -> bool:
    return any(hint in name for hint in STRUCTURE_HINTS)


def event_record(time: float, category: str, name: str, phase: str, ordinal: int) -> dict[str, Any]:
    return {
        "time": round(float(time), 2),
        "clock": clock(float(time)),
        "category": category,
        "name": name,
        "phase": phase,
        "ordinal": ordinal,
        "key": f"{category}:{name}:{ordinal}",
    }


def extract_player(data: dict[str, Any], player_name: str) -> dict[str, Any]:
    player = next((p for p in data.get("players", []) if str(p.get("name", "")).lower() == player_name.lower()), None)
    if player is None:
        raise ValueError(f"Player not found: {player_name}")

    events: list[dict[str, Any]] = []
    ordinals: dict[tuple[str, str], int] = {}
    first_key_units: set[str] = set()

    for raw in sorted(data.get("timeline", []), key=lambda item: float(item.get("time", 0))):
        if str(raw.get("player")) != str(player.get("name")):
            continue
        kind = str(raw.get("event", ""))
        time = float(raw.get("time", 0))
        if kind == "UpgradeCompleteEvent":
            name = str(raw.get("upgrade", "Unknown"))
            key = ("upgrade", name)
            ordinals[key] = ordinals.get(key, 0) + 1
            events.append(event_record(time, "upgrade", name, "complete", ordinals[key]))
            continue
        name = str(raw.get("unit", "Unknown"))
        if name in WORKERS or name in SUPPLY:
            continue
        if is_structure(name) and kind in {"UnitInitEvent", "UnitDoneEvent"}:
            category = "structure"
            phase = "start" if kind == "UnitInitEvent" else "complete"
            key = (category, name)
            if phase == "start":
                ordinals[key] = ordinals.get(key, 0) + 1
            ordinal = ordinals.get(key, 1)
            events.append(event_record(time, category, name, phase, ordinal))
        elif name in KEY_UNITS and kind in {"UnitBornEvent", "UnitDoneEvent"} and name not in first_key_units:
            first_key_units.add(name)
            events.append(event_record(time, "key_unit", name, "first", 1))

    milestones: list[dict[str, Any]] = []
    seen_workers: set[int] = set()
    seen_supply: set[int] = set()
    for stat in sorted(player.get("stats", []), key=lambda item: float(item.get("time", 0))):
        time = float(stat.get("time", 0))
        workers = int(stat.get("workers_active_count", 0) or 0)
        food = int(float(stat.get("food_used", 0) or 0))
        for threshold in WORKER_THRESHOLDS:
            if workers >= threshold and threshold not in seen_workers:
                seen_workers.add(threshold)
                milestones.append({"time": time, "clock": clock(time), "metric": "workers", "threshold": threshold, "value": workers})
        for threshold in SUPPLY_THRESHOLDS:
            if food >= threshold and threshold not in seen_supply:
                seen_supply.add(threshold)
                milestones.append({"time": time, "clock": clock(time), "metric": "supply_used", "threshold": threshold, "value": food})

    return {
        "schema_version": SCHEMA_VERSION,
        "focus_player": player.get("name"),
        "race": player.get("race"),
        "replay": {
            "map": data.get("replay", {}).get("map"),
            "release": data.get("replay", {}).get("release"),
            "base_build": data.get("replay", {}).get("base_build"),
            "type": data.get("replay", {}).get("type"),
        },
        "events": sorted(events, key=lambda item: (item["time"], item["category"], item["name"])),
        "milestones": sorted(milestones, key=lambda item: (item["time"], item["metric"])),
    }


def write_markdown(model: dict[str, Any], path: Path) -> None:
    lines = [
        "# SC2 Coach — normalized build order", "",
        f"- Player: **{model['focus_player']}**",
        f"- Race: **{model.get('race')}**",
        f"- Schema: **{model['schema_version']}**", "",
        "## Build and tech events", "",
        "| Time | Category | Event | Phase | # |",
        "|---:|---|---|---|---:|",
    ]
    for event in model["events"]:
        lines.append(f"| {event['clock']} | {event['category']} | {event['name']} | {event['phase']} | {event['ordinal']} |")
    lines += ["", "## Economic milestones", "", "| Time | Metric | Threshold | Observed |", "|---:|---|---:|---:|"]
    for item in model["milestones"]:
        lines.append(f"| {item['clock']} | {item['metric']} | {item['threshold']} | {item['value']} |")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract a normalized build-order model from replay_analysis.json")
    parser.add_argument("replay_json", type=Path)
    parser.add_argument("--player", required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    data = json.loads(args.replay_json.read_text(encoding="utf-8"))
    model = extract_player(data, args.player)
    json_path = args.out / "build_order.json"
    md_path = args.out / "build_order.md"
    json_path.write_text(json.dumps(model, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(model, md_path)
    print(json_path)
    print(md_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
