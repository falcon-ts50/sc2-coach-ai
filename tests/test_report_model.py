from pathlib import Path

import pytest

from report_model import ReportDocument, ReportSection, ReportTable


def test_report_document_supports_language_and_section_lookup() -> None:
    summary = ReportSection(section_id="summary", title="Executive Summary")
    document = ReportDocument(
        schema_version="0.5.0",
        language="en",
        title="SC2 Coach Report",
        subtitle=None,
        player_name="dragonDriver",
        match_metadata={"map": "Example Map"},
        sections=(summary,),
        source_files=(Path("replay_analysis.json"),),
    )

    assert document.section("summary") is summary


def test_report_document_rejects_duplicate_section_ids() -> None:
    section = ReportSection(section_id="summary", title="Summary")
    with pytest.raises(ValueError, match="unique"):
        ReportDocument(
            schema_version="0.5.0",
            language="ru",
            title="Отчёт SC2 Coach",
            subtitle=None,
            player_name="dragonDriver",
            match_metadata={},
            sections=(section, section),
        )


def test_report_table_requires_consistent_width() -> None:
    with pytest.raises(ValueError, match="column count"):
        ReportTable(columns=("Battle", "Result"), rows=(("B1",),))
