#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def clock(seconds: float) -> str:
    minutes, secs = divmod(int(seconds), 60)
    return f"{minutes:02d}:{secs:02d}"


def render_coaching(source: dict[str, Any], report: dict[str, Any], lang: str) -> str:
    ru = lang == "ru"
    lines = [
        "# SC2 Coach — тренерский отчёт" if ru else "# SC2 Coach — coaching report",
        "",
        f"- {'Карта' if ru else 'Map'}: **{source['replay'].get('map')}**",
        f"- {'Игрок' if ru else 'Player'}: **{report['focus_player']}**",
        f"- {'Продолжительность' if ru else 'Tracker duration'}: **{clock(report['duration'])}**",
        "",
        "## Игроки" if ru else "## Players",
        "",
        "| Игрок | Команда | MMR | Пик армии | Пик рабочих | Потери армии | Армия/рабочие в конце |" if ru else "| Player | Team | MMR | Peak army | Peak workers | Army losses | Final army/workers |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ]
    for player in report.get("players", []):
        lines.append(
            f"| {player['name']} | {player['team']} | {player.get('mmr')} | "
            f"{int(player.get('peak_army_value', 0))} @ {clock(player.get('peak_army_time') or 0)} | "
            f"{player.get('peak_workers', 0)} @ {clock(player.get('peak_workers_time') or 0)} | "
            f"{int(player.get('final_army_losses', 0))} | {int(player.get('final_army_value', 0))} / {player.get('final_workers', 0)} |"
        )
    lines += ["", "## Переломные моменты" if ru else "## Turning points", ""]
    for point in report.get("turning_points", []):
        if point.get("type") == "army_lead_lost":
            text = (
                f"- `{clock(point['time'])}` — преимущество по армии потеряно; дефицит достиг примерно **{abs(int(point['difference']))}** ресурсов."
                if ru else
                f"- `{clock(point['time'])}` — army parity was lost; deficit reached about **{abs(int(point['difference']))}** resources."
            )
        else:
            text = (
                f"- `{clock(point['time'])}` — резкий отрицательный размен; баланс армии изменился примерно на **{abs(int(point['swing']))}** ресурсов за минуту."
                if ru else
                f"- `{clock(point['time'])}` — sharp negative trade; army balance moved by about **{abs(int(point['swing']))}** resources in one minute."
            )
        lines.append(text)
    if not report.get("turning_points"):
        lines.append("- Пороговые переломы не обнаружены." if ru else "- No threshold crossing was detected.")
    return "\n".join(lines) + "\n"


def render_battles(report: dict[str, Any], lang: str) -> str:
    ru = lang == "ru"
    type_ru = {"battle": "бой", "skirmish": "стычка", "worker_harass": "харас рабочих", "base_assault": "атака базы", "minor_contact": "малый контакт"}
    outcome_ru = {"catastrophic_loss": "катастрофическое поражение", "lost": "проигран", "even": "равный", "won": "выигран", "decisive_win": "уверенная победа", "economic_damage": "экономический урон", "minor_contact": "малый контакт"}
    lines = ["# SC2 Coach — отчёт по боевым эпизодам" if ru else "# SC2 Coach — engagement report", ""]
    for item in report.get("battles", []):
        kind = item.get("engagement_type", "engagement")
        outcome = item.get("classification", "unknown")
        lines += [
            f"## #{item.get('id')} — {item.get('start_clock')}–{item.get('end_clock')}", "",
            f"- {'Тип' if ru else 'Type'}: **{type_ru.get(kind, kind) if ru else kind}**",
            f"- {'Результат' if ru else 'Outcome'}: **{outcome_ru.get(outcome, outcome) if ru else outcome}**",
            f"- {'Значимых смертей' if ru else 'Meaningful death events'}: **{item.get('death_count', 0)}**",
            f"- {'Длительность' if ru else 'Duration'}: **{int(item.get('duration', 0))} {'с' if ru else 's'}**",
            f"- {'Оценка потерь армии по игрокам' if ru else 'Estimated army-loss delta by player'}:",
        ]
        for name, value in sorted(item.get("player_loss_delta", {}).items(), key=lambda x: x[1], reverse=True):
            lines.append(f"  - {name}: {int(value)} {'ресурсов' if ru else 'resources'}")
        lines.append(f"- {'Профиль потерь' if ru else 'Loss profile'}:")
        for name, values in item.get("loss_categories", {}).items():
            lines.append(f"  - {name}: " + ", ".join(f"{k}={v}" for k, v in sorted(values.items())))
        lines.append("")
    return "\n".join(lines)


def render_summary(battle: dict[str, Any], coaching: dict[str, Any], strategic: dict[str, Any] | None, diagnostics: dict[str, Any], lang: str) -> str:
    ru = lang == "ru"
    lines = ["# SC2 Coach — сводка" if ru else "# SC2 Coach — review summary", ""]
    lines += [
        f"- {'Игрок' if ru else 'Focus player'}: **{battle.get('focus_player')}**",
        f"- {'Диагностика' if ru else 'Diagnostics'}: **{diagnostics.get('status')}** ({diagnostics.get('warning_count', 0)} {'предупреждений' if ru else 'warnings'})",
        f"- {'Количество эпизодов' if ru else 'Engagement count'}: **{len(battle.get('battles', []))}**",
        f"- {'Стратегических выводов' if ru else 'Strategic findings'}: **{strategic.get('finding_count', 0) if strategic else 0}**",
        "",
        "## Макро выбранного игрока" if ru else "## Focus-player macro snapshot",
        "",
    ]
    focus = coaching.get("focus_player")
    player = next((p for p in coaching.get("players", []) if p.get("name") == focus), None)
    if player:
        lines += [
            f"- {'Пиковая армия' if ru else 'Peak army'}: **{int(player.get('peak_army_value', 0))}**",
            f"- {'Армия в конце' if ru else 'Final army'}: **{int(player.get('final_army_value', 0))}**",
            f"- {'Рабочие в конце' if ru else 'Final workers'}: **{int(player.get('final_workers', 0))}**",
            f"- {'Потери армии' if ru else 'Recorded army losses'}: **{int(player.get('final_army_losses', 0))}**",
        ]
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--replay", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--lang", choices=["en", "ru"], default="en")
    args = parser.parse_args()
    source = load(args.replay)
    coaching = load(args.out / "coaching_analysis.json")
    battles = load(args.out / "battle_analysis.json")
    diagnostics = load(args.out / "diagnostics.json") if (args.out / "diagnostics.json").exists() else {"status": "unknown", "warning_count": 0}
    strategic = load(args.out / "strategic_analysis.json") if (args.out / "strategic_analysis.json").exists() else None
    (args.out / "coaching_report.md").write_text(render_coaching(source, coaching, args.lang), encoding="utf-8")
    (args.out / "battle_report.md").write_text(render_battles(battles, args.lang), encoding="utf-8")
    (args.out / "review_summary.md").write_text(render_summary(battles, coaching, strategic, diagnostics, args.lang), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
