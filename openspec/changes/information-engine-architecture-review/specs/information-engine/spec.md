# Information Engine Capability Specification

## Status

Reviewed during `REVIEW` gate for `information-engine-architecture-review`.

This specification defines the target capability contract. It does not authorize production code changes by itself.

## Purpose

The Information Engine shall transform replay-derived domain inputs into structured, explainable information-state outputs without claiming complete player vision or causal intent.

## Requirements

### Requirement: Independent domain responsibility

The Information Engine SHALL be owned by `java/coach-domain` and SHALL remain independent from Combat Engine, Combat Detector V3 and Combat Narrative.

It SHALL answer information-state questions: what a player potentially could know, what remained missing, which later actions are response candidates, and how confident those conclusions are.

It SHALL NOT own replay decoding, combat clustering, loss attribution, winner selection, recommendation generation, HTTP orchestration or frontend rendering.

#### Scenario: Combat context consumes information

- GIVEN a future Combat Engine or Knowledge Engine integration;
- WHEN combat or recommendation logic needs scouting context;
- THEN it MAY consume `InformationReport` output;
- AND Information Engine SHALL NOT depend on combat output to produce information.

#### Scenario: Adjacent responsibility is proposed

- GIVEN a proposed Information Engine operation already belongs to decoder, context, decision, turning-point, combat, knowledge, narrative, portal or frontend layers;
- WHEN the architecture is reviewed;
- THEN the operation is rejected from Information Engine scope unless an ADR explicitly changes ownership.

### Requirement: Canonical input contract

The Information Engine SHALL accept replay-derived domain input, currently `ReplayAnalysis`, plus a versioned configuration.

`ReplayAnalysis.timeline` and `ReplayAnalysis.players` are canonical current inputs. Coordinates, owners, teams, timestamps, units, upgrades and attributes are optional unless a specific rule requires them.

#### Scenario: Input is null or empty

- GIVEN null analysis or an analysis with no usable timeline;
- WHEN the engine processes it;
- THEN it emits an empty report or unknown states;
- AND it does not throw for ordinary missing replay data.

#### Scenario: Optional coordinates are absent

- GIVEN a timeline event without `position` or `target_position`;
- WHEN a potential observation requires geometry;
- THEN the engine SHALL omit that observation or emit an explicitly partial result with reduced confidence;
- AND it SHALL NOT reconstruct coordinates.

### Requirement: Versioned configuration

Scout-unit registry, contact thresholds, potential-vision radius, response-window duration, output limits and confidence weights SHALL be represented as versioned configuration.

#### Scenario: Default heuristics are used

- GIVEN the engine uses default scout units or thresholds;
- WHEN it emits a report;
- THEN the output or diagnostics identify the configuration version sufficient to reproduce the result.

#### Scenario: Scout-unit list is extended

- GIVEN a new scout unit is added;
- WHEN the engine runs with the updated config;
- THEN the new unit is recognized without changing unrelated engine logic.

### Requirement: Episode contract

Each `InformationEpisode` SHALL include:

- stable episode id;
- scout player;
- target player and/or target team when known;
- scout unit and unit identity when available;
- start and end;
- survived;
- confidence;
- source evidence references;
- potentially observed items;
- missing information;
- response candidates.

#### Scenario: Scout enters opponent area

- GIVEN a scout unit has replay-derived coordinates near an opponent area or opponent informative event;
- WHEN contact starts;
- THEN the engine emits an episode whose start is the first qualifying contact timestamp.

#### Scenario: Scout dies

- GIVEN a scout death can be attributed to the scout owner and unit identity or best available fallback;
- WHEN the death bounds the scouting contact;
- THEN the episode ends at the death timestamp;
- AND `survived` is false;
- AND confidence reflects death-bounded evidence and any identity ambiguity.

#### Scenario: Scout leaves or contact ends

- GIVEN no qualifying scout contact continues within the configured contact gap;
- WHEN no matching death bounds the episode;
- THEN the episode ends at the last contact plus configured expiry or an equivalent documented contact-end rule;
- AND `survived` is true only if no scout death evidence was found.

### Requirement: Potentially Observed contract

The engine SHALL use the term `Potentially Observed` for information that could have been visible from replay-derived geometry.

It SHALL NOT write or expose "Observed" as confirmed player vision unless a future decoder input provides exact vision evidence.

Each `InformationObservation` SHALL include:

- stable observation id;
- type/category;
- subject;
- target owner/team;
- time;
- coordinates when available;
- distance or spatial relation when relevant;
- derivation category;
- confidence;
- source evidence references.

#### Scenario: Technology is near scout path

- GIVEN a scout episode and a nearby opponent technology event such as Roach Warren or Robotics Facility;
- WHEN the event is inside the configured potential-vision geometry;
- THEN the engine emits a `Potentially Observed` technology observation;
- AND it records the source event and scout-position evidence.

#### Scenario: Enemy event is far away

- GIVEN an opponent event outside the configured potential-vision geometry;
- WHEN the engine evaluates potential observations;
- THEN it SHALL NOT emit it as potentially observed.

### Requirement: Missing Information contract

The engine SHALL emit `Missing Information` when scouting ended early, lacked necessary coverage, had missing coordinates, or did not sample required categories.

Each `InformationGap` SHALL include topic, target scope, reason, confidence and source evidence or missing-evidence references.

#### Scenario: Scout dies early

- GIVEN a scouting episode ends before the configured short-scout threshold;
- WHEN expected categories were not sampled;
- THEN the engine emits gaps such as Main Tech, Army Composition, Second Gas, Third Base or Tech Structure as applicable;
- AND the episode confidence is reduced.

#### Scenario: Missing coordinates prevent coverage

- GIVEN scout or target events lack coordinates;
- WHEN the engine cannot establish spatial coverage;
- THEN it emits missing-information or reduced-confidence evidence instead of inferring coverage.

### Requirement: Response Candidate contract

The engine SHALL use `Response Candidate` for later player actions that are temporally and optionally semantically compatible with scouting information.

It SHALL NOT claim "decided because", "built because", or equivalent causal language.

Each `InformationReaction` SHALL include:

- stable reaction id;
- acting player;
- action type and subject;
- time;
- delay from episode end;
- candidate basis;
- related observation IDs when any;
- derivation category;
- confidence;
- source action evidence references.

#### Scenario: Potentially Observed Roach Warren then Bunker

- GIVEN an episode with `Potentially Observed` Roach Warren;
- AND the scout player starts or completes Bunker within the configured response window;
- WHEN response candidates are evaluated;
- THEN the engine emits `Response Candidate: Build Bunker`;
- AND confidence reflects semantic match plus timing;
- AND no causal claim is emitted.

#### Scenario: No scouting occurred

- GIVEN a replay with later defensive buildings but no scouting episode;
- WHEN response candidates are evaluated;
- THEN no Information Engine response candidates are emitted.

#### Scenario: Action occurs outside response window

- GIVEN an action occurs after the configured response window;
- WHEN response candidates are evaluated;
- THEN it SHALL NOT be emitted for that episode.

### Requirement: Information State and Advantage

The engine SHALL produce explainable `InformationState` entries for each relevant player and target scope.

Knowledge states SHALL include `UNKNOWN` and `POTENTIALLY_KNOWN`. `KNOWN` SHALL only be emitted when a rule defines direct evidence sufficient for confirmed knowledge; otherwise it SHALL remain unused or be removed from the public contract.

Information Advantage SHALL NOT be a single numeric score.

#### Scenario: Player potentially knows opponent tech

- GIVEN a player has a technology observation scoped to an opponent or opponent team;
- WHEN Information State is built;
- THEN that player's state for that target marks army tech as `POTENTIALLY_KNOWN`;
- AND includes the observation evidence.

#### Scenario: Team game with multiple opponents

- GIVEN a team replay with more than one opponent;
- WHEN Information State is built;
- THEN target player/team scope is preserved;
- AND observations from different opponents are not merged into an unscoped bucket.

### Requirement: Evidence traceability

Every emitted episode, observation, gap and response candidate SHALL reference sufficient replay-derived or deterministic evidence to explain how it was produced.

Evidence references SHALL include at least replay timestamp, event type, player or team identity, subject and derivation category. When stable decoder event IDs or unit tags become available, they SHALL be used.

#### Scenario: Evidence cannot be identified

- GIVEN an output candidate whose supporting event or derivation cannot be traced;
- WHEN the engine evaluates it;
- THEN the engine SHALL either omit it or mark it partial with reduced confidence and missing evidence.

### Requirement: Deterministic behaviour

For identical versioned input and identical configuration, the Information Engine SHALL produce semantically identical output.

#### Scenario: Analysis is repeated

- GIVEN the same replay analysis input, engine version and configuration;
- WHEN processing is repeated;
- THEN information identities, ordering, values, confidence and evidence references are semantically identical.

### Requirement: Fact and inference separation

The Information Engine SHALL distinguish:

- direct replay facts;
- deterministic derivations;
- configurable heuristics;
- presentation-only transformations.

#### Scenario: A heuristic threshold is used

- GIVEN an information item depends on a radius, time window, contact gap or semantic match table;
- WHEN the item is emitted;
- THEN its derivation category and confidence reflect heuristic status rather than direct fact status.

### Requirement: Contract compatibility

Any change to serialized replay analysis, REST, Markdown, frontend or support-bundle contracts caused by Information Engine integration SHALL be explicitly versioned or demonstrated backward-compatible.

#### Scenario: REST field is added

- GIVEN `AnalysisResponse` adds an information field;
- WHEN existing clients omit or ignore that field;
- THEN the response remains backward-compatible or the API version/migration is documented.

### Requirement: Real-replay validation

Before public report integration, the engine SHALL be validated on real replay artifacts outside git, with output artifacts or summaries that can be reviewed without committing private replays.

#### Scenario: Private replay corpus is used

- GIVEN a private `.SC2Replay` sample;
- WHEN Information Engine validation runs;
- THEN the replay is not committed to git;
- AND the validation records decoder schema, Information Engine output and deviations.
