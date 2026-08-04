# SC2 Coach AI

Open-source tooling for decoding StarCraft II replay files and turning deterministic replay data into actionable coaching feedback.

## Status

The project is currently at `v0.5`:

- reads `.SC2Replay` files locally through `sc2reader` and Blizzard `s2protocol`;
- exports replay metadata, players, teams, MMR, units, structures, upgrades, commands and periodic statistics;
- generates coaching metrics, engagement analysis, charts and diagnostics;
- produces explainable strategic recommendations with numeric evidence;
- creates a polished A4 PDF report with embedded charts;
- creates one ZIP containing all reports, charts and the PDF;
- supports English by default and Russian through `--lang ru`.

The replay is processed only after an explicit CLI invocation. There is no background watch mode.

## Quick start

Requirements:

- Python 3.11+
- Internet access for the initial dependency installation
- DejaVu Sans fonts (`dejavu-fonts` on Arch/CachyOS, `fonts-dejavu-core` on Debian/Ubuntu)

```bash
chmod +x sc2-coach
```

English reports are the default:

```bash
./sc2-coach match.SC2Replay \
  --player dragonDriver \
  --out ./results/match
```

Russian reports:

```bash
./sc2-coach match.SC2Replay \
  --player dragonDriver \
  --lang ru \
  --out ./results/match
```

The complete pipeline is:

```text
decode
  -> coaching analysis
  -> engagement analysis
  -> strategic coaching
  -> charts
  -> diagnostics
  -> PDF report
  -> review bundle
```

The main human-readable output is:

```text
results/match/sc2_coach_report.pdf
```

The complete shareable package is:

```text
results/match/sc2_coach_review_bundle.zip
```

When `--out` is omitted, the command creates a directory under `results/` using the replay filename.

## PDF report

The PDF is generated programmatically with ReportLab and includes:

- a localized title page;
- match metadata and focus-player macro cards;
- prioritized strategic findings with severity, evidence and concrete actions;
- a compact engagement table;
- full-width army, worker, resource-bank, income-rate and army-loss charts;
- page headers, footers and page numbers.

The PDF does not invent new analysis. It presents the same deterministic facts and explainable rule results already exported in JSON and Markdown.

## Localization contract

`--lang` accepts:

- `en` — default;
- `ru` — Russian Markdown reports, PDF report and CLI messages.

JSON field names, categories, evidence metric names and `rule_id` values remain stable English identifiers for API compatibility. Human-readable strategic titles, explanations and recommendations follow the selected language.

## Low-level commands

The two-stage interface remains available for debugging:

```bash
sh run.sh match.SC2Replay --player dragonDriver --out ./results/match
sh run_coach.sh ./results/match/replay_analysis.json --player dragonDriver --out ./results/match --lang ru
```

## Outputs

```text
results/match/replay_analysis.json
results/match/replay_analysis.md
results/match/coaching_analysis.json
results/match/coaching_report.md
results/match/battle_analysis.json
results/match/battle_report.md
results/match/strategic_analysis.json
results/match/strategic_report.md
results/match/sc2_coach_report.pdf
results/match/review_summary.md
results/match/diagnostics.json
results/match/sc2_coach_review_bundle.zip
results/match/charts/*.png
```

Engagement windows are inferred from death-event clusters. Resource trade values are estimates derived from cumulative army-loss deltas in nearby `PlayerStatsEvent` snapshots.

## Tests

```bash
python -m pip install -r requirements-dev.txt
python -m pytest -q
```

## Roadmap

- `v0.1`: stable decoder and versioned replay JSON
- `v0.2`: economy, army and worker charts
- `v0.3`: automatic engagement and turning-point detection
- `v0.4`: explainable coaching rules and localization
- `v0.5`: polished localized PDF report
- `v0.6`: build-order comparison against reference replays
- `v1.0`: web application with explicit replay upload and interactive analysis

## License and trademarks

The repository uses the Apache License 2.0.

This project is not affiliated with or endorsed by Blizzard Entertainment. StarCraft and StarCraft II are trademarks of Blizzard Entertainment.
