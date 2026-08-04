#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VENV="$ROOT_DIR/.venv"

if [ ! -x "$VENV/bin/python" ]; then
  echo "Virtual environment not found. Run ./run.sh once first." >&2
  exit 1
fi

exec "$VENV/bin/python" "$ROOT_DIR/coach.py" "$@"
