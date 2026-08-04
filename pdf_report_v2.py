#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path
from typing import Any

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    Image,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)

import pdf_report as legacy


def build_pdf(out_dir: Path, replay_path: Path, language: str) -> Path:
    legacy.register_fonts()
    t = legacy.TEXT[language]
    style = legacy.styles()
    replay = legacy.load_json(replay_path)
    coaching = legacy.load_json(out_dir / "coaching_analysis.json")
    battles = legacy.load_json(out_dir / "battle_analysis.json")
    strategic = legacy.load_json(out_dir / "strategic_analysis.json")
    diagnostics = legacy.load_json(out_dir / "diagnostics.json") if (out_dir / "diagnostics.json").exists() else {}

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
    frame = Frame(18 * mm, 17 * mm, legacy.PAGE_WIDTH - 36 * mm, legacy.PAGE_HEIGHT - 34 * mm, id="normal")
    cover_frame = Frame(0, 0, legacy.PAGE_WIDTH, legacy.PAGE_HEIGHT, leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0, id="cover")
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
        PageTemplate(id="cover", frames=[cover_frame], onPage=lambda c, d: legacy.cover(c, d, language, {
            "player": str(focus), "map": str(map_name), "duration": legacy.clock(game_seconds), "result": str(result)
        })),
        PageTemplate(id="body", frames=[frame], onPage=lambda c, d: legacy.page_header_footer(c, d, language)),
    ])

    # The cover is drawn entirely by the page callback. Use ReportLab's native
    # page-template transition instead of a page-sized Spacer, which is brittle
    # because Frame reserves internal padding.
    story: list[Flowable] = [NextPageTemplate("body"), PageBreak()]
    story.append(Paragraph(t["summary"], style["h1"]))
    story.append(legacy.key_value_table([
        (t["focus_player"], str(focus)),
        (t["map"], str(map_name)),
        (t["duration"], legacy.clock(game_seconds)),
        (t["result"], str(result)),
        (t["engagements"], str(len(battles.get("battles", [])))),
        (t["findings"], str(len(strategic.get("findings", [])))),
        (t["warnings"], str(diagnostics.get("warning_count", 0))),
    ], style))
    story.extend([Spacer(1, 5 * mm), Paragraph(t["macro"], style["h2"])])
    cards = [[
        legacy.MetricCard(t["peak_army"], legacy.metric(focus_stats.get("peak_army_value", 0))),
        legacy.MetricCard(t["final_army"], legacy.metric(focus_stats.get("final_army_value", 0))),
        legacy.MetricCard(t["final_workers"], legacy.metric(focus_stats.get("final_workers", 0))),
        legacy.MetricCard(t["army_losses"], legacy.metric(focus_stats.get("final_army_losses", 0))),
    ]]
    card_table = Table(cards, colWidths=[42 * mm] * 4, hAlign="LEFT")
    card_table.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 0),
        ("RIGHTPADDING", (0, 0), (-1, -1), 1.5 * mm),
    ]))
    story.extend([card_table, Spacer(1, 5 * mm), Paragraph(t["priority"], style["h1"])])
    story.extend(legacy.findings_story(strategic.get("findings", []), language, style))
    story.extend([PageBreak(), Paragraph(t["combat"], style["h1"]), legacy.engagement_table(battles, language, style)])

    chart_dir = out_dir / "charts"
    existing_charts = [(chart_dir / filename, caption_key) for filename, caption_key in legacy.CHARTS if (chart_dir / filename).exists()]
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
