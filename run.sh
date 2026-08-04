#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VENV="$ROOT_DIR/.venv"

if [ ! -x "$VENV/bin/python" ]; then
  python -m venv "$VENV"
fi

"$VENV/bin/python" -m pip install --upgrade pip
"$VENV/bin/python" -m pip install -r "$ROOT_DIR/requirements.txt"
exec "$VENV/bin/python" "$ROOT_DIR/analyze.py" "$@"
