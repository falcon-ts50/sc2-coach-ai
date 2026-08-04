# Review bundle

`run_coach.sh` creates a single uploadable archive after all analysis steps finish:

```text
<out>/sc2_coach_review_bundle.zip
```

The archive contains:

- `replay_analysis.json`
- `coaching_analysis.json`
- `coaching_report.md`
- `battle_analysis.json`
- `battle_report.md`
- `review_summary.md`
- `diagnostics.json`
- all generated PNG charts
- `manifest.json` with file sizes and SHA-256 hashes

## Diagnostics

The generated `diagnostics.json` checks structural invariants before the results are reviewed:

- engagement windows must not overlap;
- end time must be after start time;
- window duration must remain within the configured limit;
- engagement types and outcomes must be known;
- army engagements with zero estimated army-loss delta are flagged.

Warnings do not stop report generation. They are included in `review_summary.md` and the ZIP bundle so detector problems can be reviewed without exchanging several files individually.

## Workflow

```bash
./run_coach.sh replay_analysis.json \
  --player dragonDriver \
  --out ./results/match
```

For external review, upload only:

```text
results/match/sc2_coach_review_bundle.zip
```
