# Information Engine - Architecture Review

## Review metadata

- Change ID: `information-engine-architecture-review`
- Lifecycle gate: `REVIEW`
- Base branch: `develop`
- Review artifact branch / PR: `agent/openspec-workflow`, PR #66, head `aef1e7e846b196831011325b4b4e7250609a7efc` before this review update
- Implementation branch or PR inspected: merged PR #63, `agent/information-engine-v1` -> `develop`
- Implementation commit reviewed: PR head `4bf4322fbeb1cc18a22a13b71851c74de7f9a316`; merge commit `8e3728b1b083c44efc48aadbd8ca67efb50586a5`; current `origin/develop` `5fc9150251d418811f7c5d2dc515fb6d83a56858`
- Reviewer: OpenClaw
- Review date: 2026-08-05

## Existing architectural context

Repository-confirmed context:

- The current public product decodes an explicitly uploaded `.SC2Replay`, maps `replay_analysis.json` into Java domain objects, analyzes macro/turning points/combat, and renders a report through REST/React. Evidence: `docs/PROJECT_STATE.md:7`, `docs/PROJECT_STATE.md:11`.
- `java/coach-domain` owns replay-independent deterministic analysis; it must not depend on Spring MVC or React. Evidence: `ARCHITECTURE.md:53`.
- `java/portal` owns orchestration, HTTP, decoder invocation, REST API and runtime safeguards. Evidence: `ARCHITECTURE.md:57`.
- The frontend renders existing facts and must not independently decide why a player won or lost. Evidence: `ARCHITECTURE.md:61`.
- Confidence must distinguish direct measurements and deterministic rules from heuristics. Evidence: `ARCHITECTURE.md:74`, `ROADMAP.md:133`.
- Missing coordinates or intent must not be silently reconstructed. Evidence: `ARCHITECTURE.md:122`, `ROADMAP.md:160`.
- ADR-011 already states that Information Engine is independent from Combat Engine and must use `Potentially Observed` / `Response Candidate` language because replay data lacks a complete vision log and player intent. Evidence: `docs/DECISIONS.md:93`, `docs/DECISIONS.md:101`.
- Project state records Information Engine V1 as domain contracts only and not wired into the public report. Evidence: `docs/PROJECT_STATE.md:41`, `docs/PROJECT_STATE.md:47`.

Implementation evidence:

- The current implementation is `ai.sc2coach.domain.information.InformationEngine`. Evidence: `java/coach-domain/src/main/java/ai/sc2coach/domain/information/InformationEngine.java:18`.
- It accepts `ReplayAnalysis` directly and returns `InformationReport`. Evidence: `InformationEngine.java:44`.
- It defines heuristic thresholds as private constants: enemy-area radius, potential vision radius, contact gap, max episode length, response window and short-scout cutoff. Evidence: `InformationEngine.java:20` through `InformationEngine.java:25`.
- It defines an extensible constructor for normalized scout units. Evidence: `InformationEngine.java:27`, `InformationEngine.java:38`.
- Output records exist for episodes, observations, gaps, reactions, report, state, advantage and confidence. Evidence: `InformationEpisode.java:6`, `InformationObservation.java:5`, `InformationReaction.java:5`, `InformationState.java:5`, `InformationAdvantage.java:5`, `InformationConfidence.java:5`, `InformationReport.java:5`.
- Tests cover the requested synthetic scenarios. Evidence: `InformationEngineTest.java:16`, `InformationEngineTest.java:41`, `InformationEngineTest.java:57`, `InformationEngineTest.java:79`, `InformationEngineTest.java:94`, `InformationEngineTest.java:108`, `InformationEngineTest.java:123`, `InformationEngineTest.java:144`.
- No Spring bean, REST DTO or frontend rendering currently references `InformationEngine`. Evidence: `rg` found only `domain/information` references plus no imports from `java/portal` or `frontend`; `AnalysisEngineConfiguration` declares `CombatEngine` and `CombatNarrativeEngine` beans but no `InformationEngine` bean (`AnalysisEngineConfiguration.java:33`, `AnalysisEngineConfiguration.java:34`).
- A separate `domain/scouting` implementation also exists and detects scouting episodes, potential observations and response candidates. Evidence: `ScoutingEpisodeDetector.java:13`, `ScoutingEpisodeDetector.java:25`, `ScoutingEpisodeDetector.java:76`, `ScoutingEpisodeDetector.java:112`.

## Problem definition

The Information Engine solves the analytical problem of reconstructing plausible information state from replay-derived facts without claiming actual player vision or intent.

It should answer:

- what a player potentially could have learned from scouting;
- what important categories remained unknown or poorly covered;
- which later actions are compatible with a possible reaction to that information;
- how complete, partial or uncertain that information was.

It is distinct from:

- replay decoding: the Python decoder exposes low-level events and coordinates, but does not own product analysis;
- match context: context computes economy/army/supply state, not scouting knowledge;
- decision detection: decision engines label actions, but must not infer what caused them unless information evidence is supplied;
- turning points: turning points identify score swings, not knowledge state;
- combat detection: combat reconstructs engagements and losses, not scouting;
- knowledge rules: knowledge rules may later consume Information Engine outputs to generate advice;
- narrative rendering: narrative may render information findings, but should not invent causal wording.

## Responsibility boundary

### Owns

The Information Engine owns these domain responsibilities:

- normalize replay-derived scouting evidence from `ReplayAnalysis.TimelineEvent`;
- define eligible scout units and scout-contact episodes;
- emit `InformationEpisode` records with scout player, target player/team, scout unit, start, end, survival and confidence;
- emit `Potentially Observed` observations for nearby economy, technology, upgrade and army facts;
- emit `Missing Information` gaps when scouting is short, absent or spatially incomplete;
- emit `Response Candidate` records for later actions in a configured timing window;
- derive per-player `InformationState` entries with `KNOWN`, `UNKNOWN` or `POTENTIALLY_KNOWN`;
- expose an `InformationAdvantage` model without a single opaque score;
- attach confidence and evidence/provenance semantics to every non-direct conclusion.

### Does not own

The Information Engine must not own:

- Blizzard replay parsing or event extraction;
- map/base geometry extraction unless supplied as a decoder or map-data input;
- combat clustering, loss attribution or combat winner selection;
- macro score, turning-point or decision detection;
- recommendation generation;
- final user-facing prose if that prose belongs to a presentation or narrative layer;
- REST serialization policy unless the integration change explicitly adds it;
- replay retention, uploads or portal runtime concerns.

### Module placement

The owning module is `java/coach-domain`.

Justification: the engine is replay-independent deterministic domain analysis over `ReplayAnalysis`, matching `ARCHITECTURE.md:53`. It should not live in `java/portal` because portal owns HTTP and runtime concerns (`ARCHITECTURE.md:57`). It should not live in `frontend` because presentation must not invent match causality (`ARCHITECTURE.md:61`).

## Inputs

### `ReplayAnalysis`

- Type: `ai.sc2coach.domain.ReplayAnalysis`.
- Producer: Python decoder output mapped by Java reader.
- Required fields: `players`, `timeline`; both default to empty lists when absent (`ReplayAnalysis.java:20`, `ReplayAnalysis.java:21`).
- Optional fields: replay metadata, focus player, transcript markdown.
- Timestamp semantics: `TimelineEvent.time` is a replay-relative `Double` in seconds (`ReplayAnalysis.java:72`).
- Player/team semantics: `ReplayAnalysis.Player` carries `pid`, `name`, `race`, `team`, result and stats (`ReplayAnalysis.java:38`). `TimelineEvent.player` may be numeric pid or name (`ReplayAnalysis.java:74`).
- Coordinate semantics: `TimelineEvent.position` and `target_position` are optional x/y coordinates (`ReplayAnalysis.java:83`, `ReplayAnalysis.java:84`, `ReplayAnalysis.java:89`).
- Ownership semantics: death ownership may be represented by victim/player/attributes; ADR-006 must remain authoritative for loss attribution (`docs/DECISIONS.md:50`).
- Behaviour when absent: the engine must omit or downgrade information when timestamps, owners or coordinates are missing. Current implementation returns empty report for null analysis (`InformationEngine.java:44`) and skips observations without positions (`InformationEngine.java:130`).

### Engine configuration

- Type: proposed `InformationEngineConfig`.
- Current implementation: private constants in `InformationEngine` (`InformationEngine.java:20` through `InformationEngine.java:25`) plus constructor-injected scout unit set (`InformationEngine.java:38`).
- Required fields: scout-unit registry, enemy-area/contact thresholds, potential-vision radius, response window, confidence weights, max output limits.
- Behaviour when absent: default config should be versioned and reported in diagnostics or metadata.

### Optional future inputs

- Normalized map/base regions from decoder or map metadata.
- Unit identity/lifecycle records from a transcript-first model.
- Decision Engine outputs for action labels.
- Combat Detector outputs as downstream context only; Information Engine must not depend on combat outputs.

## Outputs

### `InformationReport`

- Current type: `InformationReport(List<InformationEpisode>, List<InformationState>, InformationAdvantage, List<InformationNarrative>)` (`InformationReport.java:5`).
- Consumer: future Knowledge Engine, Coach Feed, REST, Markdown and React integrations.
- Invariants: deterministic ordering by episode start and stable state order by player order.
- Serialization impact: none today; future REST integration will add new response fields and must be versioned or backward-compatible.

### `InformationEpisode`

- Current type: `InformationEpisode` with scout player, target player/team, scout unit, start/end, survived, confidence and lists (`InformationEpisode.java:6`).
- Required invariant: `end >= start`; current record enforces this (`InformationEpisode.java:20`).
- Missing requirement: stable episode identity and source evidence references are absent.

### `InformationObservation`

- Current type: `InformationObservation(type, subject, time, coordinates, distance, confidence)` (`InformationObservation.java:5`).
- Semantics: `Potentially Observed`, never confirmed player vision.
- Missing requirement: no source event ID, owner/target, derivation category, config version or evidence reference.

### `InformationGap`

- Current type: `InformationGap(topic, reason, confidence)` (`InformationGap.java:3`).
- Semantics: explicit unknown or poorly covered category.
- Missing requirement: no topic enum, no supporting episode/evidence reference, no scope (target player/team/base).

### `InformationReaction`

- Current type: `InformationReaction(player, action, time, delaySeconds, basis, confidence)` (`InformationReaction.java:5`).
- Semantics: `Response Candidate`, not causal explanation.
- Missing requirement: no source action event reference, no relation to specific observation IDs, no derivation category.

### `InformationState` and `InformationAdvantage`

- Current type: per-player state entries with topic, knowledge enum and evidence strings (`InformationState.java:5`), grouped under `InformationAdvantage` (`InformationAdvantage.java:5`).
- Required invariant: no opaque single score.
- Missing requirement: does not represent target-specific knowledge; `KNOWN` is not currently produced by the implementation.

### `InformationNarrative`

- Current type: domain record that renders a text string from an episode (`InformationNarrative.java:5`).
- Required language invariant: uncertain wording; tests assert it avoids "игрок увидел" and "решил потому что" (`InformationEngineTest.java:144`).
- Architectural concern: string rendering inside `java/coach-domain` may become presentation coupling if reused as final report prose.

## Processing model

Current stages:

1. Event normalization: wraps `ReplayAnalysis.TimelineEvent` in private `EventView`. Category: direct fact normalization. Evidence: `InformationEngine.java:430`.
2. Scout eligibility: checks normalized unit names against configured set. Category: configurable heuristic. Evidence: `InformationEngine.java:27`, `InformationEngine.java:378`.
3. Enemy-area anchoring: treats scout coordinates near opponent informative events as base/contact evidence. Category: configurable heuristic. Evidence: `InformationEngine.java:279`.
4. Contact continuation and end selection: groups scout samples and ends on scout death or contact timeout. Category: configurable heuristic. Evidence: `InformationEngine.java:80`, `InformationEngine.java:302`, `InformationEngine.java:400`.
5. Potential observation extraction: emits nearby opponent economy/tech/upgrade/army facts inside potential vision radius. Category: configurable heuristic over direct facts. Evidence: `InformationEngine.java:130`.
6. Missing information: emits categories absent from the potential observation set or shortened by scout death. Category: heuristic hypothesis. Evidence: `InformationEngine.java:163`.
7. Reaction candidate detection: scans the next 90 seconds and emits later own actions. Category: heuristic correlation, not causation. Evidence: `InformationEngine.java:190`, `InformationEngine.java:227`.
8. Information state construction: summarizes per-player potentially known categories. Category: deterministic derivation from emitted observations. Evidence: `InformationEngine.java:245`.
9. Narrative text construction: transforms episode into Russian prose. Category: presentation-only transformation. Evidence: `InformationNarrative.java:5`.

No stage is AI or nondeterministic.

## Integration map

```text
Python decoder
  -> ReplayAnalysis / TimelineEvent
      -> Information Engine
          -> InformationReport
              -> future Knowledge Engine / Coach Feed / REST / Markdown / React

Combat Engine / Combat Detector V3
  -> may consume InformationReport later as context
  -> must not be an input to Information Engine

Decision Engine
  -> future action labels may be consumed by Information Engine
  -> current implementation scans raw lifecycle/upgrade events itself
```

Relationship evaluation:

- Python decoder/transcript: current input is `ReplayAnalysis`; transcript-first normalized records are deferred but likely better for source IDs and unit lifecycle.
- Java domain mapping: correct current module boundary; engine is in `java/coach-domain`.
- Match Context Engine: no dependency today; future economy/expansion state could improve gaps, but must remain explicit input.
- Decision Engine: no dependency today; response candidates duplicate simple decision-like detection. Future design should consume normalized decision/action labels rather than duplicate action classification.
- Turning Point Engine: no dependency; may later use information gaps as explanatory context.
- Combat Engine / Detector V3: no dependency and should stay downstream-only. ADR-011 requires this.
- Knowledge Engine: likely downstream consumer for recommendations.
- Combat Narrative Engine: must remain separate; Information Narrative should not be mixed with combat narrative.
- Coach Feed and REST: no integration today (`AnalysisResponse` has no information field and `AnalysisService` does not import `InformationEngine`).
- React and Markdown: no integration today; future rendering must consume structured facts and avoid causal wording.

## Invariants

Required invariants:

- Deterministic output for identical `ReplayAnalysis` and engine configuration.
- Stable timestamp semantics: all emitted times are replay-relative durations.
- Stable player/team attribution: player names map from replay players; team scope must be explicit where used.
- Fact/derivation/hypothesis separation: direct replay event, potential observation and response candidate must be distinguishable.
- Explicit missing-data handling: missing coordinates/ownership/timestamps must omit or lower confidence, not be inferred.
- No actual-vision claim: use Potentially Observed language only.
- No causal-intent claim: use Response Candidate language only.
- No opaque information advantage score.
- Evidence traceability: every emitted item must point to replay-derived source records and configuration used.

Current implementation satisfies determinism in a synthetic sense but does not yet expose source evidence references or derivation categories in the output records.

## Failure and degradation behaviour

Required behaviour:

- Null analysis returns an empty report, not an exception. Current implementation does this (`InformationEngine.java:44`).
- Empty or missing timeline returns empty episodes and unknown states.
- Missing coordinates prevent Potentially Observed facts or lower confidence; they must not be reconstructed.
- Missing owner/player identity prevents attribution-sensitive items.
- Contradictory ownership must be recorded as a gap or low-confidence condition, not silently assigned.
- Unknown units/upgrades should be emitted as raw canonical-safe identifiers only if display-name policy allows it; otherwise keep raw data internal.
- Replay without scouting must produce no reaction candidates. Current test covers this (`InformationEngineTest.java:108`).

## Current implementation findings

### Matches intended architecture

- `InformationEngine` is in `java/coach-domain`, which matches module boundaries. Evidence: `InformationEngine.java:18`, `ARCHITECTURE.md:53`.
- It is not wired into portal, frontend, Combat Engine, Combat Detector V3 or Combat Narrative. Evidence: `AnalysisEngineConfiguration.java:33`, `AnalysisEngineConfiguration.java:34`; `rg` found no portal/frontend imports.
- It uses `Potentially Observed` / response-candidate language and tests uncertain Russian narrative wording. Evidence: `docs/DECISIONS.md:101`, `InformationEngine.java:232`, `InformationEngineTest.java:144`.
- It does not compute a single Information Advantage score. Evidence: `InformationAdvantage.java:5`.
- It distinguishes observations, gaps, reactions, state and confidence as separate records. Evidence: `InformationObservation.java:5`, `InformationGap.java:3`, `InformationReaction.java:5`, `InformationState.java:5`, `InformationConfidence.java:5`.
- It covers the requested synthetic scenarios. Evidence: `InformationEngineTest.java:16`, `InformationEngineTest.java:41`, `InformationEngineTest.java:57`, `InformationEngineTest.java:79`, `InformationEngineTest.java:94`, `InformationEngineTest.java:108`.

### Deviations

#### Major: output lacks source evidence references

- Evidence: `InformationObservation` has type/subject/time/coordinates/distance/confidence only (`InformationObservation.java:5`); `InformationReaction` has player/action/time/delay/basis/confidence only (`InformationReaction.java:5`); `InformationGap` has topic/reason/confidence only (`InformationGap.java:3`).
- Consequence: downstream consumers cannot explain which replay event(s), scout positions or config thresholds produced an item. This weakens the Explainability Contract and blocks robust APPLY/VERIFY.
- Recommended correction: add source-evidence records with event ID or stable synthetic event reference, timestamps, player/team, derivation category, and config version/thresholds.
- Ownership: current implementation PR if not yet publicly integrated; otherwise a follow-up APPLY change before REST/report integration.

#### Major: duplicated scouting responsibilities

- Evidence: `domain/scouting/ScoutingEpisodeDetector` detects scout deaths, potential observations and response candidates (`ScoutingEpisodeDetector.java:25`, `ScoutingEpisodeDetector.java:76`, `ScoutingEpisodeDetector.java:112`). `domain/information/InformationEngine` now performs overlapping detection (`InformationEngine.java:44`, `InformationEngine.java:130`, `InformationEngine.java:190`).
- Consequence: two engines can emit different scouting semantics, thresholds and confidence for the same replay.
- Recommended correction: explicitly deprecate `domain/scouting`, fold it into Information Engine, or make it an internal evidence extractor used by Information Engine.
- Ownership: required before wiring public API/report output.

#### Major: heuristic thresholds are hard-coded and not surfaced

- Evidence: private constants at `InformationEngine.java:20` through `InformationEngine.java:25`.
- Consequence: results are deterministic but not reproducible by downstream consumers because thresholds and config version are absent from output.
- Recommended correction: introduce `InformationEngineConfig` with versioned defaults and expose config metadata in diagnostics or report metadata.
- Ownership: required before public integration.

#### Major: scout identity is not stable

- Evidence: contact grouping uses owner + unit name (`InformationEngine.java:373`) and first death uses owner + unit name within a time window (`InformationEngine.java:302`).
- Consequence: multiple same-unit scouts from the same player can be merged or assigned the wrong death.
- Recommended correction: consume unit tags/lifecycle identities when decoder exposes them; until then emit a missing-identity confidence factor and cap confidence for ambiguous cases.
- Ownership: follow-up implementation before claims on real replays.

#### Major: Information State does not model target-specific knowledge

- Evidence: `InformationState.Entry` contains topic, knowledge and evidence strings only (`InformationState.java:10`); `informationState` aggregates all own observations without target or team scope (`InformationEngine.java:251`).
- Consequence: team games and multi-opponent games cannot answer "what does player A know about player/team B"; `InformationAdvantage` is only a list of states.
- Recommended correction: include target player/team and subject references in `InformationState` entries and `InformationAdvantage`.
- Ownership: required before team-game integration.

#### Minor: `KNOWN` is defined but not emitted

- Evidence: enum has `KNOWN`, `UNKNOWN`, `POTENTIALLY_KNOWN` (`InformationState.java:15`); implementation emits only `UNKNOWN` or `POTENTIALLY_KNOWN` (`InformationEngine.java:266`).
- Consequence: the public contract implies a state that no rule can produce.
- Recommended correction: define when direct known state is valid, or remove/defer `KNOWN`.
- Ownership: capability-spec clarification before APPLY.

#### Minor: narrative rendering lives inside domain core

- Evidence: `InformationNarrative.from` builds Russian prose directly (`InformationNarrative.java:7`).
- Consequence: domain core now contains presentation text; this can be acceptable as a temporary DTO but conflicts with future localization and presentation boundaries if it becomes final report prose.
- Recommended correction: keep `InformationNarrative` as structured narrative facts or move prose generation to a narrative/presentation layer in the integration PR.
- Ownership: before public report integration.

#### Minor: real-replay validation is unavailable

- Evidence: current tests are synthetic only (`InformationEngineTest.java:16` through `InformationEngineTest.java:144`). A real replay sample exists outside git but was not run under this REVIEW gate.
- Consequence: thresholds and unit/contact assumptions are unvalidated against actual decoder output.
- Recommended correction: add a VERIFY/APPLY task using a private replay corpus and record decoder output artifacts without committing private replays.
- Ownership: before public rollout.

### Open questions

- Should `domain/scouting` be removed, deprecated, or retained as a lower-level extractor?
- What stable source-event identity should be used before the decoder exposes unit tags and event IDs?
- Is `InformationNarrative` intended as a domain narrative contract or merely a temporary debugging/string DTO?
- Which downstream consumer should receive Information Engine first: Knowledge Engine, Coach Feed, Markdown, or REST?
- Should response candidates consume Decision Engine outputs rather than raw lifecycle events?
- What is the minimum real-replay validation corpus for scout/information thresholds?

## Alternatives considered

### 1. Information Engine as a distinct domain service

Accepted direction. It matches ADR-011 and keeps scouting/information reasoning out of Combat Engine. It also provides a coherent place for confidence, gaps and response candidates.

### 2. Information extraction distributed across existing engines

Rejected. Placing scouting logic in Decision Engine, Combat Engine, Knowledge Engine and narrative code would duplicate thresholds and encourage causal claims. The current existence of both `domain/scouting` and `domain/information` is already a warning sign.

### 3. Transcript-first normalized information model

Deferred but recommended. A transcript-first model with unit identity, event IDs and provenance would solve several evidence and identity issues. It should not block the current domain review, but should be the preferred path before strong real-replay claims.

### 4. Presentation-oriented aggregation outside the domain core

Rejected for core inference, accepted for rendering. The frontend/Markdown layer may render Information Engine output, but must not decide what was potentially observed or which action is a response candidate.

## Test strategy

Required tests before APPLY/public integration:

- Unit tests for scout-unit registry normalization and custom config.
- Unit tests for null/empty/missing coordinate/missing owner inputs.
- Determinism test for stable ordering and identities.
- Evidence-reference tests asserting every observation, gap and reaction carries source evidence.
- Ambiguous multi-scout tests for same owner and same unit type.
- Team-game tests with two opponents and target-team scoped Information State.
- Negative tests: far-away enemy events, no scouting, missing coordinates, and action after response window must not produce response candidates.
- Contract tests for REST serialization once API output is added.
- Real-replay validation using saved private replays outside git, with artifacts recording decoder schema and Information Engine output.

## Migration and compatibility

Current implementation has no REST, Markdown, support-bundle or frontend impact because it is not wired into `AnalysisResponse` or `AnalysisService`.

Future integration will require:

- a backward-compatible optional field in `AnalysisResponse`, or a versioned API response;
- support-bundle inclusion of Information Engine output and source evidence;
- frontend/Markdown rendering based only on structured fields;
- explicit defaulting for older support bundles without information output;
- a versioned config/engine identifier for replaying analysis.

## Recommendation

ACCEPT WITH REQUIRED CHANGES

The direction is correct: Information Engine is independent, placed in `java/coach-domain`, not wired into combat or presentation flow, and uses uncertain language. However, it is not ready for public API/report integration until the major deviations above are addressed.

Minimum conditions for entering APPLY:

- define source-evidence references and derivation categories in the capability spec;
- resolve the relationship between `domain/scouting` and `domain/information`;
- move heuristic thresholds into versioned configuration;
- define target-scoped Information State / Advantage;
- decide whether domain narrative emits structured facts or rendered prose;
- add missing-data, determinism, ambiguity and real-replay validation tasks.
