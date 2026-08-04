from __future__ import annotations

import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CLI = ROOT / "sc2-coach"


def run_cli(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["sh", str(CLI), *args],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=False,
    )


def test_help_describes_unified_pipeline() -> None:
    result = run_cli("--help")
    assert result.returncode == 0
    assert "decode -> coaching -> engagements -> strategic coaching" in result.stdout
    assert "charts -> diagnostics -> review bundle" in result.stdout
    assert "--lang en|ru" in result.stdout


def test_missing_replay_fails_cleanly() -> None:
    result = run_cli("missing.SC2Replay", "--player", "dragonDriver")
    assert result.returncode == 2
    assert "Replay file not found" in result.stderr


def test_player_is_required(tmp_path: Path) -> None:
    replay = tmp_path / "match.SC2Replay"
    replay.write_bytes(b"not-a-real-replay")
    result = run_cli(str(replay))
    assert result.returncode == 2
    assert "--player is required" in result.stderr
