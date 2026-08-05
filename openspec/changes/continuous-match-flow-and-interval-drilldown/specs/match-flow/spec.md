# Match Flow Capability Specification

## Status

Proposed.

## Requirement: Continuous match timeline

The report SHALL classify the full match timeline into deterministic adjacent intervals without silent time gaps.

### Scenario: Full match coverage

- GIVEN a replay with normalized match context frames;
- WHEN match flow intervals are generated;
- THEN the first interval starts at match start;
- AND the last interval ends at match end;
- AND adjacent intervals cover the timeline without holes.

### Scenario: Thin evidence

- GIVEN a time range with insufficient evidence for a specific strategic label;
- WHEN intervals are generated;
- THEN the range is labelled `LOW_EVIDENCE` or an equivalent explicit low-confidence category;
- AND it is not omitted.

## Requirement: Non-combat interval classification

The match flow SHALL classify non-combat periods when no fight is detected.

### Scenario: Economic growth without combat

- GIVEN army/combat evidence is quiet;
- AND economy or supply metrics change materially;
- WHEN the interval is generated;
- THEN it is classified as an economic, army buildup, recovery or preparation interval rather than disappearing.

### Scenario: No detected combat in selected interval

- GIVEN a selected interval contains no detected combat;
- WHEN interval drilldown is rendered;
- THEN the report explicitly states that no detected combats occurred in the interval;
- AND displays available non-combat evidence.

## Requirement: Strong graph focus

Selecting an interval SHALL make that interval visually dominant on every graph.

### Scenario: Interval card selected

- GIVEN the user selects an interval card;
- WHEN graphs render;
- THEN the selected interval remains colour-rich and prominent;
- AND non-selected time ranges are muted or greyed;
- AND the same interval bounds apply to every visible metric graph.

### Scenario: No interval selected

- GIVEN no interval is selected;
- WHEN graphs render;
- THEN all-player series remain visible in normal overview mode.

## Requirement: Backend-owned interval drilldown

The backend SHALL own interval-to-evidence mapping and drilldown semantics.

### Scenario: Combat interval

- GIVEN one or more detected combats overlap the selected interval;
- WHEN interval drilldown is serialized;
- THEN the drilldown references those combat IDs;
- AND React renders the corresponding backend-owned combat evidence.

### Scenario: Macro interval

- GIVEN no detected combats overlap the selected interval;
- AND macro/preparation evidence is available;
- WHEN interval drilldown is serialized;
- THEN the drilldown contains the available macro evidence;
- AND does not invent missing decoder data.

## Requirement: Preserve all-player comparison

All-player participant comparison from Narrative Evidence SHALL remain part of the graph experience.

### Scenario: Benchmark replay

- GIVEN the fixed benchmark replay and `dragonDriver` perspective;
- WHEN match-flow graphs render;
- THEN `dragonDriver`, `Lulu`, `Frontdoor` and `Guardian` remain represented on the primary metric graphs where data exists;
- AND selected interval focus does not remove participant identity.

## Requirement: Combat table readability

Combat evidence in interval drilldown SHALL be easier to compare than the current participant card list.

### Scenario: Combat unit row

- GIVEN combat-unit evidence has start count, additions, losses and end count;
- WHEN the combat drilldown renders;
- THEN those values appear on one row per unit type;
- AND credited kills are shown only when supported;
- AND unknown credited kills are rendered as unknown, not zero.

### Scenario: Collateral losses

- GIVEN worker, structure or static-defence losses exist;
- WHEN combat drilldown renders;
- THEN those categories remain separate from combat-unit rows.

## Requirement: No new hidden inference

The match-flow drilldown SHALL NOT introduce new strategic-result, intent, visibility or combat-efficiency claims.

### Scenario: Selected interval explanation

- GIVEN the system explains a selected interval;
- WHEN user-facing text is generated;
- THEN it may describe observed metric changes and detected events;
- AND it SHALL NOT claim a mandatory winner, hidden intent, exact scouting knowledge, or physical participation of additions without evidence.

## Requirement: Markdown and support-bundle parity

Markdown and support bundle output SHALL include the same continuous intervals and interval evidence semantics as the browser.

### Scenario: Downloaded report

- GIVEN the browser displays match-flow intervals and selected interval evidence;
- WHEN Markdown/support bundle is generated;
- THEN interval IDs, labels, time ranges, combat IDs, empty states and limitations are represented consistently.
