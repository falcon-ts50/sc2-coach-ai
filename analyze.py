#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from dataclasses import asdict, dataclass, field
from pathlib import Path
from typing import Any

from transcript import build_transcript_markdown

try:
    import sc2reader
except ImportError as exc:
    raise SystemExit("Missing dependencies. Run: python -m pip install -r requirements.txt") from exc

SCHEMA_VERSION = "0.2.0"
GAME_LOOPS_PER_SECOND = 16.0


def json_safe(value: Any) -> Any:
    if value is None or isinstance(value, (str, int, float, bool)):
        return value
    if isinstance(value, dict):
        return {normalize_key(key): json_safe(item) for key, item in value.items()}
    if isinstance(value, (list, tuple, set, frozenset)):
        return [json_safe(item) for item in value]
    if isinstance(value, Counter):
        return json_safe(dict(value))
    if hasattr(value, "isoformat"):
        try:
            return value.isoformat()
        except (TypeError, ValueError):
            pass
    return str(value)


def normalize_key(key: Any) -> str:
    if isinstance(key, tuple):
        return "(" + ",".join(str(part) for part in key) + ")"
    return str(key)


def seconds_from_frame(frame: int | None) -> float:
    return round((frame or 0) / GAME_LOOPS_PER_SECOND, 2)


def clock(seconds: float) -> str:
    minutes, secs = divmod(int(seconds), 60)
    return f"{minutes:02d}:{secs:02d}"


def normalize_entity_name(value: Any) -> str:
    text = str(value)
    match = re.fullmatch(r"\(([^,]+),\s*\d+\)", text)
    return match.group(1) if match else text


def safe_name(obj: Any) -> str:
    value = getattr(obj, "name", None) or getattr(obj, "title", None) or obj.__class__.__name__
    return normalize_entity_name(value)


def point(value: Any) -> dict[str, float] | None:
    if value is None:
        return None
    if isinstance(value, (tuple, list)) and len(value) >= 2:
        x, y = value[0], value[1]
    else:
        x = getattr(value, "x", None)
        y = getattr(value, "y", None)
        if x is None or y is None:
            nested = getattr(value, "location", None)
            return point(nested) if nested is not value else None
    if isinstance(x, (int, float)) and isinstance(y, (int, float)):
        return {"x": round(float(x), 2), "y": round(float(y), 2)}
    return None


def event_position(event: Any) -> dict[str, float] | None:
    return point(getattr(event, "location", None)) or point(getattr(getattr(event, "unit", None), "location", None))


def command_target_position(event: Any) -> dict[str, float] | None:
    for attr in ("location", "target", "target_point"):
        result = point(getattr(event, attr, None))
        if result:
            return result
    target_unit = getattr(event, "target_unit", None)
    return point(getattr(target_unit, "location", None))


@dataclass
class PlayerReport:
    pid: int
    name: str
    race: str
    team: int | None
    result: str | None
    mmr: int | None
    apm: float | None
    events: Counter[str] = field(default_factory=Counter)
    units_born: Counter[str] = field(default_factory=Counter)
    units_died: Counter[str] = field(default_factory=Counter)
    structures_built: list[dict[str, Any]] = field(default_factory=list)
    upgrades: list[dict[str, Any]] = field(default_factory=list)
    commands: list[dict[str, Any]] = field(default_factory=list)
    stats: list[dict[str, Any]] = field(default_factory=list)
    camera_moves: int = 0
    control_group_events: int = 0
    selection_events: int = 0

    def serializable(self) -> dict[str, Any]:
        data = asdict(self)
        for key in ("events", "units_born", "units_died"):
            data[key] = dict(data[key])
        return data


def owner_pid(event: Any) -> int | None:
    player = getattr(event, "player", None)
    player_pid = getattr(player, "pid", None)
    if isinstance(player_pid, int) and player_pid > 0:
        return player_pid
    for attr in ("control_pid", "upkeep_pid", "pid"):
        value = getattr(event, attr, None)
        if isinstance(value, int) and value > 0:
            return value
    return None


def unit_name(event: Any) -> str:
    unit = getattr(event, "unit", None)
    if unit is not None:
        return safe_name(unit)
    raw = getattr(event, "unit_type_name", None) or getattr(event, "unit_type", None) or "Unknown"
    return normalize_entity_name(raw)


def load_replay(path: Path):
    return sc2reader.load_replay(str(path), load_level=4, load_map=False)


def analyze(path: Path, focus: str | None) -> dict[str, Any]:
    replay = load_replay(path)
    players: dict[int, PlayerReport] = {}
    for player in replay.players:
        pid = int(getattr(player, "pid", 0))
        avg_apm = getattr(player, "avg_apm", None)
        players[pid] = PlayerReport(
            pid=pid,
            name=str(getattr(player, "name", f"Player {pid}")),
            race=str(getattr(player, "play_race", None) or getattr(player, "pick_race", "Unknown")),
            team=getattr(getattr(player, "team", None), "number", None),
            result=getattr(player, "result", None),
            mmr=getattr(player, "mmr", None),
            apm=round(float(avg_apm), 1) if avg_apm is not None else None,
        )

    global_counts: Counter[str] = Counter()
    timeline: list[dict[str, Any]] = []

    for event in replay.events:
        kind = event.__class__.__name__
        global_counts[kind] += 1
        pid = owner_pid(event)
        report = players.get(pid)
        if report:
            report.events[kind] += 1
        event_time = seconds_from_frame(getattr(event, "frame", 0))
        base = {"time": event_time, "clock": clock(event_time), "player": report.name if report else pid, "event": kind}

        if kind in {"UnitBornEvent", "UnitInitEvent", "UnitDoneEvent"}:
            name = unit_name(event)
            if report:
                report.units_born[name] += 1
                if kind in {"UnitInitEvent", "UnitDoneEvent"}:
                    report.structures_built.append({"time": event_time, "clock": clock(event_time), "name": name, "event": kind})
            timeline.append({**base, "unit": name, "position": event_position(event)})
        elif kind == "UnitDiedEvent":
            name = unit_name(event)
            victim_owner = getattr(getattr(event, "unit", None), "owner", None)
            victim_pid = getattr(victim_owner, "pid", None)
            victim = players.get(victim_pid)
            if victim:
                victim.units_died[name] += 1
            killer_pid = getattr(event, "killing_player_id", None)
            killer = players.get(killer_pid).name if killer_pid in players else killer_pid
            timeline.append({**base, "player": killer, "killer": killer, "unit": name,
                             "victim": victim.name if victim else victim_pid, "position": event_position(event)})
        elif kind == "UpgradeCompleteEvent":
            upgrade = str(getattr(event, "upgrade_type_name", None) or getattr(event, "upgrade_type", "Unknown"))
            if report:
                report.upgrades.append({"time": event_time, "clock": clock(event_time), "name": upgrade})
            timeline.append({**base, "upgrade": upgrade})
        elif kind in {"CommandEvent", "BasicCommandEvent", "TargetPointCommandEvent", "TargetUnitCommandEvent"}:
            ability = getattr(event, "ability_name", None) or safe_name(getattr(event, "ability", None))
            target_unit = getattr(event, "target_unit", None)
            command = {
                "time": event_time,
                "clock": clock(event_time),
                "ability": str(ability),
                "target_position": command_target_position(event),
                "target_unit": safe_name(target_unit) if target_unit is not None else None,
            }
            if report:
                report.commands.append(command)
            timeline.append({**base, **command})
        elif kind == "PlayerStatsEvent" and report:
            fields: dict[str, Any] = {}
            for key in (
                "minerals_current", "vespene_current", "minerals_collection_rate", "vespene_collection_rate",
                "workers_active_count", "food_used", "food_made", "army_count",
                "minerals_used_current_army", "vespene_used_current_army",
                "minerals_lost_army", "vespene_lost_army",
            ):
                value = getattr(event, key, None)
                if value is not None:
                    fields[key] = value
            fields.update({"time": event_time, "clock": clock(event_time)})
            report.stats.append(fields)
        elif kind == "CameraEvent" and report:
            report.camera_moves += 1
        elif "ControlGroup" in kind and report:
            report.control_group_events += 1
        elif "SelectionEvent" in kind and report:
            report.selection_events += 1

    focus_player = next((p for p in players.values() if focus and p.name.lower() == focus.lower()), None)
    game_seconds = seconds_from_frame(getattr(replay, "frames", 0))
    real_length = getattr(replay, "real_length", None)

    data = {
        "schema_version": SCHEMA_VERSION,
        "source": str(path),
        "replay": {
            "map": getattr(replay, "map_name", None),
            "date": str(getattr(replay, "date", None)),
            "release": getattr(replay, "release_string", None),
            "build": getattr(replay, "build", None),
            "base_build": getattr(replay, "base_build", None),
            "category": getattr(replay, "category", None),
            "type": getattr(replay, "type", None),
            "speed": getattr(replay, "speed", None),
            "frames": getattr(replay, "frames", None),
            "game_seconds": game_seconds,
            "real_seconds": round(float(real_length.seconds), 2) if real_length else None,
            "winner": [p.name for p in getattr(getattr(replay, "winner", None), "players", [])] if getattr(replay, "winner", None) else [],
        },
        "focus_player": focus_player.name if focus_player else None,
        "players": [p.serializable() for p in players.values()],
        "event_counts": dict(global_counts),
        "timeline": sorted(timeline, key=lambda item: item["time"]),
    }
    data["transcript_markdown"] = build_transcript_markdown(data)
    return data


def write_markdown(data: dict[str, Any], path: Path) -> None:
    replay = data["replay"]
    lines = [
        "# SC2 Coach — replay extraction", "",
        f"- Schema: **{data['schema_version']}**",
        f"- Map: **{replay.get('map')}**",
        f"- Patch/build: **{replay.get('release')} / {replay.get('base_build')}**",
        f"- Mode: **{replay.get('type')}**",
        f"- Duration: **{clock(replay.get('game_seconds') or 0)} game time / {clock(replay.get('real_seconds') or 0)} real time**",
        f"- Recorded winner: **{', '.join(replay.get('winner') or []) or 'unknown'}**", "",
        "## Players", "",
        "| Player | Race | Team | Result | MMR | APM |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    for player in data["players"]:
        lines.append(f"| {player['name']} | {player['race']} | {player['team']} | {player['result']} | {player['mmr']} | {player['apm']} |")
    lines += ["", "## Key build and tech timeline", ""]
    for event in data["timeline"]:
        if event["event"] in {"UnitInitEvent", "UnitDoneEvent", "UpgradeCompleteEvent"}:
            what = event.get("unit") or event.get("upgrade")
            lines.append(f"- `{event['clock']}` **{event.get('player')}** — {what} ({event['event']})")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Decode an SC2Replay into coach-friendly JSON and Markdown")
    parser.add_argument("replay", type=Path)
    parser.add_argument("--player", default=None)
    parser.add_argument("--out", type=Path, default=Path("out"))
    args = parser.parse_args()
    if not args.replay.is_file():
        parser.error(f"Replay not found: {args.replay}")
    args.out.mkdir(parents=True, exist_ok=True)
    data = analyze(args.replay, args.player)
    json_path = args.out / "replay_analysis.json"
    md_path = args.out / "replay_analysis.md"
    transcript_path = args.out / "replay_transcript.md"
    json_path.write_text(json.dumps(json_safe(data), ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(data, md_path)
    transcript_path.write_text(data["transcript_markdown"], encoding="utf-8")
    print(json_path)
    print(md_path)
    print(transcript_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())