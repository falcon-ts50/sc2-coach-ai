# SC2 Coach Roadmap

## Vision

SC2 Coach explains why a StarCraft II match changed and what a player can try differently next time. It is not merely a replay-statistics viewer.

The product evaluates decisions, not people. Every conclusion should be:

- reproducible from replay-derived facts;
- explainable with timestamps and evidence;
- honest about uncertainty;
- deterministic in the core analysis pipeline;
- available only after explicit replay upload or CLI invocation;
- reusable by web, Markdown and future API consumers.

Optional AI may later turn structured facts into richer prose, but it must not replace the evidence-producing core.

## Product horizons

### NOW — deploy and test the free MVP

Completed:

- replay decoding with `sc2reader` and Blizzard `s2protocol`;
- versioned machine-readable replay output;
- Java 25 domain model;
- relative player context for economy, army and supply;
- lead history and measured match leader;
- first decision detectors;
- turning-point detection;
- Knowledge Engine and typed recommendations;
- confidence and evidence;
- Coach Feed;
- React upload-and-report interface;
- Markdown download;
- single-container deployment;
- hardened upload and runtime security.

Immediate validation work:

- deploy the current `main` behind HTTPS;
- process a diverse real-replay corpus;
- record decoder failures and false conclusions;
- verify 1v1 and team-game behaviour separately;
- improve the density and usefulness of the report before adding infrastructure-heavy features.

### NEXT — richer deterministic match understanding

#### Readable replay transcript

Produce a chronological, human- and AI-readable representation of the match. It should include, where available:

- players, teams, races, results and MMR;
- unit and structure creation/completion;
- upgrades and technology milestones;
- commands, targets and ability use;
- deaths and ownership changes;
- periodic economy, army, supply and resource snapshots;
- engagements and detected decisions;
- camera events;
- unit, target and event coordinates exposed by the replay protocol.

The transcript must distinguish facts, derived events and heuristics. Spatial data must retain timestamp, player, entity identity and coordinate semantics.

#### Argument-delta analysis

Move from sparse final scores to explicit changes over intervals:

- worker and income deltas;
- army-value and army-loss deltas;
- supply and production deltas;
- resource-bank growth or spending windows;
- before/after context around decisions and turning points;
- teammate synchronization in team games;
- recovery time after major losses;
- whether an advantage was converted or allowed to decay.

This is the primary path to reports that approach a human coaching review without requiring an LLM.

#### Report and UI improvements

- denser Coach Feed;
- expandable evidence;
- timeline charts;
- relative economy and army curves;
- better team comparison;
- improved Markdown structure;
- localization through a stable message catalogue;
- operational metrics and clearer user-facing errors.

### LATER — data-backed Pro features

These features require persistence, a replay corpus or substantially more compute:

- accounts and replay history;
- personal progress and recurring weakness detection;
- build-order comparison against curated references;
- league-, MMR- and matchup-specific baselines;
- professional replay reference packs;
- replay and state similarity search;
- vector database;
- counterfactual decision analysis;
- probabilistic outcome estimates with calibrated confidence;
- personalized learning plans;
- billing and resource quotas.

The free product should remain useful without these features.

## Current architecture

```text
.SC2Replay
    -> Python decoder
    -> replay_analysis.json
    -> Java domain model
    -> Context Engine
    -> Decision Engine
    -> Turning Point Engine
    -> Knowledge Engine
    -> Coach Feed
    -> REST
    -> React / Markdown
```

See `ARCHITECTURE.md` for module boundaries and deployment details.

## Design principles

### Evidence before advice

A recommendation without evidence is incomplete.

### Facts, derivations and hypotheses are different

Direct replay events, deterministic calculations and heuristics must carry distinct confidence semantics.

### Explicit processing

No in-game monitoring or background directory watch is required. A replay is processed only after the user explicitly uploads or supplies it.

### Deterministic core

Core conclusions must not depend on nondeterministic LLM output. AI narrative generation may consume the structured transcript later.

### Product-first architecture

A new abstraction is justified only when it improves the current report, removes real duplication or makes a useful rule easier to test.

### Stateless free MVP

The public MVP does not require accounts, persistent replay storage or a database.

### Security is part of the product

Uploaded replay files are untrusted input. Validation, timeouts, concurrency controls and container isolation are required features, not deployment afterthoughts.

## Rule-extension direction

New strategic knowledge should be added through typed Java `KnowledgeRule` implementations. A rule should produce a structured recommendation with category, priority, confidence, explanation, next action and evidence.

Race- and strategy-specific packs may be added later, but YAML DSLs and generic plugin frameworks are deferred until real rules demonstrate the need.

## Non-goals for the current MVP

SC2 Coach does not currently aim to:

- analyze games while they are being played;
- retain replay history;
- claim precise win probabilities;
- infer coordinates or intent absent from replay data;
- hide evidence behind recommendations;
- use an LLM as the deterministic decision engine;
- replace professional coaching.

## Definition of Done for analysis features

An analytical change is complete only when:

- CI passes;
- the conclusion has evidence and confidence;
- the browser or Markdown report becomes more useful;
- user-facing wording does not overstate causality;
- the feature has been checked against real replays, including cases where it should not fire.
