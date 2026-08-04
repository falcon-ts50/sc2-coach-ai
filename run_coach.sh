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
PREV=""
for ARG in "$@"; do
  if [ "$PREV" = "--out" ]; then OUT_DIR=$ARG; fi
  if [ "$PREV" = "--player" ]; then PLAYER=$ARG; fi
  PREV=$ARG
done

if [ -z "$PLAYER" ]; then
  echo "--player is required" >&2
  exit 2
fi

"$VENV/bin/python" "$ROOT_DIR/coach.py" "$@"
"$VENV/bin/python" "$ROOT_DIR/battles_v033.py" "$INPUT_JSON" --player "$PLAYER" --out "$OUT_DIR"
"$VENV/bin/python" "$ROOT_DIR/charts.py" "$INPUT_JSON" --battles "$OUT_DIR/battle_analysis.json" --out "$OUT_DIR"

cat >> "$OUT_DIR/coaching_report.md" <<'EOF'

## Engagement analysis

See [battle_report.md](battle_report.md) for battles, skirmishes, worker harassment and base assaults.

## Charts

### Army value
![Army value](charts/army_value.png)

### Active workers
![Workers](charts/workers.png)

### Unspent resources
![Bank](charts/bank.png)

### Collection rate
![Income rate](charts/income_rate.png)

### Cumulative army losses
![Army losses](charts/army_losses.png)
EOF

"$VENV/bin/python" "$ROOT_DIR/review_bundle.py" "$INPUT_JSON" --out "$OUT_DIR"

echo "Review bundle: $OUT_DIR/sc2_coach_review_bundle.zip"
