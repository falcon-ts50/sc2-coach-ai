# Information Engine - Architecture Review

## Review metadata

- Change ID: `information-engine-architecture-review`
- Lifecycle gate: `REVIEW`
- Base branch: `develop`
- Review artifact branch / PR: `agent/information-engine-architecture-review`, PR #67, head `14fe819e3df8251af191505a9afb2fce0c8e60f1` before this follow-up review update
- Implementation branch or PR inspected: merged PR #63, `agent/information-engine-v1` -> `develop`
- Implementation commit reviewed: PR head `4bf4322fbeb1cc18a22a13b71851c74de7f9a316`; merge commit `8e3728b1b083c44efc48aadbd8ca67efb50586a5`; current `origin/develop` `6a1e226a0ed4ef513f999807ee44e6f6df6c4d54`
- Support-bundle/report behaviour reviewed: application version `0.7.0`, commit `b30d8ce4d450`
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
- Project state explicitly plans Information Engine wiring only after real replay support-bundle validation. Evidence: `docs/PROJECT_STATE.md:58`.

Implementation evidence:

- The current implementation is `ai.sc2coach.domain.information.InformationEngine`. Evidence: `java/coach-domain/src/main/java/ai/sc2coach/domain/information/InformationEngine.java:18`.
- It accepts `ReplayAnalysis` directly and returns `InformationReport`. Evidence: `InformationEngine.java:44`.
- It defines heuristic thresholds as private constants: enemy-area radius, potential vision radius, contact gap, max episode length, response window and short-scout cutoff. Evidence: `InformationEngine.java:20` through `InformationEngine.java:25`.
- It defines an extensible constructor for normalized scout units. Evidence: `InformationEngine.java:27`, `InformationEngine.java:38`.
- Output records exist for episodes, observations, gaps, reactions, report, state, advantage and confidence. Evidence: `InformationEpisode.java:6`, `InformationObservation.java:5`, `InformationReaction.java:5`, `InformationState.java:5`, `InformationAdvantage.java:5`, `InformationConfidence.java:5`, `InformationReport.java:5`.
- Tests cover the requested synthetic scenarios. Evidence: `InformationEngineTest.java:16`, `InformationEngineTest.java:41`, `InformationEngineTest.java:57`, `InformationEngineTest.java:79`, `InformationEngineTest.java:94`, `InformationEngineTest.java:108`, `InformationEngineTest.java:123`, `InformationEngineTest.java:144`.
- No Spring bean, REST DTO or frontend rendering currently references `InformationEngine`. Evidence: `rg` found only `domain/information` references plus no imports from `java/portal` or `frontend`; `AnalysisEngineConfiguration` declares `CombatEngine` and `CombatNarrativeEngine` beans but no `InformationEngine` bean (`AnalysisEngineConfiguration.java:33`, `AnalysisEngineConfiguration.java:34`).
- A separate `domain/scouting` implementation also exists and detects scouting episodes, potential observations and response candidates. Evidence: `ScoutingEpisodeDetector.java:13`, `ScoutingEpisodeDetector.java:25`, `ScoutingEpisodeDetector.java:76`, `ScoutingEpisodeDetector.java:112`.
- The current domain state model contains workers, current minerals/gas, collection rates, army value/losses and supply. Evidence: `ReplayAnalysis.java:55`, `ReplayAnalysis.java:72`, `PlayerState.java:39`, `ReplayDomainMapper.java:36`.
- Existing strategic deltas are limited to economy, army, supply and overall score changes between context frames. Evidence: `ArgumentDeltaEngine.java:24`, `ArgumentDeltaEngine.java:26`, `ArgumentDeltaEngine.java:28`, `ArgumentDeltaEngine.java:30`.
- Existing support bundle generation in the React app writes only `report.md`, `transcript.md`, `analysis-response.json` and `metadata.json`. Evidence: `frontend/src/main.jsx:76` through `frontend/src/main.jsx:82`.

## Problem definition

The Information Engine solves the analytical problem of reconstructing plausible information state from replay-derived facts without claiming actual player vision or intent.

It should answer:

- what a player potentially could have learned from scouting;
- what important categories remained unknown or poorly covered;
- which later actions are compatible with a possible reaction to that information;
- how complete, partial or uncertain that information was.
- how the player or team prepared in the interval before a later fight, based on what they could plausibly know and what actually existed in the replay.

It is distinct from:

- replay decoding: the Python decoder exposes low-level events and coordinates, but does not own product analysis;
- match context: context computes economy/army/supply state, not scouting knowledge;
- decision detection: decision engines label actions, but must not infer what caused them unless information evidence is supplied;
- turning points: turning points identify score swings, not knowledge state;
- combat detection: combat reconstructs engagements and losses, not scouting;
- knowledge rules: knowledge rules may later consume Information Engine outputs to generate advice;
- narrative rendering: narrative may render information findings, but should not invent causal wording.

Two separate analytical layers are required:

1. Player-perspective information state. This compares omniscient replay facts with the subset a player or team potentially could have observed, when the information was acquired, when it became stale, and which conclusions remain forbidden because replay visibility evidence is incomplete.
2. Strategic preparation between engagements. This explains how players and teams allocated time and resources between fights: workers and future economy, immediate army, production capacity, technology and upgrades, expansions, static defence, scouting, resource-bank accumulation/spending, allied synchronization and readiness for the next engagement or later power spike.

Neither layer is a combat winner detector or a tactical trade-efficiency score.

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
- define player- and team-perspective knowledge state over time, including acquisition and staleness;
- define between-engagement preparation intervals and classify player/team investments inside those intervals;
- connect information state, preparation choices and later engagements with non-causal language;
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
- choosing the winner of a combat episode or reducing strategic preparation to combat efficiency.

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

### Engagement boundaries for preparation intervals

- Type: current `Combat` episodes and future Combat Detector V3 clusters.
- Producer: current production `CombatEngine` or future `CombatClusterer`/`CombatAssembler`.
- Required fields: stable engagement id, start/end, participants, teams and confidence.
- Usage boundary: Information Engine may use engagement boundaries to define preparation intervals and may attach information/preparation context to a later fight. It must not use combat winner or trade outcome to decide what the player knew.
- Behaviour when absent: fall back to decision/turning-point windows only with lower confidence, or emit no preparation comparison when interval boundaries are unreliable.

### Preparation source facts

Current replay/domain data can support partial preparation analysis:

- workers, minerals/gas bank, income rates, supply used/cap and army value/losses from `PlayerStat`/`PlayerState`;
- unit, structure, upgrade, ability and command-like timeline events from `TimelineEvent`;
- coordinates when present for scouting/contact and local context.

Current data does not yet provide a complete vision log, production queue semantics, exact resource spend per item, stable unit tags, or official map/base geometry. These gaps must become explicit confidence factors rather than hidden assumptions.

## Outputs

### `InformationReport`

- Current type: `InformationReport(List<InformationEpisode>, List<InformationState>, InformationAdvantage, List<InformationNarrative>)` (`InformationReport.java:5`).
- Consumer: future Knowledge Engine, Coach Feed, REST, Markdown and React integrations.
- Invariants: deterministic ordering by episode start and stable state order by player order.
- Serialization impact: none today; future REST integration will add new response fields and must be versioned or backward-compatible.

### `ReplayFact` / omniscient fact layer

Proposed type: replay-derived fact record used internally or exposed in diagnostics.

Fields:

- stable fact id;
- time and optional expiry;
- player/team/owner;
- subject and category;
- coordinates when present;
- source event reference;
- fact basis: direct replay event, mapped domain snapshot, deterministic derivation or heuristic.

Purpose: separate "what existed in the replay" from "what a player potentially could know".

### Player-perspective `InformationState`

Proposed extension of current `InformationState`.

Fields:

- perspective player and perspective team;
- target player and/or target team;
- topic: economy, expansions, army tech, upgrades, army composition, static defence, production capacity, resource bank, scouting coverage;
- state: `UNKNOWN`, `POTENTIALLY_KNOWN`, and only `KNOWN` when a future exact-visibility rule exists;
- acquired at;
- stale after;
- supporting Potentially Observed evidence;
- missing/contradictory evidence;
- confidence.

The current implementation has per-player state entries but no target scope, no acquisition/staleness time, no source evidence and no visibility-backed `KNOWN` rule.

### `StrategicPreparationInterval`

Proposed type for between-engagement analysis.

Fields:

- stable interval id;
- from/to;
- preceding engagement id when known;
- following engagement id when known;
- participating players/teams;
- per-player and per-team `PreparationProfile`;
- information state at interval start/end;
- readiness against the next engagement or named later power spike;
- confidence and missing-data markers.

The interval is not a combat. It is the analytical bridge between "what could be known" and "what players prepared before the next fight."

### `PreparationProfile`

Proposed type for each player or team inside an interval.

Categories:

- workers and future economy;
- resource-bank accumulation and spending;
- immediate army value/composition;
- production capacity;
- technology structures and upgrades;
- expansions;
- static defence;
- scouting and scans/information acquisition;
- allied synchronization in team games.

Every category must report raw measured changes, inferred classification, confidence and evidence. No single opaque preparation score is allowed.

### `PreparationComparison`

Proposed type comparing players or teams for one interval.

It should explain trade-offs rather than declare a winner. Examples:

- player A invested in future economy while player B built immediate army;
- team 1 started upgrades and production together while team 2 prepared asynchronously;
- player A banked resources instead of converting them into army before the next engagement;
- a player was ready for the immediate fight but behind the opponent's later upgrade/power spike.

The output language must use "consistent with", "could explain" or "may be a response candidate", never "because".

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

Target processing stages:

1. Omniscient fact normalization: create typed `ReplayFact` records from timeline events and sampled player stats. Category: direct fact normalization.
2. Visibility/perspective projection: project replay facts into player/team perspective only through scout paths, scans, overlords, observers or other evidence-backed contact. Category: configurable heuristic unless exact visibility is supplied later.
3. Information lifecycle: record acquisition time, stale-after time and invalidation triggers for each potentially known item. Category: deterministic derivation over potential observations plus heuristic expiry.
4. Information gaps: emit unknown categories caused by early scout death, incomplete spatial coverage, missing coordinates, missing owner/team or stale data. Category: heuristic hypothesis with explicit missing evidence.
5. Preparation interval construction: build intervals between engagement boundaries, or lower-confidence windows from turning points/decisions when engagement boundaries are unavailable. Category: deterministic boundary derivation plus heuristic fallback.
6. Preparation allocation extraction: classify per-player/team changes in workers/economy, bank/spend, immediate army, production, tech/upgrades, expansions, static defence and scouting. Category: direct deltas plus deterministic classification rules.
7. Team synchronization analysis: compare timing of allied investment and army readiness within a team. Category: deterministic derivation from interval profiles.
8. Readiness and power-spike context: compare interval output against the next engagement start and later known upgrade/tech timing. Category: heuristic, never a combat winner claim.
9. Correlation explanation: connect potentially observed information to preparation and later engagement using response-candidate language only. Category: presentation/knowledge-support transform over structured facts.

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

Combat / Combat Detector V3
  -> engagement boundaries may define preparation intervals
  -> combat winner/outcome must not feed information-state inference
```

Relationship evaluation:

- Python decoder/transcript: current input is `ReplayAnalysis`; transcript-first normalized records are deferred but likely better for source IDs and unit lifecycle.
- Java domain mapping: correct current module boundary; engine is in `java/coach-domain`.
- Match Context Engine: no dependency today; future economy/expansion state could improve gaps, but must remain explicit input.
- Decision Engine: no dependency today; response candidates duplicate simple decision-like detection. Future design should consume normalized decision/action labels rather than duplicate action classification.
- Turning Point Engine: no dependency; may later use information gaps as explanatory context.
- Combat Engine / Detector V3: no dependency and should stay downstream-only. ADR-011 requires this.
- Preparation intervals: should consume engagement boundaries and context snapshots, but should not become a combat-efficiency score.
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
- No omniscient leakage: a player-perspective state cannot include a replay fact unless there is scout/contact/vision evidence or the item is explicitly marked unknown/unavailable.
- Information staleness is explicit: acquired information must have a stale-after time or documented "unknown staleness" marker.
- Preparation analysis is interval-scoped and cannot use later facts as if the player knew them at interval time.
- Preparation comparisons list category trade-offs; they do not collapse to "prepared better" without evidence components.
- Evidence traceability: every emitted item must point to replay-derived source records and configuration used.

Current implementation satisfies determinism in a synthetic sense but does not yet expose source evidence references or derivation categories in the output records.

## Failure and degradation behaviour

Required behaviour:

- Null analysis returns an empty report, not an exception. Current implementation does this (`InformationEngine.java:44`).
- Empty or missing timeline returns empty episodes and unknown states.
- Missing coordinates prevent Potentially Observed facts or lower confidence; they must not be reconstructed.
- Missing owner/player identity prevents attribution-sensitive items.
- Contradictory ownership must be recorded as a gap or low-confidence condition, not silently assigned.
- Missing visibility evidence prevents a `KNOWN` claim and should normally produce `POTENTIALLY_KNOWN`, `UNKNOWN` or an explicit "visibility unavailable" limitation.
- Missing engagement boundaries prevent high-confidence preparation intervals; the engine may emit lower-confidence timeline windows only when the fallback rule is explicit.
- Missing production/tech/static-defence events prevent category-specific preparation conclusions even when sampled macro stats exist.
- Unknown units/upgrades should be emitted as raw canonical-safe identifiers only if display-name policy allows it; otherwise keep raw data internal.
- Replay without scouting must produce no reaction candidates. Current test covers this (`InformationEngineTest.java:108`).

## Current implementation findings

### Support-bundle/report behaviour at `0.7.0`, commit `b30d8ce4d450`

Observed product gap:

- User-facing report/support bundle does not expose scouting, player-perspective information state or between-fight preparation comparison.
- The web support bundle contains `report.md`, `transcript.md`, `analysis-response.json` and `metadata.json` only. Evidence: `frontend/src/main.jsx:76` through `frontend/src/main.jsx:82`.
- Markdown export includes summary, match narrative, key fights and next-game actions; it does not render information state or preparation intervals. Evidence: `frontend/src/main.jsx:41` through `frontend/src/main.jsx:71`.

Cause analysis:

- Not primarily decoder absence. Current `ReplayAnalysis` already has enough partial data for some information/preparation facts: per-player sampled workers, minerals/gas bank, income rates, supply, army value/losses (`ReplayAnalysis.java:55` through `ReplayAnalysis.java:69`), timeline units/upgrades/abilities/positions (`ReplayAnalysis.java:72` through `ReplayAnalysis.java:85`), and domain mapping into `PlayerState` (`ReplayDomainMapper.java:36` through `ReplayDomainMapper.java:56`).
- Decoder data is still incomplete for strong information claims. The current domain input lacks full visibility evidence, stable unit tags/event IDs, exact production queue semantics, exact resource spend per item, complete map/base geometry and source-event provenance. Therefore the decoder is a contributing limitation, especially for `KNOWN`, staleness and exact scout identity.
- The immediate product cause is missing integration. `AnalysisEngineConfiguration` creates beans for `CoachFeedEngine`, `EpisodeEngine`, `ArgumentDeltaEngine`, narrative engines and `CombatEngine`, but no `InformationEngine` bean (`AnalysisEngineConfiguration.java:22` through `AnalysisEngineConfiguration.java:34`). `AnalysisService` builds match context, turning points, decisions, recommendations, coach feed, combats, episodes, argument deltas and narrative, but never calls Information Engine (`AnalysisService.java:88` through `AnalysisService.java:130`). `AnalysisResponse` has no information/report field (`AnalysisResponse.java:12` through `AnalysisResponse.java:24`).
- Coach Feed cannot render Information Engine output because its build API accepts match, context, turning points, decisions and recommendations only (`CoachFeedEngine.java:19` through `CoachFeedEngine.java:25`).
- The strategic-preparation gap is not just integration. Current `MatchContextEngine` and `ArgumentDeltaEngine` aggregate economy/army/supply/overall changes (`MatchContextEngine.java:45` through `MatchContextEngine.java:58`; `ArgumentDeltaEngine.java:24` through `ArgumentDeltaEngine.java:31`), while `DecisionEngine` detects attack/rebuild/expand/tech-switch heuristics (`DecisionEngine.java:17` through `DecisionEngine.java:21`; `EconomyDecisionDetector.java:32`; `ArmyDecisionDetector.java:30`). There is no domain model for between-engagement preparation intervals, production capacity, static defence, tech/upgrades, expansions, scouting coverage, bank/spend conversion, allied synchronization or readiness against a later power spike.

Conclusion:

The missing 0.7.0 report sections are caused by a combination of factors:

1. Information Engine domain contracts exist but are not wired into portal, REST, Coach Feed, Markdown or support-bundle output.
2. Decoder/domain input is partially sufficient for low/medium-confidence scouting and preparation facts, but insufficient for exact visibility or causal claims.
3. Strategic preparation between fights lacks a domain model entirely; existing context/delta/decision code is too coarse and fight-centred to answer that product question.

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

#### Major: no acquisition/staleness lifecycle for player-perspective knowledge

- Evidence: current observations and state entries carry observation time but no acquired-at/stale-after lifecycle and no invalidation rule (`InformationObservation.java:5`, `InformationState.java:10`). State construction collapses all observations for a scout player into one current bucket (`InformationEngine.java:251` through `InformationEngine.java:281`).
- Consequence: downstream reports cannot answer when information became available, whether it was still relevant before the next fight, or whether a player was acting on stale scouting.
- Recommended correction: add `acquiredAt`, `staleAfter`, `lastConfirmedAt`, `stalenessBasis` and target scope to information-state records.
- Ownership: required before connecting information to preparation or later engagements.

#### Major: no explicit omniscient-vs-perspective boundary

- Evidence: `potentiallyObserved` filters nearby opponent events and emits observations, but the output record does not link the observation to an omniscient replay fact or mark forbidden conclusions caused by absent visibility evidence (`InformationEngine.java:130` through `InformationEngine.java:160`; `InformationObservation.java:5`).
- Consequence: future consumers can accidentally treat replay facts as player knowledge or write "saw" language.
- Recommended correction: introduce separate `ReplayFact` and player-perspective `InformationObservation`/`InformationState` records, with derivation category and visibility limitations.
- Ownership: required before public report wording.

#### Major: strategic preparation between engagements is absent as a domain contract

- Evidence: existing deltas cover only economy, army, supply and overall score (`ArgumentDeltaEngine.java:24` through `ArgumentDeltaEngine.java:31`), while current decisions are limited to attack/rebuild/expand/tech-switch detectors (`DecisionEngine.java:17` through `DecisionEngine.java:21`). No `PreparationInterval`, `PreparationProfile` or team synchronization model exists.
- Consequence: 0.7.0 reports cannot explain whether a player invested in workers, immediate army, production capacity, tech/upgrades, expansions, static defence, scouting, resource-bank conversion or ally timing between fights.
- Recommended correction: add a between-engagement preparation model that consumes context snapshots, timeline facts, information state and engagement boundaries, and emits category-level trade-offs without a single winner score.
- Ownership: new APPLY change or same Information Engine APPLY if accepted as part of its scope.

#### Major: support-bundle output cannot carry reviewable information/preparation artifacts

- Evidence: web bundle generation includes only `report.md`, `transcript.md`, `analysis-response.json` and `metadata.json` (`frontend/src/main.jsx:76` through `frontend/src/main.jsx:82`). `AnalysisResponse` has no information or preparation field (`AnalysisResponse.java:12` through `AnalysisResponse.java:24`).
- Consequence: even if domain analysis exists, reviewers cannot inspect it through the user-facing support bundle unless raw JSON happens to include it.
- Recommended correction: add versioned optional information/preparation sections to REST response and support bundle, with source evidence and compatibility defaults.
- Ownership: public integration PR after domain contracts are strengthened.

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
- Should between-engagement preparation live inside Information Engine, as a sibling `PreparationEngine`, or as a downstream consumer of Information Engine plus Combat Detector V3 boundaries?
- What default staleness windows are acceptable for scouting facts, and should they differ by topic (army, tech, economy, expansions, upgrades)?
- How should team-shared information be represented when one ally scouts and another reacts?
- Which categories require decoder enhancements before they can leave `UNKNOWN` or low-confidence states?

## Alternatives considered

### 1. Information Engine as a distinct domain service

Accepted direction. It matches ADR-011 and keeps scouting/information reasoning out of Combat Engine. It also provides a coherent place for confidence, gaps and response candidates.

### 2. Information extraction distributed across existing engines

Rejected. Placing scouting logic in Decision Engine, Combat Engine, Knowledge Engine and narrative code would duplicate thresholds and encourage causal claims. The current existence of both `domain/scouting` and `domain/information` is already a warning sign.

### 3. Transcript-first normalized information model

Deferred but recommended. A transcript-first model with unit identity, event IDs and provenance would solve several evidence and identity issues. It should not block the current domain review, but should be the preferred path before strong real-replay claims.

### 4. Presentation-oriented aggregation outside the domain core

Rejected for core inference, accepted for rendering. The frontend/Markdown layer may render Information Engine output, but must not decide what was potentially observed or which action is a response candidate.

### 5. Strategic preparation as part of Combat Engine

Rejected. Preparation intervals may be anchored by combat boundaries, but preparation is not combat outcome. Putting it in Combat Engine would bias the model toward winner/trade efficiency and make scouting/resource-allocation context harder to reuse.

### 6. Strategic preparation as a sibling domain engine

Deferred and likely acceptable. A `PreparationEngine` could consume `InformationReport`, `MatchContext`, decisions and combat boundaries, then emit `StrategicPreparationInterval`. If adopted, Information Engine should still own player-perspective knowledge state and visibility constraints, while Preparation Engine owns allocation comparison.

## Test strategy

Required tests before APPLY/public integration:

- Unit tests for scout-unit registry normalization and custom config.
- Unit tests for null/empty/missing coordinate/missing owner inputs.
- Determinism test for stable ordering and identities.
- Evidence-reference tests asserting every observation, gap and reaction carries source evidence.
- Ambiguous multi-scout tests for same owner and same unit type.
- Team-game tests with two opponents and target-team scoped Information State.
- Tests for acquisition time, stale-after time and stale information before a later engagement.
- Tests that omniscient replay facts outside player/team perspective do not enter Information State.
- Between-engagement interval tests for economy investment, immediate army investment, production-capacity growth, tech/upgrades, expansions, static defence, bank/spend conversion and scouting activity.
- Team synchronization tests where allies prepare together versus asynchronously.
- Power-spike readiness tests that compare current army/static defence against a later known upgrade/tech timing without claiming the player knew that later fact.
- Negative tests: far-away enemy events, no scouting, missing coordinates, and action after response window must not produce response candidates.
- Contract tests for REST serialization once API output is added.
- Real-replay validation using saved private replays outside git, with artifacts recording decoder schema and Information Engine output.

## Migration and compatibility

Current implementation has no REST, Markdown, support-bundle or frontend impact because it is not wired into `AnalysisResponse` or `AnalysisService`.

Future integration will require:

- a backward-compatible optional field in `AnalysisResponse`, or a versioned API response;
- support-bundle inclusion of Information Engine output and source evidence;
- support-bundle inclusion of `StrategicPreparationInterval` artifacts once implemented;
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
- define acquisition and staleness lifecycle for player-perspective knowledge;
- define omniscient replay-fact versus player-perspective contracts;
- decide whether between-engagement preparation is owned by Information Engine or a sibling Preparation Engine;
- specify `StrategicPreparationInterval`/`PreparationProfile`/`PreparationComparison`;
- decide whether domain narrative emits structured facts or rendered prose;
- add missing-data, determinism, ambiguity and real-replay validation tasks.
