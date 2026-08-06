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

### Scenario: Adjacent interval partition

- GIVEN generated match-flow intervals;
- WHEN they are serialized;
- THEN interval ordinals are strictly increasing from zero;
- AND every interval has `startedAt < endedAt`;
- AND every interval after the first starts exactly when the previous interval ends;
- AND no serialized intervals overlap.

### Scenario: Previously missing phase ranges

- GIVEN the fixed benchmark replay for `dragonDriver`;
- AND the previous Narrative phase model left uncovered time ranges including 7:10-7:50, 12:40-16:00 and 21:30-match end;
- WHEN match-flow intervals are generated;
- THEN those ranges are covered by explicit `COMBAT`, non-combat, `REGROUPING_OR_LOW_ACTIVITY` or `LOW_EVIDENCE` intervals;
- AND none of those ranges disappear from the report.

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

### Scenario: Low-confidence fallback is explicit

- GIVEN a combat-free interval with weak metric deltas and no usable production, scouting or tech evidence;
- WHEN the interval is classified;
- THEN its kind is `REGROUPING_OR_LOW_ACTIVITY`, `LOW_EVIDENCE` or an equivalent explicit low-confidence category;
- AND the interval carries confidence/completeness values and limitations.

### Scenario: No detected combat in selected interval

- GIVEN a selected interval contains no detected combat;
- WHEN interval drilldown is rendered;
- THEN the report explicitly states that no detected combats occurred in the interval;
- AND displays available non-combat evidence.

### Scenario: No development evidence in selected interval

- GIVEN a selected interval has no available economy, production, upgrade, tech, scouting or preparation evidence;
- WHEN interval drilldown is rendered;
- THEN the report explicitly states that no development evidence is available for that interval;
- AND the interval remains visible in the continuous timeline.

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

## Requirement: Backend-owned match-flow contract

The backend SHALL serialize an additive match-flow contract with stable interval identity and backend-owned evidence mapping.

### Scenario: Interval contract fields

- GIVEN match-flow intervals are serialized in `NarrativeAnalysis`;
- WHEN a client reads an interval;
- THEN it has stable `id`, `ordinal`, `kind`, `title`, `startedAt`, `endedAt`, `confidence`, `completeness`, `summary`, evidence references, combat IDs, metric start/end data, metric deltas, drilldown and limitations;
- AND React does not need to infer interval membership or recompute interval deltas from chart points.

### Scenario: Participant metric identity

- GIVEN interval metrics are serialized;
- WHEN they reference a participant;
- THEN the participant ID matches an ID from `NarrativeEvidence.ParticipantIdentity`;
- AND the same identity can be used across army, economy and supply graph series.

## Requirement: Backend-owned interval drilldown

The backend SHALL own interval-to-evidence mapping and drilldown semantics.

### Scenario: Every interval has combat and development sections

- GIVEN a match-flow interval is serialized;
- WHEN interval drilldown is read;
- THEN it contains a combat section;
- AND it contains a development section;
- AND each section contains either interval-relevant evidence or an explicit empty state.

### Scenario: Combat interval

- GIVEN one or more detected combats overlap the selected interval;
- WHEN interval drilldown is serialized;
- THEN the combat drilldown section references those combat IDs;
- AND React renders the corresponding backend-owned combat evidence.

### Scenario: Multiple combats in one interval

- GIVEN multiple detected combats overlap the same selected interval;
- WHEN interval drilldown is serialized;
- THEN every overlapping combat ID is included in the combat section in deterministic chronological order;
- AND unrelated combats outside the interval are not included.

### Scenario: Combat interval also has development evidence

- GIVEN one or more detected combats overlap the selected interval;
- AND economy, production, upgrade, tech, scouting or preparation evidence is also available inside the same bounds;
- WHEN interval drilldown is serialized;
- THEN the combat section includes the overlapping combats;
- AND the development section includes the available development evidence;
- AND the combat classification does not suppress the development section.

### Scenario: Macro/development interval

- GIVEN no detected combats overlap the selected interval;
- AND macro/preparation evidence is available;
- WHEN interval drilldown is serialized;
- THEN the combat section contains an explicit no-combat empty state;
- AND the development section contains the available macro/preparation evidence;
- AND does not invent missing decoder data.

### Scenario: Empty non-combat interval

- GIVEN no detected combats overlap the selected interval;
- AND no macro/preparation evidence is available beyond low-confidence state changes;
- WHEN interval drilldown is serialized;
- THEN the combat section contains an explicit no-combat empty state;
- AND the development section contains an explicit no-development-evidence empty state;
- AND limitations explain which data is unavailable.

## Requirement: Preserve all-player comparison

All-player participant comparison from Narrative Evidence SHALL remain part of the graph experience.

### Scenario: Benchmark replay

- GIVEN the fixed benchmark replay and `dragonDriver` perspective;
- WHEN match-flow graphs render;
- THEN `dragonDriver`, `Lulu`, `Frontdoor` and `Guardian` remain represented on the primary metric graphs where data exists;
- AND selected interval focus does not remove participant identity.

### Scenario: Selected interval focus preserves context

- GIVEN the user selects an interval;
- WHEN the army, economy and supply graphs render;
- THEN all benchmark participants remain identifiable;
- AND non-selected time is muted without deleting series identity or relationship labels.

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
- THEN interval IDs, labels, time ranges, combat IDs, combat empty states, development empty states and limitations are represented consistently.

### Scenario: Fixed benchmark acceptance record

- GIVEN the fixed benchmark replay/support bundle and `dragonDriver` perspective;
- WHEN implementation verification is performed;
- THEN the PR records ACTUAL interval count, first/last timestamps, category distribution, combat-to-interval mapping, no-combat examples and no-development-evidence examples;
- AND verifies the full timeline has no temporal gaps.
