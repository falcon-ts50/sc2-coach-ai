#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VENV="$ROOT_DIR/.venv"

if [ ! -x "$VENV/bin/python" ]; then
  echo "Virtual environment not found. Run ./run.sh once first." >&2
  exit 1
fi

INPUT_JSON=$1
OUT_DIR=out
PLAYER=""
LANGUAGE=en
PREV=""
for ARG in "$@"; do
  if [ "$PREV" = "--out" ]; then OUT_DIR=$ARG; fi
  if [ "$PREV" = "--player" ]; then PLAYER=$ARG; fi
  if [ "$PREV" = "--lang" ]; then LANGUAGE=$ARG; fi
  PREV=$ARG
done

if [ -z "$PLAYER" ]; then
  echo "--player is required" >&2
  exit 2
fi
case "$LANGUAGE" in en|ru) ;; *) echo "--lang must be en or ru" >&2; exit 2 ;; esac

"$VENV/bin/python" "$ROOT_DIR/coach.py" "$INPUT_JSON" --player "$PLAYER" --out "$OUT_DIR"
"$VENV/bin/python" "$ROOT_DIR/battles_v033.py" "$INPUT_JSON" --player "$PLAYER" --out "$OUT_DIR"
"$VENV/bin/python" "$ROOT_DIR/coach_rules_i18n.py" \
  --coaching "$OUT_DIR/coaching_analysis.json" \
  --battles "$OUT_DIR/battle_analysis.json" \
  --out "$OUT_DIR" \
  --lang "$LANGUAGE"
"$VENV/bin/python" "$ROOT_DIR/charts.py" "$INPUT_JSON" --battles "$OUT_DIR/battle_analysis.json" --out "$OUT_DIR"

# First pass creates diagnostics consumed by the localized summary.
"$VENV/bin/python" "$ROOT_DIR/review_bundle.py" "$INPUT_JSON" --out "$OUT_DIR" >/dev/null
"$VENV/bin/python" "$ROOT_DIR/report_i18n.py" --replay "$INPUT_JSON" --out "$OUT_DIR" --lang "$LANGUAGE"
# Rebuild the ZIP with localized Markdown files.
"$VENV/bin/python" "$ROOT_DIR/review_bundle.py" "$INPUT_JSON" --out "$OUT_DIR" >/dev/null

if [ "$LANGUAGE" = "ru" ]; then
  echo "Бандл для проверки: $OUT_DIR/sc2_coach_review_bundle.zip"
else
  echo "Review bundle: $OUT_DIR/sc2_coach_review_bundle.zip"
fi
