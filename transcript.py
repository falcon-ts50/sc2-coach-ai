from __future__ import annotations

from collections import defaultdict
from typing import Any


def build_transcript_markdown(data: dict[str, Any]) -> str:
    replay = data.get("replay") or {}
    lines = [
        "# SC2 Coach — AI-readable replay transcript",
        "",
        "> This document separates replay facts from derived deltas. Missing positions are not reconstructed.",
        "",
        "## Match",
        "",
        f"- Map: **{replay.get('map') or 'unknown'}**",
        f"- Mode: **{replay.get('type') or 'unknown'}**",
        f"- Patch/build: **{replay.get('release') or 'unknown'} / {replay.get('base_build') or 'unknown'}**",
        f"- Duration: **{_clock(replay.get('game_seconds') or 0)}** game time",
        f"- Recorded winner: **{', '.join(replay.get('winner') or []) or 'unknown'}**",
        "",
        "## Players",
        "",
        "| PID | Player | Race | Team | Result | MMR | APM |",
        "|---:|---|---|---:|---|---:|---:|",
    ]
    for player in data.get("players") or []:
        lines.append(
            f"| {player.get('pid')} | {player.get('name')} | {player.get('race')} | "
            f"{player.get('team')} | {player.get('result')} | {player.get('mmr')} | {player.get('apm')} |"
        )

    lines.extend(["", "## Chronological event transcript", ""])
    timeline = sorted(data.get("timeline") or [], key=lambda item: item.get("time") or 0)
    for event in timeline:
        rendered = _render_event(event)
        if rendered:
            lines.append(rendered)

    lines.extend(["", "## Periodic player-state snapshots and deltas", ""])
    for player in data.get("players") or []:
        stats = sorted(player.get("stats") or [], key=lambda item: item.get("time") or 0)
        if not stats:
            continue
        lines.extend([f"### {player.get('name')}", ""])
        previous: dict[str, Any] | None = None
        for snapshot in stats:
            absolute = _absolute_snapshot(snapshot)
            delta = _snapshot_delta(previous, snapshot)
            line = f"- `{snapshot.get('clock') or _clock(snapshot.get('time') or 0)}` — {absolute}"
            if delta:
                line += f"; Δ since previous: {delta}"
            lines.append(line)
            previous = snapshot
        lines.append("")

    lines.extend([
        "## Interpretation contract",
        "",
        "- Event rows are decoder facts unless explicitly labelled otherwise.",
        "- Snapshot deltas are deterministic arithmetic between adjacent `PlayerStatsEvent` records.",
        "- A command target point is an issued target, not proof that a unit reached that point.",
        "- Unit positions are included only when the replay library exposes them for that event.",
        "- This transcript does not infer hidden information, intent, pathing, or continuous unit trajectories.",
    ])
    return "\n".join(lines)


def _render_event(event: dict[str, Any]) -> str | None:
    kind = event.get("event")
    player = event.get("player")
    prefix = f"- `{event.get('clock') or _clock(event.get('time') or 0)}` **{player or 'unknown'}** — "
    position = _position_suffix(event.get("position"))
    target_position = _position_suffix(event.get("target_position"), "target ")

    if kind in {"UnitBornEvent", "UnitInitEvent", "UnitDoneEvent"}:
        return f"{prefix}{kind}: {event.get('unit') or 'Unknown'}{position}"
    if kind == "UnitDiedEvent":
        victim = event.get("victim") or "unknown owner"
        killer = event.get("killer") or player or "unknown killer"
        return f"{prefix}{event.get('unit') or 'Unknown'} died; owner {victim}; killer {killer}{position}"
    if kind == "UpgradeCompleteEvent":
        return f"{prefix}upgrade completed: {event.get('upgrade') or 'Unknown'}"
    if kind in {"CommandEvent", "BasicCommandEvent", "TargetPointCommandEvent", "TargetUnitCommandEvent"}:
        target = event.get("target_unit")
        target_text = f"; target unit {target}" if target else ""
        return f"{prefix}command: {event.get('ability') or 'Unknown'}{target_text}{target_position}"
    return None


def _absolute_snapshot(snapshot: dict[str, Any]) -> str:
    fields = [
        ("workers_active_count", "workers"),
        ("food_used", "supply used"),
        ("food_made", "supply cap"),
        ("minerals_current", "minerals"),
        ("vespene_current", "gas"),
        ("minerals_collection_rate", "mineral rate"),
        ("vespene_collection_rate", "gas rate"),
        ("minerals_used_current_army", "army minerals"),
        ("vespene_used_current_army", "army gas"),
        ("minerals_lost_army", "army minerals lost"),
        ("vespene_lost_army", "army gas lost"),
    ]
    values = [f"{label}={_number(snapshot.get(key))}" for key, label in fields if snapshot.get(key) is not None]
    return ", ".join(values) or "no supported metrics"


def _snapshot_delta(previous: dict[str, Any] | None, current: dict[str, Any]) -> str:
    if previous is None:
        return ""
    fields = [
        ("workers_active_count", "workers"),
        ("food_used", "supply used"),
        ("minerals_current", "minerals"),
        ("vespene_current", "gas"),
        ("minerals_collection_rate", "mineral rate"),
        ("vespene_collection_rate", "gas rate"),
        ("minerals_used_current_army", "army minerals"),
        ("vespene_used_current_army", "army gas"),
        ("minerals_lost_army", "army minerals lost"),
        ("vespene_lost_army", "army gas lost"),
    ]
    changes: list[str] = []
    for key, label in fields:
        before = previous.get(key)
        after = current.get(key)
        if not isinstance(before, (int, float)) or not isinstance(after, (int, float)):
            continue
        delta = after - before
        if delta:
            changes.append(f"{label}={delta:+g}")
    return ", ".join(changes)


def _position_suffix(value: Any, label: str = "at ") -> str:
    if not isinstance(value, dict):
        return ""
    x, y = value.get("x"), value.get("y")
    if not isinstance(x, (int, float)) or not isinstance(y, (int, float)):
        return ""
    return f"; {label}({x:.2f}, {y:.2f})"


def _number(value: Any) -> str:
    if isinstance(value, float):
        return f"{value:g}"
    return str(value)


def _clock(seconds: float) -> str:
    minutes, secs = divmod(int(float(seconds)), 60)
    return f"{minutes:02d}:{secs:02d}"
