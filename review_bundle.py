#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import zipfile
from collections import Counter
from pathlib import Path
from typing import Any

KNOWN_TYPES = {"battle", "skirmish", "worker_harass", "base_assault", "minor_contact"}
KNOWN_OUTCOMES = {
    "catastrophic_loss", "lost", "even", "won", "decisive_win",
    "economic_damage", "minor_contact",
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def margin(engagement: dict[str, Any], focus_team: str) -> float:
    losses = engagement.get("team_loss_delta", {})
    focus = float(losses.get(focus_team, 0))
    enemy = sum(float(value) for team, value in losses.items() if str(team) != focus_team)
    return focus - enemy


def diagnostics(battle_data: dict[str, Any]) -> dict[str, Any]:
    engagements = battle_data.get("battles", [])
    warnings: list[dict[str, Any]] = []
    previous_end = -1.0
    for engagement in engagements:
        engagement_id = engagement.get("id")
        start = float(engagement.get("start", 0))
        end = float(engagement.get("end", 0))
        duration = float(engagement.get("duration", end - start))
        engagement_type = engagement.get("engagement_type")
        outcome = engagement.get("classification")
        if start < previous_end - 0.01:
            warnings.append({"code": "overlap", "engagement_id": engagement_id, "message": "Engagement overlaps the previous window."})
        if end <= start:
            warnings.append({"code": "invalid_window", "engagement_id": engagement_id, "message": "End time is not after start time."})
        if duration > 95.1:
            warnings.append({"code": "duration_limit", "engagement_id": engagement_id, "message": f"Window duration is {duration:.1f}s."})
        if engagement_type not in KNOWN_TYPES:
            warnings.append({"code": "unknown_type", "engagement_id": engagement_id, "message": f"Unknown engagement type: {engagement_type!r}."})
        if outcome not in KNOWN_OUTCOMES:
            warnings.append({"code": "unknown_outcome", "engagement_id": engagement_id, "message": f"Unknown outcome: {outcome!r}."})
        total_delta = sum(float(value) for value in engagement.get("team_loss_delta", {}).values())
        if engagement_type in {"battle", "skirmish"} and total_delta == 0:
            warnings.append({"code": "zero_army_delta", "engagement_id": engagement_id, "message": "Army engagement has zero estimated army-loss delta."})
        previous_end = max(previous_end, end)
    return {
        "schema_version": "0.5.0",
        "status": "ok" if not warnings else "warnings",
        "engagement_count": len(engagements),
        "warning_count": len(warnings),
        "warnings": warnings,
    }


def summary_markdown(
    battle_data: dict[str, Any],
    coaching_data: dict[str, Any] | None,
    strategic_data: dict[str, Any] | None,
    diagnostics_data: dict[str, Any],
) -> str:
    engagements = battle_data.get("battles", [])
    focus_team = str(battle_data.get("focus_team"))
    type_counts = Counter(str(item.get("engagement_type", "unknown")) for item in engagements)
    outcome_counts = Counter(str(item.get("classification", "unknown")) for item in engagements)
    ranked = sorted(engagements, key=lambda item: margin(item, focus_team), reverse=True)
    negative = [item for item in ranked if margin(item, focus_team) > 0][:5]
    positive = sorted((item for item in engagements if margin(item, focus_team) < 0), key=lambda item: margin(item, focus_team))[:3]

    lines = [
        "# SC2 Coach — review summary",
        "",
        f"- Focus player: **{battle_data.get('focus_player', 'unknown')}**",
        f"- Engagement schema: **{battle_data.get('schema_version', 'unknown')}**",
        f"- Diagnostics: **{diagnostics_data['status']}** ({diagnostics_data['warning_count']} warnings)",
        "",
        "## Engagement counts",
        "",
    ]
    for name, count in sorted(type_counts.items()):
        lines.append(f"- {name}: **{count}**")
    lines += ["", "## Outcome counts", ""]
    for name, count in sorted(outcome_counts.items()):
        lines.append(f"- {name}: **{count}**")

    lines += ["", "## Largest negative engagements", ""]
    if negative:
        for item in negative:
            lines.append(
                f"- #{item['id']} `{item['start_clock']}–{item['end_clock']}` "
                f"{item.get('engagement_type', 'unknown')} / {item.get('classification', 'unknown')}: "
                f"**{int(margin(item, focus_team))}** resources worse for the focus team"
            )
    else:
        lines.append("- None detected.")

    lines += ["", "## Largest positive engagements", ""]
    if positive:
        for item in positive:
            lines.append(
                f"- #{item['id']} `{item['start_clock']}–{item['end_clock']}` "
                f"{item.get('engagement_type', 'unknown')} / {item.get('classification', 'unknown')}: "
                f"**{int(-margin(item, focus_team))}** resources better for the focus team"
            )
    else:
        lines.append("- None detected.")

    if strategic_data:
        lines += ["", "## Strategic findings", ""]
        findings = strategic_data.get("findings", [])
        if findings:
            for item in findings[:5]:
                timing = f" `{item.get('time_start_clock')}`" if item.get("time_start_clock") else ""
                lines.append(f"- **{item.get('severity')}**{timing} — {item.get('title')}")
        else:
            lines.append("- No coaching rule crossed its threshold.")

    if coaching_data:
        focus = coaching_data.get("focus_player")
        player = next((p for p in coaching_data.get("players", []) if p.get("name") == focus), None)
        if player:
            lines += [
                "", "## Focus-player macro snapshot", "",
                f"- Peak army: **{int(player.get('peak_army_value', 0))}**",
                f"- Final army: **{int(player.get('final_army_value', 0))}**",
                f"- Final workers: **{int(player.get('final_workers', 0))}**",
                f"- Recorded army losses: **{int(player.get('final_army_losses', 0))}**",
            ]

    if diagnostics_data["warnings"]:
        lines += ["", "## Diagnostics warnings", ""]
        for warning in diagnostics_data["warnings"]:
            lines.append(f"- `{warning['code']}` engagement #{warning.get('engagement_id')}: {warning['message']}")
    return "\n".join(lines) + "\n"


def bundle(out_dir: Path, replay_json: Path) -> Path:
    zip_path = out_dir / "sc2_coach_review_bundle.zip"
    candidates = [
        replay_json,
        out_dir / "coaching_analysis.json",
        out_dir / "coaching_report.md",
        out_dir / "battle_analysis.json",
        out_dir / "battle_report.md",
        out_dir / "strategic_analysis.json",
        out_dir / "strategic_report.md",
        out_dir / "sc2_coach_report.pdf",
        out_dir / "review_summary.md",
        out_dir / "diagnostics.json",
    ]
    candidates.extend(sorted((out_dir / "charts").glob("*.png")) if (out_dir / "charts").exists() else [])
    files = [path for path in candidates if path.exists()]
    manifest = {
        "schema_version": "0.5.0",
        "files": [
            {"name": ("replay_analysis.json" if path == replay_json else str(path.relative_to(out_dir))), "size": path.stat().st_size, "sha256": sha256(path)}
            for path in files
        ],
    }
    manifest_bytes = json.dumps(manifest, ensure_ascii=False, indent=2).encode("utf-8")
    with zipfile.ZipFile(zip_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for path in files:
            arcname = "replay_analysis.json" if path == replay_json else str(path.relative_to(out_dir))
            archive.write(path, arcname)
        archive.writestr("manifest.json", manifest_bytes)
    return zip_path


def main() -> int:
    parser = argparse.ArgumentParser(description="Create a one-file SC2 Coach review bundle")
    parser.add_argument("replay_json", type=Path)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    battle_path = args.out / "battle_analysis.json"
    coaching_path = args.out / "coaching_analysis.json"
    strategic_path = args.out / "strategic_analysis.json"
    battle_data = load_json(battle_path)
    coaching_data = load_json(coaching_path) if coaching_path.exists() else None
    strategic_data = load_json(strategic_path) if strategic_path.exists() else None
    diagnostic_data = diagnostics(battle_data)
    (args.out / "diagnostics.json").write_text(json.dumps(diagnostic_data, ensure_ascii=False, indent=2), encoding="utf-8")
    (args.out / "review_summary.md").write_text(
        summary_markdown(battle_data, coaching_data, strategic_data, diagnostic_data),
        encoding="utf-8",
    )
    zip_path = bundle(args.out, args.replay_json)
    print(zip_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
