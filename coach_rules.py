#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

SEVERITY_ORDER = {"critical": 0, "high": 1, "medium": 2, "low": 3}


def clock(seconds: float) -> str:
    minutes, secs = divmod(int(seconds), 60)
    return f"{minutes:02d}:{secs:02d}"


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def focus_player(coaching: dict[str, Any]) -> dict[str, Any]:
    name = coaching.get("focus_player")
    player = next((item for item in coaching.get("players", []) if item.get("name") == name), None)
    if not player:
        raise SystemExit(f"Focus player not found in coaching analysis: {name}")
    return player


def focus_team_margin(engagement: dict[str, Any], focus_team: str) -> float:
    losses = engagement.get("team_loss_delta", {})
    own = float(losses.get(focus_team, 0))
    enemy = sum(float(value) for team, value in losses.items() if str(team) != focus_team)
    return own - enemy


def finding(
    rule_id: str,
    category: str,
    severity: str,
    title: str,
    explanation: str,
    recommendation: str,
    evidence: list[dict[str, Any]],
    time_start: float | None = None,
    time_end: float | None = None,
) -> dict[str, Any]:
    result: dict[str, Any] = {
        "rule_id": rule_id,
        "category": category,
        "severity": severity,
        "title": title,
        "explanation": explanation,
        "recommendation": recommendation,
        "evidence": evidence,
    }
    if time_start is not None:
        result["time_start"] = round(time_start, 2)
        result["time_start_clock"] = clock(time_start)
    if time_end is not None:
        result["time_end"] = round(time_end, 2)
        result["time_end_clock"] = clock(time_end)
    return result


def macro_rules(player: dict[str, Any]) -> list[dict[str, Any]]:
    results: list[dict[str, Any]] = []
    floats = player.get("mineral_float_intervals", [])
    total_float = sum(float(end) - float(start) for start, end in floats)
    if total_float >= 120:
        longest = max(floats, key=lambda item: float(item[1]) - float(item[0]))
        results.append(finding(
            "macro.mineral_float",
            "macro",
            "high" if total_float >= 240 else "medium",
            "Длительное накопление минералов",
            f"Минералы держались выше 1000 не менее {int(total_float)} секунд. Это означает, что часть экономики не превращалась в армию, производство или новые базы.",
            "Во время игры проверяй банк после каждого боя и при значении выше 1000 добавляй производство либо запускай восполнение армии.",
            [{"metric": "mineral_float_seconds", "value": int(total_float)}, {"metric": "longest_interval", "value": [clock(longest[0]), clock(longest[1])]}],
            float(longest[0]), float(longest[1]),
        ))

    supply = player.get("supply_blocks", [])
    total_supply = sum(float(end) - float(start) for start, end in supply)
    if total_supply >= 20:
        results.append(finding(
            "macro.supply_block",
            "macro",
            "medium",
            "Заметные блокировки лимита",
            f"Полный или почти полный лимит сохранялся не менее {int(total_supply)} секунд.",
            "Начинай следующий источник supply примерно за 15–20 лимита до капа и ставь отдельный hotkey/ритуал проверки supply после постановки производства.",
            [{"metric": "supply_block_seconds", "value": int(total_supply)}, {"metric": "interval_count", "value": len(supply)}],
        ))

    peak = float(player.get("peak_army_value", 0))
    final = float(player.get("final_army_value", 0))
    if peak >= 1500 and final <= peak * 0.25:
        results.append(finding(
            "macro.army_not_recovered",
            "recovery",
            "high",
            "Армия не была восстановлена после потерь",
            f"Пиковая стоимость армии составляла {int(peak)}, а к концу осталось {int(final)} — менее четверти пика.",
            "После тяжёлого размена переходи в режим восстановления: отступление, сохранение оставшихся дорогих юнитов, одновременный заказ с нескольких производств и отказ от следующего боя до возвращения критической массы.",
            [{"metric": "peak_army_value", "value": int(peak)}, {"metric": "final_army_value", "value": int(final)}],
            float(player.get("peak_army_time") or 0), None,
        ))
    return results


def engagement_rules(battles: dict[str, Any]) -> list[dict[str, Any]]:
    results: list[dict[str, Any]] = []
    focus_team = str(battles.get("focus_team"))
    engagements = battles.get("battles", [])
    negative = [item for item in engagements if focus_team_margin(item, focus_team) >= 500]
    catastrophic = [item for item in engagements if item.get("classification") == "catastrophic_loss"]

    for item in sorted(negative, key=lambda event: focus_team_margin(event, focus_team), reverse=True)[:3]:
        margin = focus_team_margin(item, focus_team)
        results.append(finding(
            f"combat.trade.{item.get('id')}",
            "combat",
            "critical" if margin >= 1500 else "high",
            f"Невыгодный {item.get('engagement_type', 'engagement')} на {item.get('start_clock')}",
            f"Команда выбранного игрока потеряла примерно на {int(margin)} ресурсов больше соперников в окне {item.get('start_clock')}–{item.get('end_clock')}.",
            "Перед следующим крупным контактом сравни армии союзников, избегай продолжения боя после потери ключевых контрюнитов и заранее определи маршрут отхода.",
            [
                {"metric": "trade_margin", "value": int(margin)},
                {"metric": "team_loss_delta", "value": item.get("team_loss_delta", {})},
                {"metric": "focus_player_units_lost", "value": item.get("units_lost", {}).get(battles.get("focus_player"), {})},
            ],
            float(item.get("start", 0)), float(item.get("end", 0)),
        ))

    if len(catastrophic) >= 2:
        first, last = catastrophic[0], catastrophic[-1]
        results.append(finding(
            "combat.cascade",
            "decision_making",
            "critical",
            "Каскад последовательных катастрофических разменов",
            f"Зафиксировано {len(catastrophic)} катастрофических эпизода. После первого поражения команда снова вступала в крупные бои до полного восстановления.",
            "После первого катастрофического размена объявляй общий reset: не выходить по одному, собрать новую армию, восстановить рабочих и только затем принимать следующий бой.",
            [{"metric": "catastrophic_engagement_count", "value": len(catastrophic)}, {"metric": "engagement_ids", "value": [item.get("id") for item in catastrophic]}],
            float(first.get("start", 0)), float(last.get("end", 0)),
        ))

    worker_harass = [item for item in engagements if item.get("engagement_type") == "worker_harass"]
    damaging = []
    focus = battles.get("focus_player")
    for item in worker_harass:
        lost = item.get("loss_categories", {}).get(focus, {}).get("worker", 0)
        if lost >= 4:
            damaging.append((item, lost))
    if damaging:
        item, lost = max(damaging, key=lambda pair: pair[1])
        results.append(finding(
            "economy.worker_harass",
            "economy_defense",
            "high",
            "Серьёзный урон рабочим",
            f"В одном эпизоде потеряно {lost} рабочих выбранного игрока.",
            "После первого появления мобильной угрозы держи обзор маршрутов, оставляй минимальную статическую защиту у минеральной линии и заранее назначай точку эвакуации рабочих.",
            [{"metric": "workers_lost", "value": lost}, {"metric": "engagement_id", "value": item.get("id")}],
            float(item.get("start", 0)), float(item.get("end", 0)),
        ))
    return results


def teamwork_rules(coaching: dict[str, Any]) -> list[dict[str, Any]]:
    results: list[dict[str, Any]] = []
    focus_team = str(coaching.get("focus_team"))
    rows = coaching.get("team_timeline", [])
    large_swings = []
    previous_diff: float | None = None
    for row in rows:
        teams = row.get("teams", {})
        opponents = [key for key in teams if str(key) != focus_team]
        if not opponents or focus_team not in teams:
            continue
        diff = float(teams[focus_team].get("army_value", 0)) - sum(float(teams[key].get("army_value", 0)) for key in opponents)
        if previous_diff is not None and previous_diff > 1000 and diff < 0:
            large_swings.append((row, previous_diff, diff))
        previous_diff = diff
    if large_swings:
        row, before, after = large_swings[0]
        results.append(finding(
            "teamwork.lead_conversion",
            "teamwork",
            "high",
            "Командное преимущество не было конвертировано",
            f"Команда имела преимущество армии около {int(before)} ресурсов, но к {row.get('clock')} уже оказалась позади на {abs(int(after))}.",
            "Когда команда получает заметное преимущество, договоритесь об одной цели: совместная атака, уничтожение базы или безопасное закрепление. Не расходуйте преимущество отдельными боями.",
            [{"metric": "army_lead_before", "value": int(before)}, {"metric": "army_deficit_after", "value": abs(int(after))}],
            float(row.get("time", 0)) - 60, float(row.get("time", 0)),
        ))
    return results


def build(coaching: dict[str, Any], battles: dict[str, Any]) -> dict[str, Any]:
    player = focus_player(coaching)
    findings = macro_rules(player) + engagement_rules(battles) + teamwork_rules(coaching)
    findings.sort(key=lambda item: (SEVERITY_ORDER.get(item["severity"], 99), item.get("time_start", 1e12)))
    return {
        "schema_version": "0.4.0",
        "focus_player": coaching.get("focus_player"),
        "focus_team": coaching.get("focus_team"),
        "method": "deterministic_rule_engine",
        "disclaimer": "Recommendations are deterministic heuristics derived from replay metrics; they are not a substitute for reviewing unit positions and player intent.",
        "finding_count": len(findings),
        "findings": findings,
    }


def write_markdown(report: dict[str, Any], path: Path) -> None:
    lines = [
        "# SC2 Coach — strategic recommendations",
        "",
        f"- Player: **{report.get('focus_player')}**",
        f"- Method: **{report.get('method')}**",
        f"- Findings: **{report.get('finding_count')}**",
        "",
        "> Recommendations are explainable heuristics based on replay evidence. They do not infer hidden intent or unseen unit positioning.",
        "",
    ]
    for index, item in enumerate(report.get("findings", []), 1):
        window = ""
        if item.get("time_start_clock"):
            window = f" — `{item['time_start_clock']}"
            if item.get("time_end_clock"):
                window += f"–{item['time_end_clock']}"
            window += "`"
        lines += [
            f"## {index}. {item['title']}{window}",
            "",
            f"- Severity: **{item['severity']}**",
            f"- Category: **{item['category']}**",
            f"- Rule: `{item['rule_id']}`",
            "",
            item["explanation"],
            "",
            f"**Recommendation:** {item['recommendation']}",
            "",
            "**Evidence:**",
        ]
        for evidence in item.get("evidence", []):
            lines.append(f"- `{evidence.get('metric')}`: `{json.dumps(evidence.get('value'), ensure_ascii=False)}`")
        lines.append("")
    if not report.get("findings"):
        lines.append("No coaching rule crossed its configured threshold.")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate explainable strategic recommendations")
    parser.add_argument("--coaching", type=Path, required=True)
    parser.add_argument("--battles", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    report = build(load(args.coaching), load(args.battles))
    json_path = args.out / "strategic_analysis.json"
    md_path = args.out / "strategic_report.md"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(report, md_path)
    print(json_path)
    print(md_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
