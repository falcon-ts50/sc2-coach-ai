# SC2 Coach AI

Open-source tooling for decoding StarCraft II replay files and turning deterministic replay data into actionable coaching feedback.

## Status

The project is currently at `v0.3`:

- reads `.SC2Replay` files locally through `sc2reader` and Blizzard `s2protocol`;
- exports replay metadata, players, teams, MMR (when present), units, structures, upgrades, commands and periodic statistics;
- produces machine-readable JSON and Markdown;
- generates coaching metrics for army value, losses, workers, resource float, supply blocks and turning points;
- generates PNG charts for army value, workers, bank, collection rate and cumulative army losses;
- classifies engagements as battles, skirmishes, worker harassment, base assaults or minor contacts;
- produces diagnostics and a single review ZIP containing all reports and charts.

The replay is processed locally and is not uploaded anywhere.

## Quick start

Requirements:

- Python 3.11+
- Internet access for the initial dependency installation

Make the unified command executable once:

```bash
chmod +x sc2-coach
```

Run the complete pipeline with one command:

```bash
./sc2-coach match.SC2Replay \
  --player dragonDriver \
  --out ./results/match
```

This performs:

```text
decode
  -> coaching analysis
  -> engagement analysis
  -> charts
  -> diagnostics
  -> review bundle
```

The main file to share for review is:

```text
results/match/sc2_coach_review_bundle.zip
```

When `--out` is omitted, the command creates a directory under `results/` using the replay filename:

```bash
./sc2-coach match.SC2Replay --player dragonDriver
```

### Low-level commands

The two-stage interface remains available for debugging and development:

```bash
sh run.sh match.SC2Replay \
  --player dragonDriver \
  --out ./results/match

sh run_coach.sh ./results/match/replay_analysis.json \
  --player dragonDriver \
  --out ./results/match
```

## Outputs

```text
results/match/replay_analysis.json
results/match/replay_analysis.md
results/match/coaching_analysis.json
results/match/coaching_report.md
results/match/battle_analysis.json
results/match/battle_report.md
results/match/review_summary.md
results/match/diagnostics.json
results/match/manifest.json
results/match/sc2_coach_review_bundle.zip
results/match/charts/army_value.png
results/match/charts/workers.png
results/match/charts/bank.png
results/match/charts/income_rate.png
results/match/charts/army_losses.png
```

Engagement windows are inferred from death-event clusters. Resource trade values are estimates derived from cumulative army-loss deltas in the nearest `PlayerStatsEvent` snapshots.

## Time model

StarCraft II replay frames are game loops. The decoder uses 16 loops per game-time second:

```text
game_seconds = replay.frames / 16
```

On `Faster`, real elapsed time is shorter. Both values are exported separately as `game_seconds` and `real_seconds`.

## Tests

```bash
python -m pip install -r requirements-dev.txt
python -m pytest -q
```

## Roadmap

- `v0.1`: stable decoder and versioned replay JSON
- `v0.2`: economy, army and worker charts
- `v0.3`: automatic engagement and turning-point detection
- `v0.4`: coaching rules for macro, scouting, composition and teamwork
- `v0.5`: build-order comparison against reference replays
- `v0.6`: polished HTML/PDF reports
- `v1.0`: web application with replay upload and interactive analysis

## Architecture direction

```text
SC2Replay
   ↓
Python decoder adapter
   ↓
Versioned normalized replay JSON
   ↓
Timeline / economy / combat engines
   ↓
Coaching rules and chart renderer
   ↓
Markdown / JSON / HTML / PDF reports
```

A future Java 25 core is planned for the typed domain model, analysis engine, API and report generation. Python remains a replaceable replay-decoder adapter.

## License and trademarks

The repository uses the Apache License 2.0.

This project is not affiliated with or endorsed by Blizzard Entertainment. StarCraft and StarCraft II are trademarks of Blizzard Entertainment.
