from __future__ import annotations

from pathlib import Path
from typing import Protocol

from report_model import ReportDocument


class ReportRenderer(Protocol):
    """Output adapter for a renderer-independent report document."""

    format_name: str

    def render(self, document: ReportDocument, output_path: Path) -> Path:
        """Render document to output_path and return the created file path."""
        ...
