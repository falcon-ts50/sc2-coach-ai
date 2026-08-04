#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

import battles_v032 as base


def engagement_type(loss_categories: dict[str, dict[str, int]]) -> str:
    totals: dict[str, int] = {}
    for categories in loss_categories.values():
        for key, value in categories.items():
            totals[key] = totals.get(key, 0) + int(value)
    army = totals.get("army", 0)
    workers = totals.get("worker", 0)
    static = totals.get("static_defense", 0)
    economy = totals.get("economy_structure", 0)
    if army >= 8:
        return "battle"
    if army >= 2:
        return "skirmish"
    if workers >= 3 and army < 2:
        return "worker_harass"
    if static + economy >= 2 and army < 2:
        return "base_assault"
    return "minor_contact"


def economic_damage(loss_categories: dict[str, dict[str, int]]) -> dict[str, int]:
    return {
        name: int(categories.get("worker", 0)) + int(categories.get("economy_structure", 0))
        for name, categories in loss_categories.items()
    }


def build(data: dict[str, Any], focus_player: str) -> dict[str, Any]:
    report = base.build(data, focus_player)
    report["schema_version"] = "0.3.3"
    report["detector"]["engagement_types"] = [
        "battle", "skirmish", "worker_harass", "base_assault", "minor_contact"
    ]
    for engagement in report["battles"]:
        categories = engagement.get("loss_categories", {})
        engagement["engagement_type"] = engagement_type(categories)
        engagement["economic_damage"] = economic_damage(categories)
        if engagement["engagement_type"] in {"worker_harass", "base_assault"}:
            engagement["classification"] = "economic_damage"
    return report


def write_markdown(report: dict[str, Any], path: Path) -> None:
    lines = ["# SC2 Coach — engagement report", ""]
    for item in report["battles"]:
        lines += [
            f"## Engagement #{item['id']} — {item['start_clock']}–{item['end_clock']}", "",
            f"- Type: **{item['engagement_type']}**",
            f"- Classification: **{item['classification']}**",
            f"- Meaningful death events: **{item['death_count']}**",
            f"- Duration: **{int(item['duration'])} s**",
            "- Estimated army-loss delta by player:",
        ]
        for name, value in sorted(item["player_loss_delta"].items(), key=lambda x: x[1], reverse=True):
            lines.append(f"  - {name}: {int(value)} resources")
        lines.append("- Loss profile:")
        for name, values in item["loss_categories"].items():
            lines.append(f"  - {name}: " + ", ".join(f"{k}={v}" for k, v in sorted(values.items())))
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("json", type=Path)
    parser.add_argument("--player", required=True)
    parser.add_argument("--out", type=Path, default=Path("out"))
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    report = build(json.loads(args.json.read_text(encoding="utf-8")), args.player)
    (args.out / "battle_analysis.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    write_markdown(report, args.out / "battle_report.md")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
