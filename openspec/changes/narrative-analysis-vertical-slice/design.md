# Design: Narrative Analysis Vertical Slice

## Current-state diagnosis

The current pipeline produces multiple useful analytical outputs but composes them primarily as separate report sections and cards. The missing capability is a deterministic layer that converts those outputs into a connected explanation of match development.

The vertical slice must answer a narrower question than the complete future Narrative Analysis Engine:

> Which meaningful phases and measurable state transitions best explain the selected player's match trajectory, and what conservative causal chain connects them?

It must not yet decide whether the official replay result matches the strategic result.

## Architecture

```text
existing engine outputs and replay-derived time series
                    |
                    v
           Narrative Input Normalization
                    |
                    v
             Match State Timeline
                    |
                    v
          State Transition Detection
                    |
                    v
             Phase Segmentation
                    |
                    v
       Minimal Causal Link Assembly
                    |
                    v
 NarrativeTimeline + NarrativeSummary + ChartModel
                    |
                    v
       REST / support bundle / React / Markdown
```

All analytical ownership remains in `java/coach-domain`. `java/portal` maps the result to HTTP/support-bundle contracts. React renders the supplied analysis and does not infer phase boundaries or causality.

## Inputs

The narrative normalizer consumes stable references to existing outputs where available:

- periodic context snapshots and relative economy/army/supply measurements;
- decision records;
- turning points;
- combat episodes, participants, army snapshots and categorized losses;
- information episodes using their existing uncertainty semantics;
- knowledge recommendations and evidence;
- direct replay metadata required for timestamps, players and teams.

Each normalized input carries:

- stable narrative-event ID;
- timestamp or interval;
- player/team scope;
- source engine and source reference;
- derivation class: `FACT`, `DETERMINISTIC_DERIVATION`, or `HEURISTIC`;
- confidence;
- evidence references;
- missing-data markers.

## Domain model

### NarrativeEvent

A normalized reference to an existing fact or analytical output. It does not copy the source engine's detection logic.

Minimum fields:

- `id`;
- `kind`;
- `startTime` and optional `endTime`;
- `scope` (`PLAYER`, `TEAM`, `MATCH`);
- `subjectIds`;
- `sourceType` and `sourceId`;
- `derivationType`;
- `confidence`;
- `evidence`;
- `limitations`.

### MatchStateSnapshot

An explainable state at a specific time. V1 includes, where available:

- selected-player army value;
- opponent/team-relative army context;
- workers or economy proxy;
- supply used/cap;
- bases and production-structure counts as descriptive data;
- active combat pressure markers;
- relevant upgrades/technology milestones;
- team identifiers and teammate context;
- completeness/confidence.

No global power score is introduced.

### StateTransition

A material before/after change between snapshots.

Minimum fields:

- `id`;
- `type`;
- `startTime` and `endTime`;
- `beforeStateRef` and `afterStateRef`;
- absolute and relative deltas used by the detector;
- scope;
- evidence;
- confidence;
- limitations.

Initial transition types should cover:

- `ARMY_COLLAPSED`;
- `ARMY_RECOVERED`;
- `ECONOMY_IMPROVED`;
- `ECONOMY_DECLINED`;
- `SUPPLY_PRESSURE_INCREASED`;
- `SUPPLY_RECOVERED`;
- `PRESSURE_STARTED`;
- `PRESSURE_STABILIZED`;
- `MOMENTUM_IMPROVED`;
- `MOMENTUM_WORSENED`.

Names may be refined, but every transition must remain evidence-backed and explainable.

### MatchPhase

A meaningful interval bounded by material state changes, not fixed clock buckets.

Minimum fields:

- `id`;
- `type` and display label;
- `startTime` and `endTime`;
- entry/exit state references;
- included narrative-event and transition IDs;
- player/team scope;
- boundary reason;
- evidence;
- confidence;
- limitations.

Initial phase vocabulary may include:

- `OPENING`;
- `EARLY_PRESSURE`;
- `ADAPTATION`;
- `STABILIZATION`;
- `COMPETITIVE_MIDGAME`;
- `DETERIORATION`;
- `LATE_COLLAPSE`;
- `RECOVERY_ATTEMPT`;
- `UNCLASSIFIED`.

The vocabulary is descriptive. Full advantage, recovery and strategic-collapse semantics belong to later changes.

### CausalLink

A typed relationship between narrative nodes.

V1 supports:

- `PRECEDED`: confirmed temporal ordering only;
- `CONTRIBUTED_TO`: evidence supports a possible contribution, not exclusive cause;
- `ENABLED`: the source created measurable conditions used by the target;
- `RECOVERED_FROM`: the later state measurably compensates for an earlier deterioration.

Minimum fields:

- `sourceId`;
- `targetId`;
- `type`;
- `derivationType`;
- `confidence`;
- `evidence`;
- `limitations`.

Rules:

- temporal proximity alone can produce only `PRECEDED`;
- heuristic links must never use certainty wording;
- Information Engine `Potentially Observed` remains uncertain;
- no link may claim player intent unless direct replay evidence supports it.

### NarrativeTimeline

Ordered phases, transitions, events and causal links with stable deterministic ordering.

### NarrativeSummary

V1 contains:

- preliminary verdict about match trajectory, not strategic result;
- ordered phase summary;
- principal causal chain;
- strongest measured recovery/adaptation candidate when present;
- major unresolved questions;
- evidence and confidence.

`strategicResultStatus` is present and set to `NOT_EVALUATED` in this change.

### NarrativeChartModel

Backend-owned chart data contract:

- normalized time points;
- army-value series;
- worker/economy-proxy series;
- supply series;
- optional bases and production-structure series;
- combat/turning-point markers;
- phase intervals;
- source completeness and series confidence.

The frontend may format, hide/show, zoom and synchronize series. It must not derive analytical conclusions.

## Processing sequence

1. Normalize source outputs into deterministic `NarrativeEvent` records.
2. Build a unified sorted time grid from existing snapshots and event boundaries.
3. Interpolate only where the source contract explicitly permits it; otherwise retain gaps.
4. Build `MatchStateSnapshot` records.
5. Detect significant transitions using centralized versioned thresholds.
6. Propose phase boundaries at significant transitions, combat boundaries and turning points.
7. Merge adjacent proposals when their state semantics are equivalent and evidence does not justify a boundary.
8. Assign phase labels conservatively.
9. Assemble permitted causal links.
10. Rank links and transitions to select a principal chain.
11. Produce summary and chart model.
12. Map the same domain result to REST, React, Markdown and support-bundle output.

## Phase-boundary strategy

A boundary requires at least one material condition:

- significant army/economy/supply transition;
- start/end of sustained pressure supported by combat evidence;
- stabilization after a measurable decline;
- turning point corroborated by before/after context;
- major technology or production shift combined with a state change.

A single command, isolated death or clock interval is insufficient.

Thresholds must be centralized in a versioned `NarrativeAnalysisConfig` and covered by tests.

Implementation defaults for this APPLY pass:

- config version: `narrative-analysis-config.v1`;
- early phase boundary: first `7:00`;
- mid-game boundary: first `16:00`;
- army swing threshold: `300` resource-value points;
- recovery threshold: first post-decline army recovery of at least `150` resource-value points;
- economy swing threshold: `350` economy-proxy points;
- supply swing threshold: `12` supply;
- overall score swing threshold: `18`;
- default phase confidence: `0.68`;
- default causal-link confidence: `0.64`.

These are deliberately configurable heuristics. They are strong enough to find the fixed benchmark trajectory without encoding that replay's exact timestamps, but they are not treated as universal strategic truth.

## Principal causal chain

The chain is an ordered subset of narrative nodes selected by:

- relevance to the selected player's largest state changes;
- evidence quality;
- chronological continuity;
- coverage of deterioration and recovery;
- minimal redundancy.

The chain must not be forced when fewer than two defensible links exist. In that case the report explicitly states that a reliable causal chain could not be assembled.

## Chart and UI design

Recommended report order:

1. official replay result and narrative-analysis status;
2. preliminary narrative verdict;
3. match-overview chart;
4. chronological phases;
5. principal causal chain;
6. existing detailed sections.

Default chart series:

- army value;
- workers/economy proxy;
- supply.

Additional toggles:

- bases;
- production structures.

Required overlays:

- combat markers;
- phase bands or phase-boundary markers;
- turning-point markers where available.

The chart and phase list share a time axis. Selecting a phase highlights the corresponding interval. Mobile layout may use horizontal scrolling or a simplified series selector, but must retain labels and evidence access.

## REST and compatibility

Additive API fields only. Existing clients and report sections continue to work.

The response should expose a versioned narrative payload containing:

- status;
- timeline;
- summary;
- chart model;
- evidence references;
- limitations.

Stable ordering is mandatory: timestamps, then explicit type order, then stable IDs.

## Markdown parity

Markdown cannot reproduce full chart interaction. It must include:

- the same verdict;
- ordered phases;
- principal causal chain;
- phase time ranges;
- key series values or before/after deltas supporting each phase;
- marker references;
- uncertainty and evidence.

## Missing data

Missing or incompatible samples remain explicit. The engine may reduce confidence, omit a transition, use `UNCLASSIFIED`, or return a partial narrative. It must not synthesize coordinates, vision, intent or exact unseen state.

## Team-game handling in V1

V1 remains selected-player focused but preserves team membership and identifies Lulu as dragonDriver's teammate in the benchmark. Team-level conclusions are limited to source-supported context. Full team synchronization and team strategic-state inference are deferred.

## Alternatives considered

### Frontend-only narrative composition

Rejected because it duplicates business logic, prevents Markdown parity and makes causality hard to test.

### Contracts-only foundation with no UI

Rejected for this phase because the agreed strategy is a product vertical slice validated on the website.

### One global match-power score

Rejected because it hides why the position changed and conflicts with explainability requirements.

### Full strategic-result inference in V1

Deferred because it requires explicit production and recovery-capacity modelling and would make the first change too broad.

### LLM narrative generation

Rejected for deterministic core analysis. It may later paraphrase structured results but cannot own evidence or conclusions.

## Open questions for implementation

The implementation agent must resolve and document, without expanding scope:

- which existing context series is the most defensible economy proxy;
- exact time-grid normalization rules;
- initial transition thresholds;
- chart library choice consistent with the current frontend dependency policy;
- whether bases/production series are sufficiently complete to enable by default or only as optional incomplete series.
