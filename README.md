# SC2 Coach AI

SC2 Coach is an open-source StarCraft II replay coaching platform. A player explicitly uploads a `.SC2Replay` file and receives an explainable match report: relative player comparison, lead history, turning points, detected decisions, Coach Feed recommendations, and a downloadable Markdown summary.

The project evaluates decisions rather than players. Every recommendation should be traceable to replay-derived evidence, and heuristics must be labelled as heuristics.

## Current status

The deployable stateless MVP is complete:

- Python decoder based on `sc2reader` and Blizzard `s2protocol`;
- versioned replay JSON contract;
- Java 25 domain and analysis core;
- relative economy, army and supply context;
- lead history and final measured leader;
- decision detection (`ATTACK`, `REBUILD`, heuristic `EXPAND` and `TECH_SWITCH`);
- turning-point detection;
- Knowledge Engine with typed recommendations, evidence and confidence;
- Coach Feed;
- React/Vite browser interface;
- Markdown report download;
- single-container Docker deployment;
- upload validation, rate limiting and hardened runtime configuration.

The current report is intentionally deterministic. It does not use an LLM to invent conclusions or winner probabilities.

## Architecture

```text
.SC2Replay
    -> Python decoder
    -> replay_analysis.json
    -> Java domain model
    -> Context / Decisions / Turning Points
    -> Knowledge Engine
    -> Coach Feed
    -> REST API
    -> React report / Markdown
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for module boundaries, explainability rules and the security model.

## Quick local deployment

Requirements:

- Git;
- Docker Engine;
- Docker Compose plugin;
- approximately 2 GB RAM for the running container;
- additional build memory or swap for Maven and Vite.

```bash
git clone https://github.com/falcon-ts50/sc2-coach-ai.git
cd sc2-coach-ai
chmod +x deploy.sh
./deploy.sh
```

The hardened Compose configuration binds the application only to:

```text
127.0.0.1:18080
```

For public deployment, put Caddy or another HTTPS reverse proxy in front of that address. A sample `Caddyfile` is included; replace `sc2-coach.example.com` with the real domain.

Health check:

```bash
curl http://127.0.0.1:18080/actuator/health
docker compose ps
docker compose logs --tail=100
```

Subsequent updates use the same command:

```bash
./deploy.sh
```

## Security model

The portal currently enforces:

- explicit upload only;
- maximum replay size of 25 MB;
- safe filename handling;
- `.SC2Replay` extension and MPQ archive signature checks;
- up to three uploads per client per minute;
- one in-flight analysis per client;
- two global concurrent analyses;
- decoder timeout;
- constrained Tomcat connections and threads;
- localhost-only backend binding;
- non-root, read-only container;
- dropped Linux capabilities and `no-new-privileges`;
- bounded CPU, memory and PIDs;
- temporary `tmpfs` workspaces and Docker healthcheck.

This is a single-instance MVP. Shared queues, distributed rate limiting, accounts and persistent storage are deliberately deferred.

## Developer workflows

Python tests:

```bash
python -m pip install -r requirements-dev.txt
python -m pytest -q
```

Java tests:

```bash
cd java
mvn --batch-mode --no-transfer-progress verify
```

Frontend development:

```bash
cd frontend
npm ci
npm run dev
```

Production verification:

```bash
docker build -t sc2-coach-ai:test .
```

CI builds React, runs Java tests and builds the full production image.

## Legacy CLI

The original local pipeline remains useful for decoder debugging and detailed artifacts:

```bash
./sc2-coach match.SC2Replay \
  --player dragonDriver \
  --lang ru \
  --out ./results/match
```

Low-level stages:

```bash
sh run.sh match.SC2Replay --player dragonDriver --out ./results/match
sh run_coach.sh ./results/match/replay_analysis.json --player dragonDriver --out ./results/match --lang ru
```

The web MVP currently exports Markdown rather than PDF. Historical CLI PDF and chart code remains in the repository but is not the primary portal output.

## Next work

Near-term priorities are driven by real replay testing:

- richer timeline and argument-delta analysis;
- a readable, AI-friendly replay transcript;
- more complete unit, structure, upgrade and command evidence;
- spatial events and coordinates where the replay protocol exposes them;
- better Coach Feed density and explanations;
- visual charts and timeline polish;
- HTTPS deployment documentation and operational monitoring.

Longer-term Pro features may include replay history, personal trends, reference-replay and build-order comparison, cohort analysis, similar-state search and counterfactual decision analysis.

See [ROADMAP.md](ROADMAP.md) for scope and sequencing.

## License and trademarks

The repository uses the Apache License 2.0.

This project is not affiliated with or endorsed by Blizzard Entertainment. StarCraft and StarCraft II are trademarks of Blizzard Entertainment.
