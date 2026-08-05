# Information Engine — Architecture Review

## Review metadata

- Change ID: `information-engine-architecture-review`
- Lifecycle gate: `REVIEW`
- Base branch: `develop`
- Implementation branch or PR inspected: _to be completed_
- Reviewed commit: _to be completed_
- Reviewer: OpenClaw
- Review date: _to be completed_
- Reference support bundle: application `0.7.0`, commit `b30d8ce4d450`, analysis `0a0801bc-deb3-4f87-918b-edec6e40b2b6`

## Existing architectural context

Summarize only repository-confirmed facts from `ARCHITECTURE.md`, `ROADMAP.md`, `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, current implementation/tests and the reference support bundle. Do not use Telegram history as evidence.

The reference bundle currently exposes combat episodes and turning points but no explicit scouting analysis. Treat this as observed product behaviour, not proof of decoder limitations.

## Problem definition

Describe the user-visible and downstream analytical problem the Information Engine solves. It must cover more than combat outcome.

The review shall determine how the system can explain the causal chain:

```text
available replay facts
  -> player-perspective observations
  -> current knowledge / uncertainty
  -> preparation choices between engagements
  -> readiness and strategic posture
  -> later engagement or missed timing
```

Distinguish this responsibility from replay decoding, match context, decisions, turning points, combat detection, knowledge rules and narrative rendering.

## Core analytical concepts

### Omniscient fact

A fact recoverable from the replay regardless of whether a player observed it.

### Observation

Evidence that a player or team acquired information through vision, scouting units, scans, detection, attacks, revealed structures or another repository-supported mechanism.

### Knowledge state

The best deterministic representation of what a player or team had evidence to know at a timestamp. It must preserve uncertainty, staleness and missing evidence and must not silently inherit omniscient replay state.

### Strategic interval

A bounded period used to compare preparation choices. Candidate boundaries include engagements, major attacks, expansions, tech milestones or explicitly configured windows. The review must select and justify canonical boundaries.

### Preparation allocation

Measured change during an interval classified into explainable categories. At minimum evaluate:

- economy: workers and income capacity;
- immediate army: combat units available soon;
- technology and upgrades;
- production capacity;
- expansion and long-term economy;
- static defence;
- information acquisition;
- resource banking and spending tempo.

### Strategic posture

A derived, evidence-backed description such as immediate pressure, defensive stabilization, economic growth, technology transition or delayed power spike. Posture is a heuristic interpretation, not direct intent.

## Responsibility boundary

### Owns

Determine whether Information Engine should own:

- normalization of observations;
- player/team knowledge-state construction;
- interval-level preparation deltas;
- explainable allocation comparison;
- uncertainty and information-staleness semantics;
- structured facts consumed by Decision, Knowledge and Narrative layers.

### Does not own

It must not own raw replay decoding, combat clustering, final combat winner declaration, recommendation wording, UI rendering or unsupported mind-reading of player intent.

### Module placement

State the owning module and justify it against existing module boundaries.

## Inputs

For every input specify type/contract, producer, required and optional fields, timestamp and player/team semantics, confidence/provenance and behaviour when absent.

Explicitly inspect availability of:

- commands and targets;
- unit and structure lifecycle events;
- periodic player-state snapshots;
- coordinates and vision-related evidence;
- scans and detection abilities;
- combat episodes and boundaries;
- upgrades, production structures, workers and expansions;
- resource and supply measurements.

Do not assume camera events or complete vision events exist. Record decoder gaps separately from domain-design decisions.

## Outputs

Define proposed contracts for at least:

1. `Observation` or equivalent;
2. `KnowledgeState` or equivalent;
3. `StrategicInterval`;
4. per-player and per-team `PreparationDelta`;
5. explainable `PreparationComparison`;
6. optional heuristic `StrategicPosture`.

For every output specify consumers, invariants, ordering/identity, serialization impact, confidence, evidence and partial-data flags.

## Processing model

Describe stages in order and classify each as direct fact normalization, deterministic derivation, configurable heuristic or presentation transform.

At minimum evaluate this pipeline:

```text
replay facts
  -> observation extraction
  -> knowledge-state timeline
  -> interval boundary selection
  -> state and production deltas
  -> allocation classification
  -> player/team comparison
  -> posture hypothesis
  -> downstream decisions and narrative
```

## Scouting and information model

Answer explicitly:

- What counts as scouting evidence?
- Can the decoder establish visibility, or only movement/commands/positions?
- How are scans, sacrificial scouts, attacks and revealed structures represented?
- How long does an observation remain current?
- How is stale knowledge represented?
- How are team observations shared, if at all?
- How does the model avoid claiming a player knew an unseen tech switch?
- What can be emitted when visibility evidence is incomplete?

Separate three values when necessary:

- actual omniscient state;
- observed state;
- inferred or stale expected state.

## Between-engagement strategy comparison

The review must define how to compare what participants did between fights without declaring one universally correct.

For each interval evaluate:

- worker count delta;
- army composition and army-value delta;
- production started/completed;
- upgrades and technology started/completed;
- bases and economic infrastructure;
- static defence;
- resources banked/spent;
- scouting actions and newly observed enemy facts;
- teammate synchronization;
- expected readiness horizon when supportable.

The output should support statements such as:

- player A invested in immediate army while player B added workers and production for a later power spike;
- one team entered the next fight with more units, while the other preserved a stronger economic trajectory;
- a tech transition was started before the fight but had not matured;
- a player prepared for pressure without evidence of the opponent's actual composition;
- a player had scouting evidence but did not visibly adapt, subject to confidence limits.

Do not convert these comparisons into an opaque universal efficiency score.

## Integration map

Provide a diagram and explicitly evaluate relationships to Python decoder/transcript, Java mapping, Match Context, Decision, Turning Point, Combat Engine/Detector V3, Knowledge Engine, Combat Narrative, Coach Feed/REST and React/Markdown.

Combat should provide useful interval boundaries and consequences, but Information Engine must remain useful even when no clean combat outcome can be determined.

## Invariants

At minimum:

- evidence traceability;
- deterministic output for identical input/configuration;
- stable timestamps and player/team attribution;
- explicit missing-data handling;
- separation of omniscient fact, observation, knowledge and hypothesis;
- no presentation-layer causality;
- no hidden aggregate score;
- no claim of player knowledge without observation evidence;
- interval deltas reconcile with their source states;
- investment categories remain explainable and non-overlapping or document overlap explicitly.

## Failure and degradation behaviour

Specify behaviour for missing coordinates, visibility, commands, resource snapshots, ownership, transformations, incomplete production lifecycle and contradictory events. Prefer explicit absence or reduced confidence over invented values.

## Current implementation findings

### Matches intended architecture

_To be completed with file and test references._

### Product-level gaps observed

Validate against current code and bundle:

- no explicit scouting or knowledge-state section in report;
- report narrative dominated by combat result and losses;
- no interval-level comparison of economy, production, tech and immediate army choices;
- no clear separation between actual enemy state and what the focus player could know.

Do not mark these as implementation defects until repository evidence confirms ownership and intended scope.

### Deviations

For each deviation record severity, evidence, consequence, correction and ownership.

### Open questions

Record unresolved decisions without assumption.

## Alternatives considered

Evaluate at least:

1. distinct domain Information Engine;
2. extraction distributed across existing engines;
3. transcript-first normalized information model;
4. presentation-oriented aggregation;
5. knowledge-state timeline plus strategic-interval analyser as two cooperating domain services.

## Test strategy

Define unit, contract, integration and real-replay validation. Include:

- early worker scout;
- reaper/overlord scouting;
- scan revealing tech;
- no scouting evidence;
- stale observation after enemy transition;
- team-shared versus unshared information;
- one player producing army while another drones/workers;
- delayed tech power spike;
- production-capacity investment without immediate army growth;
- interval with no combat;
- simultaneous or overlapping fights;
- missing coordinates and incomplete lifecycle events.

## Migration and compatibility

Document JSON, REST, Markdown, frontend, support-bundle and test impact. State versioning requirements.

## Recommendation

Finish with exactly one:

- `ACCEPT CURRENT DIRECTION`;
- `ACCEPT WITH REQUIRED CHANGES`;
- `REDESIGN BEFORE APPLY`.

Support it with repository evidence and minimum APPLY conditions.