from __future__ import annotations

import json
from pathlib import Path

import pytest

from pdf_report import build_pdf, register_fonts


def write_json(path: Path, value: dict) -> None:
    path.write_text(json.dumps(value, ensure_ascii=False), encoding="utf-8")


def make_fixture(tmp_path: Path) -> tuple[Path, Path]:
    out = tmp_path / "out"
    out.mkdir()
    replay = tmp_path / "replay_analysis.json"
    write_json(replay, {
        "metadata": {"map_name": "Тестовая карта", "game_seconds": 721},
        "players": [{"name": "dragonDriver", "result": "Loss"}],
    })
    write_json(out / "coaching_analysis.json", {
        "focus_player": "dragonDriver",
        "players": [{
            "name": "dragonDriver",
            "peak_army_value": 5250,
            "final_army_value": 1100,
            "final_workers": 31,
            "final_army_losses": 8900,
        }],
    })
    write_json(out / "battle_analysis.json", {
        "focus_player": "dragonDriver",
        "focus_team": "1",
        "battles": [{
            "id": 1,
            "start_clock": "15:00",
            "end_clock": "16:10",
            "engagement_type": "battle",
            "classification": "lost",
            "team_loss_delta": {"1": 2400, "2": 900},
        }],
    })
    write_json(out / "strategic_analysis.json", {
        "findings": [{
            "severity": "critical",
            "title": "Позднее восстановление армии",
            "explanation": "После крупного размена стоимость армии долго не восстанавливалась.",
            "recommendation": "Сразу ставить производство и сохранять банк на восстановление.",
            "time_start_clock": "16:10",
            "time_end_clock": "18:30",
            "evidence": [{"metric": "army_value_drop", "value": 3100}],
        }],
    })
    write_json(out / "diagnostics.json", {"warning_count": 0})
    return replay, out


def test_fonts_are_available_on_supported_linux_runner() -> None:
    try:
        regular, bold = register_fonts()
    except FileNotFoundError:
        pytest.skip("DejaVu Sans is not installed in this environment")
    assert regular == "SC2Sans"
    assert bold == "SC2Sans-Bold"


@pytest.mark.parametrize("language", ["en", "ru"])
def test_build_pdf_report(tmp_path: Path, language: str) -> None:
    replay, out = make_fixture(tmp_path)
    try:
        pdf_path = build_pdf(out, replay, language)
    except FileNotFoundError:
        pytest.skip("DejaVu Sans is not installed in this environment")
    payload = pdf_path.read_bytes()
    assert payload.startswith(b"%PDF-")
    assert len(payload) > 10_000
