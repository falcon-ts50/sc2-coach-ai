# Narrative Analysis Capability

## Requirements

### Requirement: Deterministic narrative output

The system SHALL produce the same ordered narrative phases, state transitions, causal links, summary and chart model for the same replay analysis, selected player and configuration.

#### Scenario: Repeated analysis

- GIVEN identical replay-derived input and selected player
- WHEN Narrative Analysis runs twice
- THEN all narrative IDs, ordering, phase boundaries, transition values and causal-link types SHALL be identical.

### Requirement: Existing engines remain authoritative

Narrative Analysis SHALL consume normalized references to existing engine outputs and SHALL NOT reimplement combat detection, loss attribution, scouting detection, turning-point scoring or recommendation rules.

#### Scenario: Combat evidence reference

- GIVEN a combat episode produced by Combat Engine
- WHEN it contributes to a narrative phase
- THEN the phase SHALL reference the combat source and SHALL NOT recalculate victim ownership independently.

### Requirement: Explainable match states

The system SHALL build timestamped match-state snapshots using available army, economy proxy, supply and descriptive infrastructure/team context without an opaque global power score.

#### Scenario: Partial state

- GIVEN army and supply samples but missing economy data
- WHEN a state snapshot is built
- THEN the snapshot SHALL remain partial, SHALL expose missing economy data and SHALL reduce confidence rather than inventing a value.

### Requirement: Material state transitions

Every state transition SHALL expose before/after state references, relevant deltas, evidence, scope, confidence and limitations.

#### Scenario: Army decline

- GIVEN a material decrease in selected-player army value supported by combat/loss evidence
- WHEN transition detection runs
- THEN an army-decline transition SHALL include its time range and before/after values.

#### Scenario: Isolated event

- GIVEN a single command or isolated unit death without a material state change
- WHEN transition detection runs
- THEN the system SHALL NOT emit a major transition solely from that event.

### Requirement: Meaningful phase segmentation

Match phases SHALL be bounded by evidence-supported state changes and SHALL NOT be fixed-duration clock buckets.

#### Scenario: Stabilization boundary

- GIVEN an earlier decline followed by sustained recovery or stabilization
- WHEN phase segmentation runs
- THEN the system SHALL create a boundary only when the state timeline supports the change and SHALL expose the boundary reason.

#### Scenario: Insufficient evidence

- GIVEN a timeline with no defensible semantic boundary
- WHEN phase segmentation runs
- THEN the system SHALL retain a broader or `UNCLASSIFIED` phase instead of fabricating multiple phases.

### Requirement: Conservative causal links

V1 SHALL support typed links equivalent to `PRECEDED`, `CONTRIBUTED_TO`, `ENABLED` and `RECOVERED_FROM`.

Temporal proximity alone SHALL support only temporal precedence.

#### Scenario: Temporal order only

- GIVEN event A occurs before event B with no additional causal evidence
- WHEN links are assembled
- THEN the strongest permitted link SHALL be `PRECEDED`.

#### Scenario: Possible contribution

- GIVEN an earlier army loss, measurable reduced mobile combat strength and later pressure
- WHEN the evidence supports a possible relationship but not exclusive cause
- THEN the system MAY emit `CONTRIBUTED_TO` with heuristic derivation, non-high confidence and explicit limitations.

### Requirement: No unsupported intent or vision claims

The system SHALL distinguish facts, deterministic derivations and heuristics and SHALL preserve Information Engine uncertainty.

#### Scenario: Potential scouting information

- GIVEN Information Engine reports `Potentially Observed`
- WHEN the fact is used in Narrative Analysis
- THEN user-facing output SHALL NOT say the player definitely saw, knew or decided because of it.

### Requirement: Principal causal chain

The system SHALL select at most one principal chain ordered by time, evidence quality and relevance to major selected-player state changes.

#### Scenario: Defensible chain

- GIVEN at least two defensible causal links connecting major transitions
- WHEN the summary is built
- THEN the website SHALL display one ordered principal chain with evidence and confidence per link.

#### Scenario: No defensible chain

- GIVEN fewer than two defensible links
- WHEN the summary is built
- THEN the system SHALL state that a reliable principal chain could not be assembled.

### Requirement: Strategic result deferred

This change SHALL preserve the official replay result and SHALL expose strategic-result analysis as `NOT_EVALUATED`.

#### Scenario: Official win with adverse late state

- GIVEN the replay records an official win but the timeline contains late deterioration
- WHEN V1 narrative is rendered
- THEN the official win SHALL remain visible and the system SHALL NOT infer strategic victory or defeat.

### Requirement: Match-overview chart contract

The backend SHALL provide a chart model with normalized time points, series metadata, source completeness, phase intervals and event markers.

Default series SHALL include selected-player army value, workers/economy proxy and supply when available.

#### Scenario: Complete default series

- GIVEN all three default series are available
- WHEN the report renders
- THEN all three SHALL be visible by default on a shared time axis.

#### Scenario: Incomplete series

- GIVEN a series has missing samples
- WHEN it is returned and rendered
- THEN gaps or incompleteness SHALL remain explicit and SHALL NOT be silently interpolated unless the source contract explicitly allows it.

### Requirement: Chart overlays and synchronization

The chart SHALL show combat markers, narrative phase intervals/boundaries and available turning-point markers.

Selecting a phase SHALL highlight the matching chart interval.

#### Scenario: Phase selection

- GIVEN a rendered phase with a time interval
- WHEN the user selects or expands it
- THEN the corresponding chart interval SHALL be highlighted without recomputing the phase in the frontend.

### Requirement: Player and team identity

The narrative SHALL preserve selected-player, teammate, opponent and team identities.

#### Scenario: Benchmark 2v2

- GIVEN dragonDriver is selected and Lulu is on the same team
- WHEN narrative output is built
- THEN Lulu SHALL be identified as teammate context and SHALL NOT be treated as an opponent.

### Requirement: Additive API compatibility

Narrative payload fields SHALL be additive. Existing analysis response fields and report sections SHALL remain available.

#### Scenario: Existing client

- GIVEN a client ignores the new narrative payload
- WHEN it consumes the updated response
- THEN all previously available fields SHALL retain compatible meaning.

### Requirement: Stable ordering

Events, transitions, phases, markers and links SHALL use stable deterministic ordering by timestamp, explicit type order and stable ID.

#### Scenario: Equal timestamps

- GIVEN multiple items share a timestamp
- WHEN serialized
- THEN their order SHALL be determined by documented type order and stable ID, not collection iteration order.

### Requirement: Browser and Markdown parity

Browser and Markdown SHALL use the same domain narrative result.

#### Scenario: Download report

- GIVEN the browser displays a verdict, phases and principal chain
- WHEN Markdown is downloaded
- THEN it SHALL contain the same verdict, ordered phases, chain semantics, evidence, confidence, uncertainty and strategic-result status.

### Requirement: Benchmark vertical-slice behaviour

For the fixed benchmark support bundle, Narrative Analysis SHALL expose a coherent trajectory containing evidence-supported equivalents of early decline, defensive adaptation/stabilization, mid-game improvement and later deterioration.

#### Scenario: Benchmark report

- GIVEN the fixed benchmark support bundle and dragonDriver perspective
- WHEN the website renders
- THEN it SHALL show a chronological narrative and synchronized graph rather than only independent cards;
- AND it SHALL preserve the official win;
- AND it SHALL mark strategic result as not evaluated;
- AND it SHALL recognize recovery/improvement where supported;
- AND it SHALL show later deterioration without yet declaring strategic defeat.

### Requirement: Responsive graph

The chart SHALL remain interpretable on mobile through responsive layout, series selection, scrolling or equivalent interaction.

#### Scenario: Mobile viewport

- GIVEN a supported mobile viewport
- WHEN the narrative report is opened
- THEN metric labels, phase intervals and evidence access SHALL remain usable without requiring desktop width.
