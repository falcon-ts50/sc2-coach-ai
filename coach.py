#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any, Callable


def clock(seconds: float) -> str:
    minutes, secs = divmod(int(seconds), 60)
    return f"{minutes:02d}:{secs:02d}"


def nearest(stats: list[dict[str, Any]], time: float) -> dict[str, Any]:
    return min(stats, key=lambda item: abs(float(item.get("time", 0)) - time)) if stats else {}


def army(stat: dict[str, Any]) -> float:
    return float(stat.get("minerals_used_current_army", 0)) + float(stat.get("vespene_used_current_army", 0))


def losses(stat: dict[str, Any]) -> float:
    return float(stat.get("minerals_lost_army", 0)) + float(stat.get("vespene_lost_army", 0))


def bank(stat: dict[str, Any]) -> float:
    return float(stat.get("minerals_current", 0)) + float(stat.get("vespene_current", 0))


def income(stat: dict[str, Any]) -> float:
    return float(stat.get("minerals_collection_rate", 0)) + float(stat.get("vespene_collection_rate", 0))


def intervals(
    stats: list[dict[str, Any]],
    predicate: Callable[[dict[str, Any]], bool],
    minimum_duration: float = 20,
) -> list[tuple[float, float]]:
    result: list[tuple[float, float]] = []
    start: float | None = None
    last: float | None = None
    for stat in stats:
        time = float(stat.get("time", 0))
        hit = predicate(stat)
        if hit and start is None:
            start = time
        if hit:
            last = time
        if not hit and start is not None and last is not None:
            if last - start >= minimum_duration:
                result.append((start, last))
            start = last = None
    if start is not None and last is not None and last - start >= minimum_duration:
        result.append((start, last))
    return result


def summarize_player(player: dict[str, Any]) -> dict[str, Any]:
    stats = player.get("stats", [])
    peak_army = max(stats, key=army) if stats else {}
    peak_workers = max(stats, key=lambda stat: stat.get("workers_active_count", 0)) if stats else {}
    maximum_bank = max(stats, key=bank) if stats else {}
    supply_blocks = intervals(
        stats,
        lambda stat: stat.get("food_made", 0) > 0 and stat.get("food_used", 0) >= stat.get("food_made", 0) - 0.01,
        10,
    )
    mineral_float = intervals(stats, lambda stat: stat.get("minerals_current", 0) >= 1000, 30)
    return {
        "name": player["name"],
        "race": player["race"],
        "team": player["team"],
        "mmr": player.get("mmr"),
        "peak_army_value": army(peak_army),
        "peak_army_time": peak_army.get("time"),
        "peak_workers": peak_workers.get("workers_active_count", 0),
        "peak_workers_time": peak_workers.get("time"),
        "max_bank": bank(maximum_bank),
        "max_bank_time": maximum_bank.get("time"),
        "final_workers": stats[-1].get("workers_active_count", 0) if stats else 0,
        "final_army_value": army(stats[-1]) if stats else 0,
        "final_army_losses": losses(stats[-1]) if stats else 0,
        "supply_blocks": supply_blocks,
        "mineral_float_intervals": mineral_float,
        "camera_moves": player.get("camera_moves", 0),
        "control_group_events": player.get("control_group_events", 0),
        "selection_events": player.get("selection_events", 0),
    }


def team_series(players: list[dict[str, Any]], times: list[int]) -> list[dict[str, Any]]:
    teams: dict[Any, list[dict[str, Any]]] = {}
    for player in players:
        teams.setdefault(player["team"], []).append(player)
    rows: list[dict[str, Any]] = []
    for time in times:
        row: dict[str, Any] = {"time": time, "clock": clock(time), "teams": {}}
        for team, members in teams.items():
            snapshots = [nearest(member.get("stats", []), time) for member in members]
            row["teams"][str(team)] = {
                "army_value": sum(army(snapshot) for snapshot in snapshots),
                "army_losses": sum(losses(snapshot) for snapshot in snapshots),
                "workers": sum(float(snapshot.get("workers_active_count", 0)) for snapshot in snapshots),
                "bank": sum(bank(snapshot) for snapshot in snapshots),
                "income_rate": sum(income(snapshot) for snapshot in snapshots),
                "food_used": sum(float(snapshot.get("food_used", 0)) for snapshot in snapshots),
            }
        rows.append(row)
    return rows


def find_turning_points(rows: list[dict[str, Any]], focus_team: Any) -> list[dict[str, Any]]:
    if not rows:
        return []
    focus_key = str(focus_team)
    opponents = [key for key in rows[0]["teams"] if key != focus_key]
    if not opponents:
        return []
    opponent_key = opponents[0]
    points: list[dict[str, Any]] = []
    previous: float | None = None
    for row in rows:
        difference = row["teams"][focus_key]["army_value"] - row["teams"][opponent_key]["army_value"]
        if previous is not None:
            if previous >= 0 and difference < -1000:
                points.append({"time": row["time"], "type": "army_lead_lost", "difference": difference})
            if difference - previous < -1800:
                points.append({"time": row["time"], "type": "sharp_army_swing", "swing": difference - previous, "difference": difference})
        previous = difference
    deduplicated: list[dict[str, Any]] = []
    for point in points:
        if not deduplicated or point["time"] - deduplicated[-1]["time"] >= 60:
            deduplicated.append(point)
    return deduplicated


def build_report(data: dict[str, Any], focus: str) -> dict[str, Any]:
    players = data["players"]
    focus_player = next((player for player in players if player["name"].lower() == focus.lower()), None)
    if not focus_player:
        raise SystemExit(f"Player not found: {focus}")
    duration = max(
        (stat.get("time", 0) for player in players for stat in player.get("stats", [])),
        default=data["replay"].get("game_seconds", 0) or 0,
    )
    times = list(range(300, int(duration) + 1, 60))
    rows = team_series(players, times)
    summaries = [summarize_player(player) for player in players]
    return {
        "schema_version": "0.1.0",
        "focus_player": focus,
        "focus_team": focus_player["team"],
        "duration": duration,
        "players": summaries,
        "team_timeline": rows,
        "turning_points": find_turning_points(rows, focus_player["team"]),
    }


def write_markdown(source: dict[str, Any], report: dict[str, Any], path: Path) -> None:
    focus = next(player for player in report["players"] if player["name"] == report["focus_player"])
    lines = [
        "# SC2 Coach — coaching report", "",
        f"- Map: **{source['replay'].get('map')}**",
        f"- Player: **{report['focus_player']}**",
        f"- Tracker duration: **{clock(report['duration'])}**", "",
        "## Players", "",
        "| Player | Team | MMR | Peak army | Peak workers | Army losses | Final army/workers |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for player in report["players"]:
        lines.append(
            f"| {player['name']} | {player['team']} | {player['mmr']} | "
            f"{int(player['peak_army_value'])} @ {clock(player['peak_army_time'] or 0)} | "
            f"{player['peak_workers']} @ {clock(player['peak_workers_time'] or 0)} | "
            f"{int(player['final_army_losses'])} | {int(player['final_army_value'])} / {player['final_workers']} |"
        )
    lines += ["", "## Turning points", ""]
    for point in report["turning_points"]:
        if point["type"] == "army_lead_lost":
            lines.append(f"- `{clock(point['time'])}` — army parity was lost; deficit reached about **{abs(int(point['difference']))}** resources.")
        else:
            lines.append(f"- `{clock(point['time'])}` — sharp negative trade; army balance moved by about **{abs(int(point['swing']))}** resources in one minute.")
    if not report["turning_points"]:
        lines.append("- No threshold crossing was detected.")
    lines += ["", "## Automated observations", ""]
    if focus["mineral_float_intervals"]:
        total = sum(end - start for start, end in focus["mineral_float_intervals"])
        lines.append(f"- Minerals exceeded 1000 for at least **{int(total)} seconds** in total.")
    if focus["supply_blocks"]:
        total = sum(end - start for start, end in focus["supply_blocks"])
        lines.append(f"- Full or near-full supply lasted at least **{int(total)} seconds** in total.")
    lines.append(f"- Peak army: **{int(focus['peak_army_value'])}** at `{clock(focus['peak_army_time'] or 0)}`; final army: **{int(focus['final_army_value'])}**.")
    lines.append(f"- Recorded army losses: approximately **{int(focus['final_army_losses'])}** resources.")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate deterministic coaching metrics from replay_analysis.json")
    parser.add_argument("json", type=Path)
    parser.add_argument("--player", required=True)
    parser.add_argument("--out", type=Path, default=Path("out"))
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    source = json.loads(args.json.read_text(encoding="utf-8"))
    report = build_report(source, args.player)
    analysis_path = args.out / "coaching_analysis.json"
    report_path = args.out / "coaching_report.md"
    analysis_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(source, report, report_path)
    print(analysis_path)
    print(report_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
