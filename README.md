# SC2 Coach AI

Open-source tooling for decoding StarCraft II replay files and turning deterministic replay data into actionable coaching feedback.

## Status

The project is currently stabilizing `v0.1`:

- reads `.SC2Replay` files locally through `sc2reader` and Blizzard `s2protocol`;
- exports replay metadata, players, teams, MMR (when present), units, structures, upgrades, commands and periodic statistics;
- produces machine-readable JSON and Markdown;
- generates initial coaching metrics for army value, losses, workers, resource float, supply blocks and turning points.

The replay is processed locally and is not uploaded anywhere.

## Quick start

Requirements:

- Python 3.11+
- Internet access for the initial dependency installation

```bash
chmod +x run.sh run_coach.sh

./run.sh match.SC2Replay \
  --player dragonDriver \
  --out ./results/match
```

Generate deterministic coaching metrics:

```bash
./run_coach.sh ./results/match/replay_analysis.json \
  --player dragonDriver \
  --out ./results/match
```

Outputs:

```text
results/match/replay_analysis.json
results/match/replay_analysis.md
results/match/coaching_analysis.json
results/match/coaching_report.md
```

## Time model

StarCraft II replay frames are game loops. The decoder uses 16 loops per game-time second:

```text
game_seconds = replay.frames / 16
```

On `Faster`, real elapsed time is shorter. Both values are exported separately as `game_seconds` and `real_seconds`.

## Tests

```bash
python -m pip install -r requirements-dev.txt
pytest -q
```

## Roadmap

- `v0.1`: stable decoder and versioned replay JSON
- `v0.2`: economy, army and worker charts
- `v0.3`: automatic battle and turning-point detection
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
Coaching rules
   ↓
Markdown / JSON / HTML / PDF reports
```

A future Java 25 core is planned for the typed domain model, analysis engine, API and report generation. Python remains a replaceable replay-decoder adapter.

## License and trademarks

The repository uses the Apache License 2.0.

This project is not affiliated with or endorsed by Blizzard Entertainment. StarCraft and StarCraft II are trademarks of Blizzard Entertainment.
