# Information Engine Capability Specification

## Status

Draft for architecture review. Complete during the REVIEW gate; placeholders are not accepted requirements.

## Purpose

The Information Engine shall transform repository-defined replay and analysis inputs into structured, explainable information about what happened, what players could observe, and how they prepared over time. It shall support downstream deterministic analysis and presentation without inventing facts, player knowledge or intent.

## Requirements

### Requirement: Explicit responsibility boundary

The Information Engine SHALL have a documented responsibility that does not duplicate replay decoding, combat detection, final combat outcome evaluation, turning-point detection, recommendation generation or presentation rendering.

#### Scenario: Combat efficiency is proposed as the whole capability

- GIVEN an implementation describes Information Engine primarily as winner, loss or combat-efficiency scoring;
- WHEN the architecture is reviewed;
- THEN the design is rejected or corrected because information acquisition and strategic preparation are required independent capabilities.

### Requirement: Omniscient fact and player knowledge separation

The Information Engine SHALL distinguish actual replay state from player-perspective observations and derived knowledge state.

#### Scenario: Enemy technology exists but was not observed

- GIVEN the replay proves that an enemy technology structure existed;
- AND no supported observation proves the focus player or team saw it;
- WHEN player-perspective information is produced;
- THEN the technology is not presented as known to that player.

#### Scenario: Visibility evidence is unavailable

- GIVEN the decoder does not expose enough data to prove that an entity was visible;
- WHEN scouting analysis is produced;
- THEN the engine records the limitation and does not promote proximity, camera position or omniscient existence to confirmed observation.

### Requirement: Structured observation evidence

Every confirmed observation SHALL identify observer player or team, observed subject, timestamp or interval, evidence type, provenance and confidence.

#### Scenario: Scan reveals enemy assets

- GIVEN supported replay evidence identifies a scan and entities revealed within its scope;
- WHEN observations are built;
- THEN the output attributes those observations to the scanning player or team with evidence references.

#### Scenario: Sacrificial scout enters enemy territory

- GIVEN supported movement, position and visibility evidence proves a scouting unit observed enemy assets before dying;
- WHEN observations are built;
- THEN the observed assets and scout event are represented separately from the scout's later combat loss.

### Requirement: Knowledge staleness

Knowledge derived from an observation SHALL preserve observation time and staleness rather than remaining permanently current.

#### Scenario: Enemy composition changes after scouting

- GIVEN a player observed an enemy composition at time T1;
- AND the omniscient replay state changed later without a new supported observation;
- WHEN knowledge is queried at T2;
- THEN the previous information is marked stale or last-observed and the new composition is not treated as known.

### Requirement: Strategic intervals

The Information Engine SHALL represent bounded intervals suitable for comparing preparation choices, including intervals between engagements and intervals where no engagement occurs.

#### Scenario: Two consecutive engagements exist

- GIVEN reliable boundaries for engagement A and engagement B;
- WHEN preparation analysis is performed;
- THEN an interval between those boundaries is produced with explicit start/end semantics.

#### Scenario: No clean engagement is detected

- GIVEN meaningful macro and information changes occur without a reliable combat episode;
- WHEN preparation analysis is performed;
- THEN the capability can use another documented boundary strategy or explicitly report that no comparable interval was formed.

### Requirement: Explainable preparation allocation

For each supported player and team, interval changes SHALL be classified into explainable preparation categories.

At minimum the design SHALL evaluate economy, immediate army, production capacity, technology/upgrades, expansion, static defence, information acquisition and resource banking/spending.

#### Scenario: One player makes workers while another makes army

- GIVEN player A increases worker/economic capacity;
- AND player B increases immediately available combat forces in the same interval;
- WHEN preparation is compared;
- THEN the output describes the different investment horizons without declaring either choice universally superior.

#### Scenario: Production is added before units appear

- GIVEN a player adds production capacity but the next army snapshot has not yet materially grown;
- WHEN interval preparation is summarized;
- THEN production investment is preserved as a distinct category rather than treated as no preparation.

#### Scenario: Technology will mature after the next fight

- GIVEN a technology or upgrade starts during the interval but completes after the next engagement begins;
- WHEN readiness is compared;
- THEN the output distinguishes future investment from capability available at fight start.

### Requirement: Team preparation comparison

Team-game output SHALL preserve individual choices while also supporting team-level synchronization analysis.

#### Scenario: Allies prepare on different horizons

- GIVEN one ally invests in immediate pressure and another invests in workers or technology;
- WHEN team preparation is analysed;
- THEN both individual allocations and their timing relationship are available downstream.

#### Scenario: Team attacks before an ally's power spike

- GIVEN an engagement starts before a teammate's documented upgrade, production cycle or army reinforcement completes;
- WHEN synchronization is assessed;
- THEN the timing mismatch may be emitted as a deterministic timing fact or heuristic hypothesis with explicit evidence and confidence.

### Requirement: No unsupported intent claims

The Information Engine SHALL NOT present inferred intent as fact.

#### Scenario: Player builds workers after a fight

- GIVEN worker production increases after an engagement;
- WHEN strategic posture is inferred;
- THEN the engine may describe economic investment or a likely delayed power horizon, but it does not claim the player's private plan.

### Requirement: Evidence traceability

Every emitted information item SHALL reference sufficient replay-derived or deterministic source evidence to explain how it was produced.

#### Scenario: Information is emitted from complete evidence

- GIVEN valid source records required by an information rule;
- WHEN the rule emits an information item;
- THEN output identifies relevant timestamps, players/teams, source records and derivation category.

#### Scenario: Evidence cannot be identified

- GIVEN an output candidate whose support cannot be traced;
- WHEN evaluated;
- THEN it is not presented as a confirmed fact.

### Requirement: Deterministic behaviour

For identical versioned input and configuration, output SHALL be semantically identical.

#### Scenario: Analysis is repeated

- GIVEN the same input, engine version and configuration;
- WHEN processing is repeated;
- THEN identities, interval boundaries, ordering, values, confidence and evidence are semantically identical.

### Requirement: Fact and inference separation

The output SHALL distinguish direct facts, deterministic derivations and heuristic hypotheses.

#### Scenario: Strategic posture uses thresholds

- GIVEN posture depends on configurable thresholds or incomplete behavioural interpretation;
- WHEN emitted;
- THEN category and confidence identify it as heuristic rather than direct fact.

### Requirement: Explicit missing-data degradation

The engine SHALL NOT silently reconstruct missing coordinates, visibility, ownership, timestamps, resources, values or intent.

#### Scenario: Required scouting data is missing

- GIVEN source data required to confirm an observation is absent;
- WHEN processing occurs;
- THEN the engine omits the confirmed observation or emits an explicitly partial result with reduced confidence and missing-evidence details.

#### Scenario: Production lifecycle is incomplete

- GIVEN a structure or unit start/completion event is missing;
- WHEN preparation deltas are calculated;
- THEN the affected category is marked partial and does not fabricate completion timing.

### Requirement: Stable attribution

Information concerning players, teams, entities, upgrades or losses SHALL use repository-defined identity and ownership semantics.

#### Scenario: Victim and killer differ

- GIVEN a death event with different victim and killer owners;
- WHEN loss-related information is produced;
- THEN loss is attributed to the victim owner and killer identity is only participation evidence, consistent with ADR-006.

### Requirement: Explainable aggregates

The engine SHALL NOT expose an opaque universal score as evidence unless all components and calculation are available.

#### Scenario: Preparation comparison uses a composite

- GIVEN multiple preparation dimensions are summarized;
- WHEN a composite is exposed;
- THEN components, units, normalization and versioned calculation are available, and the raw dimensions remain accessible.

### Requirement: Combat independence

Information and preparation analysis SHALL remain useful when combat outcome is unknown, ambiguous or intentionally not evaluated.

#### Scenario: Fight outcome is not determined

- GIVEN an engagement is detected but no winner is assigned;
- WHEN interval strategy is rendered;
- THEN scouting, preparation allocation, readiness timing and player/team comparisons remain available.

### Requirement: Contract compatibility

Any serialized replay-analysis, REST or report change SHALL be explicitly versioned or demonstrated backward compatible.

#### Scenario: New information-state contract is added

- GIVEN observations, knowledge states or preparation intervals are added to an existing response;
- WHEN the design is reviewed;
- THEN migration, optionality, defaults and support-bundle compatibility are documented before APPLY.

## Open specification items

The REVIEW must replace this section with repository-grounded decisions for:

- canonical input and output types;
- observation proof rules for each available replay event type;
- team information-sharing semantics;
- knowledge expiry/staleness policy;
- interval boundary algorithm;
- investment-category accounting and overlap rules;
- item identity and ordering;
- confidence/evidence representation;
- owning module/package and downstream consumers;
- failure policy;
- required tests and real-replay corpus.