#!/usr/bin/env python3
"""Validate SC2 Coach release versions and synchronized version references."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

SEMVER = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")


def parse(value: str) -> tuple[int, int, int]:
    value = value.strip()
    match = SEMVER.fullmatch(value)
    if not match:
        raise ValueError(f"Invalid release version {value!r}; expected MAJOR.MINOR.PATCH")
    return tuple(map(int, match.groups()))


def read_version(path: Path) -> str:
    value = path.read_text(encoding="utf-8").strip()
    parse(value)
    return value


def check_increase(previous: str, candidate: str) -> None:
    if parse(candidate) <= parse(previous):
        raise ValueError(
            f"Release version must increase: previous={previous}, candidate={candidate}"
        )


def check_sync(root: Path, version: str) -> None:
    checks = {
        root / "java/pom.xml": f"<version>{version}-SNAPSHOT</version>",
        root / "java/coach-domain/pom.xml": f"<version>{version}-SNAPSHOT</version>",
        root / "java/portal/pom.xml": f"<version>{version}-SNAPSHOT</version>",
        root / "frontend/package.json": f'"version": "{version}"',
        root / "Dockerfile": f"ARG APP_VERSION={version}-SNAPSHOT",
        root / "compose.yaml": f"${{APP_VERSION:-{version}-SNAPSHOT}}",
    }
    errors: list[str] = []
    for path, expected in checks.items():
        if not path.exists():
            errors.append(f"missing file: {path.relative_to(root)}")
            continue
        if expected not in path.read_text(encoding="utf-8"):
            errors.append(f"{path.relative_to(root)} does not contain {expected!r}")
    if errors:
        raise ValueError("Version references are not synchronized:\n- " + "\n- ".join(errors))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--previous")
    parser.add_argument("--check-sync", action="store_true")
    args = parser.parse_args()

    try:
        version = read_version(args.root / "VERSION")
        if args.previous is not None:
            check_increase(args.previous, version)
        if args.check_sync:
            check_sync(args.root, version)
    except (OSError, ValueError) as exc:
        print(f"release-version error: {exc}", file=sys.stderr)
        return 1

    print(version)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
