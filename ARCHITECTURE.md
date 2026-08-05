# SC2 Coach Architecture

## Purpose

SC2 Coach turns an explicitly uploaded StarCraft II replay into an explainable coaching report. The deterministic core evaluates replay-derived facts and decisions; presentation layers render those results for people and other software.

## Production pipeline

```text
.SC2Replay
    |
    v
Python replay decoder
(sc2reader + Blizzard s2protocol)
    |
    v
Versioned replay_analysis.json
    |
    v
Java 25 domain mapping
    |
    +--> Match Context Engine
    |      - relative economy, army and supply
    |      - lead history and final measured leader
    |
    +--> Decision Engine
    |      - ATTACK and REBUILD from measured changes
    |      - EXPAND and TECH_SWITCH as labelled heuristics
    |
    +--> Turning Point Engine
    |      - largest meaningful score swings
    |
    v
Knowledge Engine
    - typed rules
    - confidence
    - evidence
    - concrete next actions
    |
    v
Coach Feed / REST response
    |
    +--> React browser report
    +--> Markdown download
```

## Module boundaries

### Python decoder

The Python layer owns Blizzard replay-format compatibility and extraction. It may expose low-level events, periodic statistics, units, structures, upgrades, commands and future spatial data. It must not become the main product-rule engine.

### `java/coach-domain`

This module owns replay-independent business concepts and deterministic analysis: match and player state, context, decisions, turning points, evidence, confidence, knowledge rules, recommendations and Coach Feed composition. It must not depend on Spring MVC or React.

### `java/portal`

The portal owns orchestration and HTTP concerns: replay validation, temporary workspace lifecycle, decoder invocation and timeout, REST API, rate and concurrency limits, static React delivery and health endpoints. Strategic rules do not belong here.

### `frontend`

React renders the REST contract. It may format, filter and visualize existing facts, but must not independently decide why a player won or lost.

## Explainability contract

Every recommendation should answer four questions:

1. What happened?
2. Why does it matter?
3. What should the player try next time?
4. Which replay-derived evidence supports the conclusion?

Confidence must reflect the basis of the conclusion. Direct measurements and deterministic rules remain distinguishable from heuristics.

## Security and processing model

Replay processing happens only after an explicit upload or CLI command. The public portal is stateless and does not retain replay history.

Current safeguards include:

- 25 MB upload limit;
- safe filename handling;
- `.SC2Replay` extension and MPQ signature validation;
- per-client upload throttling;
- one in-flight upload per client;
- two global concurrent analyses;
- decoder timeout;
- constrained Tomcat connections and threads;
- non-root read-only container;
- dropped Linux capabilities and `no-new-privileges`;
- temporary writable `tmpfs` only;
- CPU, memory and PID limits;
- Docker healthcheck;
- application bound to `127.0.0.1:18080` behind Caddy.

The in-memory limiter is suitable for the single-instance MVP. A multi-instance service will require a shared limiter or queue.

## Deployment shape

```text
Internet
   |
   v
Caddy :443
   |
   v
127.0.0.1:18080
   |
   v
single Docker container
   - Spring Boot / Java 25
   - embedded React assets
   - Python decoder
```

No Node.js process is required at runtime.

## Near-term extension: replay transcript

The next decoder contract should expose a readable chronological transcript for humans and AI systems. Candidate records include unit and structure creation, upgrades, commands and targets, deaths, periodic player-state snapshots, detected engagements, camera events when available, and unit or target positions when the protocol provides them.

Spatial data must carry explicit coordinate semantics and timestamps. Missing coordinates must never be silently reconstructed.
