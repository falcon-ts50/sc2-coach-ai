# Narrative Evidence Visualization Capability

## Requirements

### Requirement: All-participant metric comparison

The system SHALL provide participant-aware series for army value, economy proxy and occupied supply on a shared match-time domain.

#### Scenario: Benchmark 2v2 comparison

- GIVEN the fixed benchmark support bundle and `dragonDriver` perspective
- WHEN the report chart model is built
- THEN each primary metric SHALL include `dragonDriver`, teammate `Lulu` and both opponents when source data is available;
- AND the participant relationship and team identity SHALL be explicit for every series.

#### Scenario: Missing participant metric

- GIVEN one participant has incomplete economy samples
- WHEN the comparison chart is built
- THEN that participant SHALL remain represented with explicit completeness and missing-range metadata;
- AND missing values SHALL NOT be silently converted to zero.

### Requirement: Stable selected-player emphasis

The selected player SHALL be visually distinguishable from all other participants by non-colour cues on every primary graph.

#### Scenario: Selected player styling

- GIVEN `dragonDriver` is selected
- WHEN army, economy and supply graphs render
- THEN `dragonDriver` SHALL use the strongest solid stroke consistently;
- AND non-selected participants SHALL use subordinate dashed or dotted strokes;
- AND identity SHALL NOT depend on colour alone.

### Requirement: Stable cross-chart identity

A participant SHALL retain the same display name, team relationship, style key and legend identity across all graphs and combat evidence views.

#### Scenario: Participant across metrics

- GIVEN a player appears in all three primary metrics
- WHEN the report renders
- THEN the player's visual identity SHALL remain stable across army, economy and supply graphs.

### Requirement: Shared chart synchronization

All primary metric graphs SHALL share the same match-time domain, hover timestamp, selected interval and evidence focus.

#### Scenario: Shared hover

- GIVEN multiple metric graphs are visible
- WHEN the pointer hovers at a match timestamp on one graph
- THEN the same timestamp SHALL be indicated on every visible metric graph.

#### Scenario: Phase selection

- GIVEN a narrative phase has a canonical interval
- WHEN the user selects that phase
- THEN the interval SHALL be highlighted on all primary metric graphs without recalculating the phase in the frontend.

#### Scenario: Combat selection

- GIVEN a combat card references a canonical combat interval
- WHEN the card or its graph marker is selected
- THEN all graphs SHALL highlight the same interval;
- AND the corresponding marker/card SHALL receive linked focus.

### Requirement: Explicit graph markers

Combat, turning-point and narrative-event markers SHALL carry stable source IDs, timestamps or intervals and deterministic ordering.

#### Scenario: Equal timestamp markers

- GIVEN several markers share one timestamp
- WHEN serialized and rendered
- THEN ordering SHALL use documented marker type order and stable source ID rather than collection iteration order.

### Requirement: Team-aware combat force table

Every displayed combat SHALL provide an evidence table grouped by SC2 side/team and then participant.

#### Scenario: Team combat

- GIVEN a detected 2v2 combat
- WHEN the combat evidence is rendered
- THEN the selected player's team SHALL be grouped separately from the opposing team;
- AND each participant SHALL remain individually attributable;
- AND team totals SHALL NOT replace participant rows.

### Requirement: Per-unit combat evidence

Combat participant evidence SHALL expose, by combat-capable unit type where available, start count, additions during the interval, combat losses, end count and replay-attributed kill count.

#### Scenario: Complete unit row

- GIVEN lifecycle and killer-unit evidence support all values for one unit type
- WHEN the combat row is built
- THEN the row SHALL show start, additions, losses, end and credited kills;
- AND reconciliation status SHALL be explicit.

#### Scenario: Unknown kill attribution

- GIVEN victims are known but killer-unit identity is absent or ambiguous
- WHEN kill evidence is rendered
- THEN credited kills SHALL be marked unknown or incomplete;
- AND the UI SHALL NOT render unknown as zero.

#### Scenario: Additions semantics

- GIVEN units become available during a combat interval
- WHEN additions are shown
- THEN wording SHALL preserve ADR-012 semantics;
- AND the report SHALL NOT claim those units physically joined the local fight without spatial evidence.

### Requirement: Separate collateral losses

Worker, infrastructure and static-defence losses SHALL remain separate from combat-unit composition and unit rows.

#### Scenario: Structure losses during combat

- GIVEN production structures are destroyed during an engagement
- WHEN the force table renders
- THEN those losses SHALL appear in the infrastructure category;
- AND they SHALL NOT inflate combat-unit losses or army composition.

### Requirement: Reconciliation and completeness propagation

Participant, side and combat evidence SHALL expose reconciliation and completeness. Partial participant evidence SHALL propagate to derived side totals.

#### Scenario: Partial participant

- GIVEN one participant cannot reconcile start plus additions minus losses to end
- WHEN team totals are built
- THEN that participant SHALL be marked partial;
- AND the team total SHALL also disclose partial completeness;
- AND the system SHALL NOT silently adjust counts to force equality.

### Requirement: Backend authority

The backend SHALL own participant relationship, team grouping, canonical intervals, combat rows, totals, kill-credit completeness and reconciliation semantics.

#### Scenario: Frontend rendering

- GIVEN a complete evidence payload
- WHEN React renders it
- THEN React SHALL NOT infer teammate status, recalculate combat totals or reinterpret missing kill attribution.

### Requirement: Readable dark-theme evidence

Affected normal text, axis labels, tick labels, legends, annotations, table text and interactive focus states SHALL remain readable against their actual dark-theme backgrounds.

#### Scenario: Chart label contrast

- GIVEN the dark report theme shown by the benchmark report
- WHEN the graph section renders
- THEN metric captions, axes, ticks and legend labels SHALL have at least WCAG AA contrast for normal text where measurable;
- AND selected/focused states SHALL remain distinguishable without colour alone.

### Requirement: Responsive evidence presentation

All-participant comparison and combat tables SHALL remain usable on supported mobile widths.

#### Scenario: Mobile metrics

- GIVEN a supported mobile viewport
- WHEN the graph group renders
- THEN the user SHALL be able to inspect every primary metric and participant identity without requiring desktop width.

#### Scenario: Mobile combat table

- GIVEN a combat contains multiple participants and unit types
- WHEN rendered on mobile
- THEN side and participant sections SHALL remain identifiable;
- AND all evidence columns SHALL remain accessible through collapsing, scrolling or an equivalent interaction.

### Requirement: Markdown parity

Markdown SHALL contain the same participant comparison meaning and combat evidence values as the browser, excluding interactive-only synchronization behaviour.

#### Scenario: Downloaded report

- GIVEN the browser displays all-player metric evidence and a combat force table
- WHEN Markdown is downloaded
- THEN it SHALL identify all participant series and their relationship;
- AND it SHALL include team/participant combat rows, completeness, reconciliation and available kill counts.

### Requirement: Additive compatibility

The change SHALL be additive to existing narrative and combat API contracts.

#### Scenario: Existing client

- GIVEN a client ignores new evidence visualization fields
- WHEN it consumes the updated analysis response
- THEN existing narrative and combat fields SHALL retain compatible meaning.

### Requirement: Fixed benchmark website result

The fixed benchmark support bundle SHALL be used for acceptance criteria across backend, browser, Markdown and support-bundle tests.

#### Scenario: Benchmark website

- GIVEN the fixed benchmark support bundle and `dragonDriver` perspective
- WHEN the website renders
- THEN all four players SHALL be comparable on army value, economy proxy and occupied supply where available;
- AND `dragonDriver` SHALL be the dominant solid series;
- AND phase and combat selection SHALL synchronize every graph;
- AND combat cards SHALL contain team-aware force tables with per-unit start, additions, losses, end and available kills;
- AND incomplete evidence SHALL be visible;
- AND chart and table labels SHALL be readable on the dark background.
