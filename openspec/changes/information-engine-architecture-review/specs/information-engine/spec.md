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

It SHALL support, directly or through a sibling preparation capability, between-engagement strategic preparation analysis grounded in player-perspective information state.

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

### Requirement: Replay facts are separate from player perspective

The Information Engine SHALL distinguish omniscient replay facts from player- or team-perspective information state.

An omniscient fact is something that existed in the replay. A player-perspective item is something a player or team potentially could have known based on scouting/contact evidence.

#### Scenario: Enemy tech exists but was not scouted

- GIVEN a replay contains an enemy technology structure;
- AND no scout/contact/visibility evidence links that structure to the player perspective;
- WHEN Information State is built;
- THEN the structure SHALL NOT be emitted as `POTENTIALLY_KNOWN` for that player;
- AND the relevant topic remains `UNKNOWN` or a `Missing Information` gap.

#### Scenario: Visibility evidence is unavailable

- GIVEN replay data contains low-level unit and structure events;
- AND the decoder does not provide a complete vision log;
- WHEN the engine emits player-perspective information;
- THEN it uses `Potentially Observed`;
- AND it records that exact visibility is unavailable.

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

Every player-perspective state SHALL include acquisition timing and staleness semantics when it represents potentially known information.

#### Scenario: Player potentially knows opponent tech

- GIVEN a player has a technology observation scoped to an opponent or opponent team;
- WHEN Information State is built;
- THEN that player's state for that target marks army tech as `POTENTIALLY_KNOWN`;
- AND includes the observation evidence.

#### Scenario: Information becomes stale

- GIVEN a player potentially observed an opponent army composition early;
- AND no later contact refreshed that topic before the next engagement;
- WHEN the report explains pre-fight information state;
- THEN the topic is marked stale or uncertain after the configured staleness window;
- AND the report SHALL NOT imply current knowledge.

#### Scenario: Team game with multiple opponents

- GIVEN a team replay with more than one opponent;
- WHEN Information State is built;
- THEN target player/team scope is preserved;
- AND observations from different opponents are not merged into an unscoped bucket.

#### Scenario: Ally scouts for the team

- GIVEN one ally potentially observes opponent tech;
- WHEN team-shared information is enabled by a documented rule;
- THEN teammates may receive a team-scoped `POTENTIALLY_KNOWN` state;
- AND the output records which ally provided the evidence;
- AND confidence reflects that team sharing is a gameplay/team-inference rule, not direct individual vision.

### Requirement: Strategic preparation intervals

The capability SHALL model preparation between engagements as interval-scoped strategic allocation, not as combat winner selection.

Each `StrategicPreparationInterval` SHALL include:

- stable interval id;
- start and end;
- previous engagement id when known;
- next engagement id when known;
- players and teams;
- information state at interval start/end;
- per-player and per-team preparation profiles;
- confidence and missing-data markers.

The interval MAY be owned by Information Engine or by a sibling Preparation Engine, but it SHALL consume Information Engine output rather than reverse the dependency.

#### Scenario: Interval between fights is available

- GIVEN two engagement boundaries are available;
- WHEN preparation analysis runs;
- THEN it emits one interval ending at the later engagement start;
- AND it does not use the later engagement winner to classify earlier preparation choices.

#### Scenario: Engagement boundary is missing

- GIVEN combat boundaries are unavailable or low confidence;
- WHEN preparation analysis runs;
- THEN it either omits the interval or emits a lower-confidence fallback window with the fallback basis recorded.

### Requirement: Preparation profile categories

For each player/team interval, the capability SHALL classify measurable preparation categories separately:

- workers and future economy;
- resource-bank accumulation and spending;
- immediate army value and composition;
- production capacity;
- technology structures;
- upgrades;
- expansions;
- static defence;
- scouting and information acquisition;
- allied synchronization.

Each category SHALL include direct deltas or source facts, inferred classification, confidence and evidence. No single opaque preparation score is allowed.

#### Scenario: Future economy versus immediate army

- GIVEN player A adds workers or expands while player B adds army value in the same interval;
- WHEN preparation is compared;
- THEN the output explains the trade-off by category;
- AND it SHALL NOT simply declare that one player prepared better.

#### Scenario: Resource bank is not converted

- GIVEN a player accumulates a large mineral/gas bank while army and production capacity do not grow;
- WHEN preparation profile is built;
- THEN the profile records bank accumulation and low conversion evidence;
- AND confidence depends on available resource-bank and production evidence.

#### Scenario: Static defence responds to possible threat

- GIVEN a player potentially observed air tech;
- AND later builds static defence within the interval;
- WHEN response candidates are evaluated;
- THEN the static defence action may be linked as a `Response Candidate`;
- AND the output SHALL NOT claim the player built it because of the scouting.

#### Scenario: Allied timing differs

- GIVEN a team game where allies start army/upgrade/production preparation at materially different times;
- WHEN team synchronization is evaluated;
- THEN the interval records asynchronous preparation;
- AND it preserves each player contribution rather than flattening the team into one actor.

### Requirement: Readiness against next engagement or later power spike

The capability SHALL be able to describe readiness at the next engagement start and against a later known power spike without using future facts as player knowledge.

#### Scenario: Ready for immediate fight but behind later upgrade timing

- GIVEN a player has higher immediate army value before the next fight;
- AND the opponent has a later upgrade or technology power spike in the replay;
- WHEN preparation comparison is emitted;
- THEN it may say the player was prepared for the immediate fight but risks falling behind the later power spike;
- AND it SHALL distinguish actual replay facts from what the player potentially knew at that time.

#### Scenario: Later fact was not knowable

- GIVEN a later power spike exists in the replay;
- AND no player-perspective evidence made it potentially knowable before the interval ended;
- WHEN narrative or Coach Feed consumes the comparison;
- THEN it SHALL NOT say the player ignored or failed to react to that power spike as a fact.

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

#### Scenario: Support bundle carries information artifacts

- GIVEN a support bundle is generated after Information Engine integration;
- WHEN the bundle is inspected;
- THEN it includes versioned information/preparation artifacts or embeds them in `analysis-response.json`;
- AND older bundles without those artifacts remain readable with explicit "not available in this version" behaviour.

#### Scenario: Version 0.7.0 bundle is reviewed

- GIVEN a support bundle from application version `0.7.0`, commit `b30d8ce4d450`;
- WHEN scouting and preparation sections are absent;
- THEN the review records this as a combination of missing integration and incomplete domain/preparation contracts;
- AND it SHALL NOT conclude that the decoder alone lacked all necessary data.

### Requirement: Real-replay validation

Before public report integration, the engine SHALL be validated on real replay artifacts outside git, with output artifacts or summaries that can be reviewed without committing private replays.

#### Scenario: Private replay corpus is used

- GIVEN a private `.SC2Replay` sample;
- WHEN Information Engine validation runs;
- THEN the replay is not committed to git;
- AND the validation records decoder schema, Information Engine output and deviations.
