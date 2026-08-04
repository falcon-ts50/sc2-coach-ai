#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any


def clock(seconds: float) -> str:
    minutes, secs = divmod(int(seconds), 60)
    return f"{minutes:02d}:{secs:02d}"


def army_losses(stat: dict[str, Any]) -> float:
    return float(stat.get("minerals_lost_army", 0)) + float(stat.get("vespene_lost_army", 0))


def nearest(stats: list[dict[str, Any]], time: float) -> dict[str, Any]:
    return min(stats, key=lambda item: abs(float(item.get("time", 0)) - time)) if stats else {}


def death_events(data: dict[str, Any]) -> list[dict[str, Any]]:
    return [event for event in data.get("timeline", []) if event.get("event") == "UnitDiedEvent"]


def cluster_deaths(events: list[dict[str, Any]], max_gap: float = 18.0, padding: float = 8.0, minimum_deaths: int = 3) -> list[tuple[float, float, list[dict[str, Any]]]]:
    if not events:
        return []
    events = sorted(events, key=lambda event: float(event.get("time", 0)))
    groups: list[list[dict[str, Any]]] = [[events[0]]]
    for event in events[1:]:
        if float(event.get("time", 0)) - float(groups[-1][-1].get("time", 0)) <= max_gap:
            groups[-1].append(event)
        else:
            groups.append([event])
    result = []
    for group in groups:
        if len(group) < minimum_deaths:
            continue
        start = max(0.0, float(group[0].get("time", 0)) - padding)
        end = float(group[-1].get("time", 0)) + padding
        result.append((start, end, group))
    return result


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
            "classification": classify_trade(team_deltas, focus_team),
        })
    return {"schema_version": "0.3.0", "focus_player": focus_player, "focus_team": focus_team, "battles": battles}


def write_markdown(report: dict[str, Any], path: Path) -> None:
    lines = ["# SC2 Coach — battle report", ""]
    for battle in report["battles"]:
        lines += [
            f"## Battle #{battle['id']} — {battle['start_clock']}–{battle['end_clock']}", "",
            f"- Classification: **{battle['classification']}**",
            f"- Death events: **{battle['death_count']}**",
            f"- Duration: **{int(battle['duration'])} s**",
            "- Estimated army-loss delta by player:",
        ]
        for name, value in sorted(battle["player_loss_delta"].items(), key=lambda item: item[1], reverse=True):
            lines.append(f"  - {name}: {int(value)} resources")
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
