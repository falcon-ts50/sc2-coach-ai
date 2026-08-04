#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any, Iterable

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    Image,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)

PAGE_WIDTH, PAGE_HEIGHT = A4
ACCENT = colors.HexColor("#4F7CFF")
ACCENT_DARK = colors.HexColor("#243B75")
INK = colors.HexColor("#182033")
MUTED = colors.HexColor("#64748B")
PANEL = colors.HexColor("#F2F5FA")
GRID = colors.HexColor("#D8E0EC")
GOOD = colors.HexColor("#238B57")
WARN = colors.HexColor("#C27A16")
BAD = colors.HexColor("#B8323D")

FONT_CANDIDATES = (
    (Path("/usr/share/fonts/TTF/DejaVuSans.ttf"), Path("/usr/share/fonts/TTF/DejaVuSans-Bold.ttf")),
    (Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"), Path("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf")),
)

TEXT = {
    "en": {
        "title": "SC2 Coach Match Report",
        "subtitle": "Explainable replay analysis",
        "generated": "Generated from an explicitly supplied replay",
        "summary": "Match summary",
        "focus_player": "Focus player",
        "map": "Map",
        "duration": "Game time",
        "result": "Recorded result",
        "engagements": "Engagements",
        "findings": "Strategic findings",
        "warnings": "Diagnostics warnings",
        "macro": "Focus-player macro",
        "peak_army": "Peak army",
        "final_army": "Final army",
        "final_workers": "Final workers",
        "army_losses": "Army losses",
        "priority": "Priority coaching actions",
        "evidence": "Evidence",
        "recommendation": "Recommended action",
        "combat": "Engagement review",
        "id": "#",
        "time": "Time",
        "type": "Type",
        "outcome": "Outcome",
        "margin": "Trade margin",
        "charts": "Performance charts",
        "chart_army_value": "Army value over time",
        "chart_workers": "Active workers",
        "chart_bank": "Unspent resources",
        "chart_income_rate": "Resource collection rate",
        "chart_army_losses": "Cumulative army losses",
        "no_findings": "No coaching rule crossed its threshold.",
        "no_engagements": "No engagements were detected.",
        "footer": "SC2 Coach AI - deterministic replay evidence and explainable coaching rules",
        "unknown": "unknown",
    },
    "ru": {
        "title": "Отчёт SC2 Coach по матчу",
        "subtitle": "Объяснимый анализ реплея",
        "generated": "Создан после явной загрузки реплея пользователем",
        "summary": "Сводка матча",
        "focus_player": "Анализируемый игрок",
        "map": "Карта",
        "duration": "Игровое время",
        "result": "Результат в реплее",
        "engagements": "Боевые эпизоды",
        "findings": "Стратегические выводы",
        "warnings": "Предупреждения диагностики",
        "macro": "Макропоказатели игрока",
        "peak_army": "Пиковая армия",
        "final_army": "Армия в конце",
        "final_workers": "Рабочие в конце",
        "army_losses": "Потери армии",
        "priority": "Приоритетные рекомендации",
        "evidence": "Доказательства",
        "recommendation": "Что делать",
        "combat": "Разбор боевых эпизодов",
        "id": "№",
        "time": "Время",
        "type": "Тип",
        "outcome": "Итог",
        "margin": "Разница размена",
        "charts": "Графики матча",
        "chart_army_value": "Стоимость армии во времени",
        "chart_workers": "Количество активных рабочих",
        "chart_bank": "Накопленные ресурсы",
        "chart_income_rate": "Темп добычи ресурсов",
        "chart_army_losses": "Накопленные потери армии",
        "no_findings": "Ни одно тренерское правило не превысило порог.",
        "no_engagements": "Боевые эпизоды не обнаружены.",
        "footer": "SC2 Coach AI - факты реплея и объяснимые тренерские правила",
        "unknown": "неизвестно",
    },
}

CHARTS = (
    ("army_value.png", "chart_army_value"),
    ("workers.png", "chart_workers"),
    ("bank.png", "chart_bank"),
    ("income_rate.png", "chart_income_rate"),
    ("army_losses.png", "chart_army_losses"),
)


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def register_fonts() -> tuple[str, str]:
    for regular, bold in FONT_CANDIDATES:
        if regular.exists() and bold.exists():
            pdfmetrics.registerFont(TTFont("SC2Sans", str(regular)))
            pdfmetrics.registerFont(TTFont("SC2Sans-Bold", str(bold)))
            return "SC2Sans", "SC2Sans-Bold"
    raise FileNotFoundError(
        "DejaVu Sans fonts were not found. Install the dejavu-fonts package "
        "to generate English and Russian PDF reports."
    )


def clock(seconds: Any) -> str:
    try:
        value = max(0, int(float(seconds)))
    except (TypeError, ValueError):
        return "-"
    return f"{value // 60:02d}:{value % 60:02d}"


def clean(value: Any, fallback: str = "-") -> str:
    if value is None or value == "":
        return fallback
    return str(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def severity_color(severity: str) -> colors.Color:
    return {"critical": BAD, "high": BAD, "medium": WARN, "low": GOOD}.get(severity, MUTED)


def trade_margin(engagement: dict[str, Any], focus_team: str) -> float:
    losses = engagement.get("team_loss_delta", {})
    focus = float(losses.get(focus_team, 0) or 0)
    enemy = sum(float(value or 0) for team, value in losses.items() if str(team) != focus_team)
    return focus - enemy


def metric(value: Any) -> str:
    try:
        number = float(value)
    except (TypeError, ValueError):
        return clean(value)
    if abs(number) >= 1000:
        return f"{number:,.0f}".replace(",", " ")
    return f"{number:.0f}" if number.is_integer() else f"{number:.1f}"


class MetricCard(Flowable):
    def __init__(self, label: str, value: str, width: float = 42 * mm, height: float = 22 * mm):
        super().__init__()
        self.label = label
        self.value = value
        self.width = width
        self.height = height

    def draw(self) -> None:
        canvas = self.canv
        canvas.setFillColor(PANEL)
        canvas.roundRect(0, 0, self.width, self.height, 4 * mm, fill=1, stroke=0)
        canvas.setFillColor(MUTED)
        canvas.setFont("SC2Sans", 8)
        canvas.drawString(4 * mm, self.height - 7 * mm, self.label[:30])
        canvas.setFillColor(INK)
        canvas.setFont("SC2Sans-Bold", 15)
        canvas.drawString(4 * mm, 5 * mm, self.value[:24])


def styles() -> dict[str, ParagraphStyle]:
    base = getSampleStyleSheet()
    return {
        "title": ParagraphStyle("title", parent=base["Title"], fontName="SC2Sans-Bold", fontSize=27, leading=31, textColor=colors.white, alignment=TA_LEFT, spaceAfter=4 * mm),
        "subtitle": ParagraphStyle("subtitle", parent=base["Normal"], fontName="SC2Sans", fontSize=12, leading=16, textColor=colors.HexColor("#DCE6FF")),
        "h1": ParagraphStyle("h1", parent=base["Heading1"], fontName="SC2Sans-Bold", fontSize=18, leading=22, textColor=ACCENT_DARK, spaceBefore=4 * mm, spaceAfter=4 * mm),
        "h2": ParagraphStyle("h2", parent=base["Heading2"], fontName="SC2Sans-Bold", fontSize=13, leading=17, textColor=INK, spaceBefore=3 * mm, spaceAfter=2 * mm),
        "body": ParagraphStyle("body", parent=base["BodyText"], fontName="SC2Sans", fontSize=9.3, leading=13, textColor=INK, spaceAfter=2 * mm),
        "small": ParagraphStyle("small", parent=base["BodyText"], fontName="SC2Sans", fontSize=7.8, leading=10.5, textColor=MUTED),
        "card_title": ParagraphStyle("card_title", parent=base["Heading3"], fontName="SC2Sans-Bold", fontSize=11, leading=14, textColor=INK, spaceAfter=1.5 * mm),
        "card_body": ParagraphStyle("card_body", parent=base["BodyText"], fontName="SC2Sans", fontSize=8.6, leading=12, textColor=INK),
        "caption": ParagraphStyle("caption", parent=base["BodyText"], fontName="SC2Sans-Bold", fontSize=10, leading=13, textColor=ACCENT_DARK, alignment=TA_CENTER, spaceAfter=3 * mm),
    }


def page_header_footer(canvas: Any, doc: BaseDocTemplate, language: str) -> None:
    t = TEXT[language]
    canvas.saveState()
    if doc.page > 1:
        canvas.setStrokeColor(GRID)
        canvas.line(18 * mm, PAGE_HEIGHT - 14 * mm, PAGE_WIDTH - 18 * mm, PAGE_HEIGHT - 14 * mm)
        canvas.setFont("SC2Sans", 7.5)
        canvas.setFillColor(MUTED)
        canvas.drawString(18 * mm, PAGE_HEIGHT - 10.5 * mm, t["title"])
    canvas.setStrokeColor(GRID)
    canvas.line(18 * mm, 13 * mm, PAGE_WIDTH - 18 * mm, 13 * mm)
    canvas.setFont("SC2Sans", 7)
    canvas.setFillColor(MUTED)
    canvas.drawString(18 * mm, 8.5 * mm, t["footer"])
    canvas.drawRightString(PAGE_WIDTH - 18 * mm, 8.5 * mm, str(doc.page))
    canvas.restoreState()


def cover(canvas: Any, doc: BaseDocTemplate, language: str, metadata: dict[str, str]) -> None:
    t = TEXT[language]
    canvas.saveState()
    canvas.setFillColor(ACCENT_DARK)
    canvas.rect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, fill=1, stroke=0)
    canvas.setFillColor(ACCENT)
    canvas.circle(PAGE_WIDTH - 25 * mm, PAGE_HEIGHT - 27 * mm, 38 * mm, fill=1, stroke=0)
    canvas.setFillColor(colors.HexColor("#6F94FF"))
    canvas.circle(PAGE_WIDTH - 6 * mm, PAGE_HEIGHT - 12 * mm, 24 * mm, fill=1, stroke=0)
    canvas.setFont("SC2Sans-Bold", 29)
    canvas.setFillColor(colors.white)
    canvas.drawString(22 * mm, PAGE_HEIGHT - 58 * mm, t["title"])
    canvas.setFont("SC2Sans", 13)
    canvas.setFillColor(colors.HexColor("#DCE6FF"))
    canvas.drawString(22 * mm, PAGE_HEIGHT - 68 * mm, t["subtitle"])
    canvas.setFont("SC2Sans", 9)
    canvas.drawString(22 * mm, PAGE_HEIGHT - 76 * mm, t["generated"])

    y = PAGE_HEIGHT - 112 * mm
    for label, key in ((t["focus_player"], "player"), (t["map"], "map"), (t["duration"], "duration"), (t["result"], "result")):
        canvas.setFillColor(colors.HexColor("#AFC4FF"))
        canvas.setFont("SC2Sans", 8)
        canvas.drawString(22 * mm, y, label.upper())
        canvas.setFillColor(colors.white)
        canvas.setFont("SC2Sans-Bold", 15)
        canvas.drawString(22 * mm, y - 7 * mm, metadata.get(key, "-")[:55])
        y -= 23 * mm
    canvas.setFillColor(colors.HexColor("#AFC4FF"))
    canvas.setFont("SC2Sans", 8)
    canvas.drawString(22 * mm, 20 * mm, "SC2 COACH AI")
    canvas.restoreState()


def key_value_table(rows: Iterable[tuple[str, str]], style: dict[str, ParagraphStyle]) -> Table:
    data = [[Paragraph(clean(label), style["small"]), Paragraph(clean(value), style["body"])] for label, value in rows]
    table = Table(data, colWidths=[48 * mm, 115 * mm], hAlign="LEFT")
    table.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("BACKGROUND", (0, 0), (0, -1), PANEL),
        ("BOX", (0, 0), (-1, -1), 0.4, GRID),
        ("INNERGRID", (0, 0), (-1, -1), 0.3, GRID),
        ("LEFTPADDING", (0, 0), (-1, -1), 3 * mm),
        ("RIGHTPADDING", (0, 0), (-1, -1), 3 * mm),
        ("TOPPADDING", (0, 0), (-1, -1), 2.3 * mm),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 2.3 * mm),
    ]))
    return table


def findings_story(findings: list[dict[str, Any]], language: str, style: dict[str, ParagraphStyle]) -> list[Flowable]:
    t = TEXT[language]
    if not findings:
        return [Paragraph(t["no_findings"], style["body"])]
    story: list[Flowable] = []
    for index, finding in enumerate(findings, 1):
        severity = str(finding.get("severity", "info"))
        title = clean(finding.get("title", f"Finding {index}"))
        explanation = clean(finding.get("explanation", finding.get("description", "")))
        recommendation = clean(finding.get("recommendation", ""))
        timing = finding.get("time_start_clock")
        if timing and finding.get("time_end_clock"):
            timing = f"{timing}-{finding.get('time_end_clock')}"
        evidence_items = []
        for item in finding.get("evidence", [])[:5]:
            evidence_items.append(f"{clean(item.get('metric'))}: <b>{metric(item.get('value'))}</b>")
        badge = f"<font color='{severity_color(severity).hexval()}'><b>{severity.upper()}</b></font>"
        heading = f"{badge}  {title}"
        if timing:
            heading += f"  <font color='{MUTED.hexval()}'>[{clean(timing)}]</font>"
        parts: list[Flowable] = [Paragraph(heading, style["card_title"])]
        if explanation:
            parts.append(Paragraph(explanation, style["card_body"]))
        if evidence_items:
            parts.append(Spacer(1, 1.5 * mm))
            parts.append(Paragraph(f"<b>{t['evidence']}:</b> " + " | ".join(evidence_items), style["card_body"]))
        if recommendation:
            parts.append(Spacer(1, 1.5 * mm))
            parts.append(Paragraph(f"<b>{t['recommendation']}:</b> {recommendation}", style["card_body"]))
        table = Table([[parts]], colWidths=[163 * mm], hAlign="LEFT")
        table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, -1), PANEL),
            ("BOX", (0, 0), (-1, -1), 0.6, severity_color(severity)),
            ("LEFTPADDING", (0, 0), (-1, -1), 4 * mm),
            ("RIGHTPADDING", (0, 0), (-1, -1), 4 * mm),
            ("TOPPADDING", (0, 0), (-1, -1), 3 * mm),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 3 * mm),
        ]))
        story.extend([KeepTogether(table), Spacer(1, 3 * mm)])
    return story


def engagement_table(battle_data: dict[str, Any], language: str, style: dict[str, ParagraphStyle]) -> Flowable:
    t = TEXT[language]
    engagements = battle_data.get("battles", [])
    if not engagements:
        return Paragraph(t["no_engagements"], style["body"])
    focus_team = str(battle_data.get("focus_team", ""))
    header = [t["id"], t["time"], t["type"], t["outcome"], t["margin"]]
    data: list[list[Any]] = [[Paragraph(f"<b>{clean(cell)}</b>", style["small"]) for cell in header]]
    for item in engagements:
        margin_value = trade_margin(item, focus_team)
        margin_text = f"-{metric(margin_value)}" if margin_value > 0 else f"+{metric(-margin_value)}"
        data.append([
            Paragraph(clean(item.get("id")), style["small"]),
            Paragraph(f"{clean(item.get('start_clock'))}-{clean(item.get('end_clock'))}", style["small"]),
            Paragraph(clean(item.get("engagement_type")), style["small"]),
            Paragraph(clean(item.get("classification")), style["small"]),
            Paragraph(margin_text, style["small"]),
        ])
    table = Table(data, colWidths=[10 * mm, 31 * mm, 39 * mm, 43 * mm, 32 * mm], repeatRows=1, hAlign="LEFT")
    table_style = [
        ("BACKGROUND", (0, 0), (-1, 0), ACCENT_DARK),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("GRID", (0, 0), (-1, -1), 0.35, GRID),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 2 * mm),
        ("RIGHTPADDING", (0, 0), (-1, -1), 2 * mm),
        ("TOPPADDING", (0, 0), (-1, -1), 1.8 * mm),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 1.8 * mm),
    ]
    for row in range(1, len(data)):
        if row % 2 == 0:
            table_style.append(("BACKGROUND", (0, row), (-1, row), PANEL))
    table.setStyle(TableStyle(table_style))
    return table


def build_pdf(out_dir: Path, replay_path: Path, language: str) -> Path:
    register_fonts()
    t = TEXT[language]
    style = styles()
    replay = load_json(replay_path)
    coaching = load_json(out_dir / "coaching_analysis.json")
    battles = load_json(out_dir / "battle_analysis.json")
    strategic = load_json(out_dir / "strategic_analysis.json")
    diagnostics = load_json(out_dir / "diagnostics.json") if (out_dir / "diagnostics.json").exists() else {}

    focus = coaching.get("focus_player") or battles.get("focus_player") or t["unknown"]
    players = coaching.get("players", [])
    focus_stats = next((item for item in players if item.get("name") == focus), {})
    metadata = replay.get("metadata", {})
    replay_players = replay.get("players", [])
    replay_focus = next((item for item in replay_players if item.get("name") == focus), {})
    game_seconds = metadata.get("game_seconds", replay.get("game_seconds", 0))
    result = replay_focus.get("result", replay_focus.get("play_result", t["unknown"]))
    map_name = metadata.get("map_name", metadata.get("map", replay.get("map_name", t["unknown"])))

    pdf_path = out_dir / "sc2_coach_report.pdf"
    frame = Frame(18 * mm, 17 * mm, PAGE_WIDTH - 36 * mm, PAGE_HEIGHT - 34 * mm, id="normal")
    first_frame = Frame(0, 0, PAGE_WIDTH, PAGE_HEIGHT, id="cover")
    doc = BaseDocTemplate(
        str(pdf_path),
        pagesize=A4,
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=18 * mm,
        bottomMargin=17 * mm,
        title=t["title"],
        author="SC2 Coach AI",
        subject="StarCraft II replay coaching report",
    )
    doc.addPageTemplates([
        PageTemplate(id="cover", frames=[first_frame], onPage=lambda c, d: cover(c, d, language, {
            "player": str(focus), "map": str(map_name), "duration": clock(game_seconds), "result": str(result)
        })),
        PageTemplate(id="body", frames=[frame], onPage=lambda c, d: page_header_footer(c, d, language)),
    ])

    story: list[Flowable] = [Spacer(1, PAGE_HEIGHT - 1 * mm), PageBreak()]
    story.append(Paragraph(t["summary"], style["h1"]))
    story.append(key_value_table([
        (t["focus_player"], str(focus)),
        (t["map"], str(map_name)),
        (t["duration"], clock(game_seconds)),
        (t["result"], str(result)),
        (t["engagements"], str(len(battles.get("battles", [])))),
        (t["findings"], str(len(strategic.get("findings", [])))),
        (t["warnings"], str(diagnostics.get("warning_count", 0))),
    ], style))
    story.extend([Spacer(1, 5 * mm), Paragraph(t["macro"], style["h2"])])
    cards = [[
        MetricCard(t["peak_army"], metric(focus_stats.get("peak_army_value", 0))),
        MetricCard(t["final_army"], metric(focus_stats.get("final_army_value", 0))),
        MetricCard(t["final_workers"], metric(focus_stats.get("final_workers", 0))),
        MetricCard(t["army_losses"], metric(focus_stats.get("final_army_losses", 0))),
    ]]
    card_table = Table(cards, colWidths=[42 * mm] * 4, hAlign="LEFT")
    card_table.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 0), ("RIGHTPADDING", (0, 0), (-1, -1), 1.5 * mm)]))
    story.extend([card_table, Spacer(1, 5 * mm), Paragraph(t["priority"], style["h1"])])
    story.extend(findings_story(strategic.get("findings", []), language, style))
    story.extend([PageBreak(), Paragraph(t["combat"], style["h1"]), engagement_table(battles, language, style)])

    chart_dir = out_dir / "charts"
    existing_charts = [(chart_dir / filename, caption_key) for filename, caption_key in CHARTS if (chart_dir / filename).exists()]
    if existing_charts:
        story.extend([PageBreak(), Paragraph(t["charts"], style["h1"])])
        for index, (path, caption_key) in enumerate(existing_charts):
            image = Image(str(path))
            max_w, max_h = 163 * mm, 104 * mm
            scale = min(max_w / image.imageWidth, max_h / image.imageHeight)
            image.drawWidth = image.imageWidth * scale
            image.drawHeight = image.imageHeight * scale
            story.extend([Paragraph(t[caption_key], style["caption"]), image, Spacer(1, 6 * mm)])
            if index != len(existing_charts) - 1 and index % 2 == 1:
                story.append(PageBreak())

    doc.build(story)
    return pdf_path


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate a localized SC2 Coach PDF report")
    parser.add_argument("replay_json", type=Path)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--lang", choices=("en", "ru"), default="en")
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    path = build_pdf(args.out, args.replay_json, args.lang)
    print(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
