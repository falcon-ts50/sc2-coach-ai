from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Mapping, Sequence


@dataclass(frozen=True)
class ReportFinding:
    finding_id: str
    severity: str
    title: str
    explanation: str
    recommendation: str
    time_start_seconds: float | None = None
    time_end_seconds: float | None = None
    evidence: tuple[Mapping[str, Any], ...] = ()


@dataclass(frozen=True)
class ReportTable:
    columns: tuple[str, ...]
    rows: tuple[tuple[str, ...], ...]
    caption: str | None = None

    def __post_init__(self) -> None:
        width = len(self.columns)
        if width == 0:
            raise ValueError("ReportTable requires at least one column")
        if any(len(row) != width for row in self.rows):
            raise ValueError("Every table row must match the column count")


@dataclass(frozen=True)
class ReportSection:
    section_id: str
    title: str
    paragraphs: tuple[str, ...] = ()
    findings: tuple[ReportFinding, ...] = ()
    tables: tuple[ReportTable, ...] = ()
    chart_paths: tuple[Path, ...] = ()
    metadata: Mapping[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class ReportDocument:
    schema_version: str
    language: str
    title: str
    subtitle: str | None
    player_name: str
    match_metadata: Mapping[str, Any]
    sections: tuple[ReportSection, ...]
    source_files: tuple[Path, ...] = ()

    def __post_init__(self) -> None:
        if self.language not in {"en", "ru"}:
            raise ValueError("language must be 'en' or 'ru'")
        section_ids = [section.section_id for section in self.sections]
        if len(section_ids) != len(set(section_ids)):
            raise ValueError("Report section IDs must be unique")

    def section(self, section_id: str) -> ReportSection:
        for section in self.sections:
            if section.section_id == section_id:
                return section
        raise KeyError(section_id)
