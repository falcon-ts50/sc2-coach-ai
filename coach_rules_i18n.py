#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

import coach_rules

EN_TEXT = {
    "macro.mineral_float": (
        "Extended mineral float",
        "Minerals remained above 1000 for at least {seconds} seconds. Part of the economy was not converted into army, production, or expansion.",
        "Check the bank after each engagement. Above 1000 minerals, add production or start rebuilding the army immediately.",
    ),
    "macro.supply_block": (
        "Significant supply blocks",
        "Full or nearly full supply persisted for at least {seconds} seconds.",
        "Start the next supply source roughly 15–20 supply before the cap and check supply whenever new production is added.",
    ),
    "macro.army_not_recovered": (
        "Army was not rebuilt after losses",
        "Peak army value was {peak}, while only {final} remained at the end—less than one quarter of the peak.",
        "After a heavy trade, switch to recovery mode: retreat, preserve expensive survivors, produce from all facilities, and avoid the next fight until critical mass is restored.",
    ),
    "combat.cascade": (
        "Cascade of catastrophic engagements",
        "{count} catastrophic engagements were detected. After the first defeat, the team entered another major fight before fully recovering.",
        "After the first catastrophic trade, call a full reset: regroup, rebuild workers and army, and do not move out separately.",
    ),
    "economy.worker_harass": (
        "Severe worker damage",
        "The focus player lost {workers} workers in one engagement.",
        "After the first mobile threat appears, maintain route vision, keep minimal static defense near the mineral line, and predefine a worker evacuation point.",
    ),
    "teamwork.lead_conversion": (
        "Team army lead was not converted",
        "The team led by roughly {before} army resources but was behind by {after} at the end of the next interval.",
        "When the team gains a clear lead, agree on one objective: attack together, destroy a base, or consolidate safely. Do not spend the lead in separate fights.",
    ),
}


def evidence_value(item: dict[str, Any], metric: str, default: Any = 0) -> Any:
    for evidence in item.get("evidence", []):
        if evidence.get("metric") == metric:
            return evidence.get("value", default)
    return default


def localize(report: dict[str, Any], lang: str) -> dict[str, Any]:
    report["language"] = lang
    report["disclaimer"] = (
        "Recommendations are deterministic heuristics derived from replay metrics; they do not infer hidden intent or unseen unit positions."
        if lang == "en" else
        "Рекомендации основаны на детерминированных эвристиках по данным реплея; они не приписывают игрокам скрытые намерения и не предполагают невидимые позиции юнитов."
    )
    if lang == "ru":
        return report
    for item in report.get("findings", []):
        rule_id = str(item.get("rule_id", ""))
        if rule_id.startswith("combat.trade."):
            margin = evidence_value(item, "trade_margin")
            start = item.get("time_start_clock", "unknown")
            end = item.get("time_end_clock", "unknown")
            item["title"] = f"Unfavorable engagement at {start}"
            item["explanation"] = f"The focus team lost approximately {int(margin)} more resources than the opponents during {start}–{end}."
            item["recommendation"] = "Before the next major contact, compare allied army strength, stop the fight after losing key counter-units, and preserve a retreat route."
            continue
        text = EN_TEXT.get(rule_id)
        if not text:
            continue
        title, explanation, recommendation = text
        values = {
            "seconds": evidence_value(item, "mineral_float_seconds", evidence_value(item, "supply_block_seconds", 0)),
            "peak": evidence_value(item, "peak_army_value"),
            "final": evidence_value(item, "final_army_value"),
            "count": evidence_value(item, "catastrophic_engagement_count"),
            "workers": evidence_value(item, "workers_lost"),
            "before": evidence_value(item, "army_lead_before"),
            "after": evidence_value(item, "army_deficit_after"),
        }
        item["title"] = title
        item["explanation"] = explanation.format(**values)
        item["recommendation"] = recommendation
    return report


def write_markdown(report: dict[str, Any], path: Path, lang: str) -> None:
    ru = lang == "ru"
    lines = [
        "# SC2 Coach — стратегические рекомендации" if ru else "# SC2 Coach — strategic recommendations",
        "",
        f"- {'Игрок' if ru else 'Player'}: **{report.get('focus_player')}**",
        f"- {'Метод' if ru else 'Method'}: **{report.get('method')}**",
        f"- {'Выводов' if ru else 'Findings'}: **{report.get('finding_count')}**",
        "",
        f"> {report.get('disclaimer')}",
        "",
    ]
    severity_ru = {"critical": "критическая", "high": "высокая", "medium": "средняя", "low": "низкая"}
    category_ru = {"macro": "макро", "recovery": "восстановление", "combat": "бой", "decision_making": "решения", "economy_defense": "защита экономики", "teamwork": "командная игра"}
    for index, item in enumerate(report.get("findings", []), 1):
        window = ""
        if item.get("time_start_clock"):
            window = f" — `{item['time_start_clock']}"
            if item.get("time_end_clock"):
                window += f"–{item['time_end_clock']}"
            window += "`"
        lines += [
            f"## {index}. {item['title']}{window}", "",
            f"- {'Важность' if ru else 'Severity'}: **{severity_ru.get(item['severity'], item['severity']) if ru else item['severity']}**",
            f"- {'Категория' if ru else 'Category'}: **{category_ru.get(item['category'], item['category']) if ru else item['category']}**",
            f"- {'Правило' if ru else 'Rule'}: `{item['rule_id']}`", "",
            item["explanation"], "",
            f"**{'Рекомендация' if ru else 'Recommendation'}:** {item['recommendation']}", "",
            f"**{'Доказательства' if ru else 'Evidence'}:**",
        ]
        for evidence in item.get("evidence", []):
            lines.append(f"- `{evidence.get('metric')}`: `{json.dumps(evidence.get('value'), ensure_ascii=False)}`")
        lines.append("")
    if not report.get("findings"):
        lines.append("Ни одно правило не превысило настроенный порог." if ru else "No coaching rule crossed its configured threshold.")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate localized explainable strategic recommendations")
    parser.add_argument("--coaching", type=Path, required=True)
    parser.add_argument("--battles", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--lang", choices=["en", "ru"], default="en")
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    report = coach_rules.build(coach_rules.load(args.coaching), coach_rules.load(args.battles))
    report = localize(report, args.lang)
    json_path = args.out / "strategic_analysis.json"
    md_path = args.out / "strategic_report.md"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(report, md_path, args.lang)
    print(json_path)
    print(md_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
