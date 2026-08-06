# Version 0.9.0 requirements

## Requirement: Match dashboard summary

The system SHALL expose and render a compact match summary containing match identity, participants, teams, races, duration and official replay result.

### Scenario: Team replay summary

- GIVEN the fixed benchmark support bundle
- WHEN the report is opened for the selected player
- THEN the dashboard shows all teams and participants without treating teammates as opponents
- AND the official replay result is labelled as official
- AND no inferred strategic result is displayed unless a future backend contract explicitly provides it

## Requirement: Backend-owned summary metrics

The backend SHALL provide visualization-ready summary metrics with evidence references, units, completeness and limitations.

### Scenario: KPI rendering

- GIVEN the fixed benchmark support bundle
- WHEN summary metrics are serialized
- THEN React and Markdown can render the same values without recomputing maxima or totals
- AND unavailable values are represented as unavailable rather than zero

## Requirement: Primary comparative chart

The website SHALL render one primary chart workspace with selectable metric tabs and all relevant participants on a shared match-time axis.

### Scenario: Switching metric tabs

- GIVEN army, economy and supply series
- WHEN the user switches the active metric
- THEN participant identity and line semantics remain stable
- AND the selected player remains visually dominant
- AND the selected evidence range remains synchronized

## Requirement: Key episode navigation

The backend SHALL expose key evidence episodes that aggregate references to MatchFlow intervals, turning points and combats without replacing their analytical ownership.

### Scenario: Selecting a turning episode

- GIVEN an episode with related combat and turning-point references
- WHEN the user selects its card
- THEN the same time range is highlighted on the active chart
- AND the related evidence panel is opened
- AND unrelated combats are not presented as part of the episode

## Requirement: Episode-wide synchronization

The website SHALL use one selected episode state to synchronize charts, combat evidence, metric deltas and timeline position.

### Scenario: Selection from chart

- GIVEN the user selects a marked interval on the chart
- WHEN that interval maps to an evidence episode
- THEN the corresponding episode card, combat evidence and timeline range become selected

## Requirement: Team-aware force table

For a related combat, the website SHALL render backend-owned force data grouped by team and participant.

### Scenario: Reconstructed combat

- GIVEN combat evidence with army at start, additions, losses and army at end
- WHEN the episode detail is rendered
- THEN combat units are not mixed with workers, infrastructure or static defence
- AND reconciliation/completeness status is visible when incomplete
- AND additions are not described as confirmed local reinforcements without spatial evidence

## Requirement: Unsupported evidence stays absent

The website SHALL NOT invent strategic outcomes, exact unit kill credit, visibility or spatial battle maps.

### Scenario: Missing killer identity

- GIVEN combat data without stable killer-unit identity
- WHEN the force table is rendered
- THEN kill credit is shown as unavailable or omitted
- AND it is not displayed as zero

### Scenario: Missing spatial evidence

- GIVEN a combat without validated coordinates
- WHEN its card is rendered
- THEN no fabricated battle mini-map is shown
- AND a non-spatial summary is used instead

## Requirement: Responsive dashboard

The dashboard SHALL preserve access to all primary evidence at viewport widths down to 320 CSS pixels.

### Scenario: Mobile report

- GIVEN the fixed benchmark support bundle
- WHEN the report is viewed at mobile width
- THEN summary, active chart, episode cards, selected evidence and timeline remain accessible
- AND participant identity does not rely on colour alone
- AND dense tables use local scrolling only where necessary

## Requirement: Output parity

New backend-owned facts SHALL remain available through REST, React, Markdown and the support bundle.

### Scenario: Benchmark parity

- GIVEN the fixed benchmark support bundle
- WHEN all output formats are generated
- THEN episode IDs, summary metric values, evidence ranges and combat references agree across outputs
