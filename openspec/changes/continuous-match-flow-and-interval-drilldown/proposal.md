# Change: Continuous Match Flow and Interval Drilldown

## Status

Proposed — REVIEW packet refined with contract and benchmark acceptance expectations; APPLY only after approval.

## Problem

The current `Ход матча` section is useful, but it still behaves like a set of notable episodes laid over graphs. It leaves unclassified gaps, uses weak interval highlighting, and does not make the selected interval the main navigation object.

For StarCraft II analysis this is the wrong centre of gravity. The match is continuous: if there is no fight, there is still economy, production, scouting, regrouping, tech, expansion, recovery, map control, or at least low-evidence time. The report should treat the graph and its intervals as the core match-flow interface, with combat and macro evidence shown as explanations of the selected interval.

## Goals

1. Make `Ход матча` a continuous timeline without temporal gaps from match start to match end.
2. Classify every interval, including non-combat economic and preparation periods.
3. Make selected interval focus visually dominant on all graphs: selected interval remains coloured/legible, non-selected time is visibly muted.
4. Add interval drilldown below the graphs:
   - combat evidence inside the selected interval, or an explicit no-combat empty state;
   - development evidence inside the same selected interval, or an explicit no-development-evidence empty state;
   - economic/tech/production/scouting/preparation evidence for every interval where data exists, including intervals that also contain combat.
5. Keep all-player graph comparisons introduced by `narrative-evidence-visualization`.
6. Start redesigning combat evidence presentation so army state, additions, losses and end state are easier to compare.

## REVIEW-ready refinements

Before APPLY, this change packet must be treated as owning a concrete backend contract, not only a UI idea:

- `NarrativeAnalysis` gets an additive backend-owned `matchFlow` payload, or an equivalent nested field with the same semantics.
- `matchFlow.intervals` is a no-gap partition of the match from canonical match start to canonical match end.
- Each interval has a stable ID, ordinal, kind, title, time bounds, confidence, completeness, evidence references, combat references, metric deltas, drilldown content and limitations.
- Every selected interval drilldown is serialized by the backend. React stores only selected interval ID/UI state and renders the already-owned facts.
- Drilldown content is two-track for every interval: combat and development/macro are independent sections, each with its own evidence or empty state.
- The fixed benchmark replay/support bundle for `dragonDriver` must be used to record ACTUAL interval count, interval bounds, category distribution, combat mapping, and combat/development empty states before implementation is considered done.

## Non-goals

- Do not introduce a single opaque combat efficiency, army power, or strategic-result score.
- Do not claim a produced unit physically joined a fight without spatial evidence.
- Do not infer hidden player intent or exact vision state.
- Do not replace Combat Engine V2/V3 in this change.
- Do not make React infer phases, combat membership, team relationship, or reconciliation semantics.

## User-facing intent

The first screen of the generated report should answer:

- what happened during every part of the match;
- who was ahead or falling behind on the graph;
- what evidence explains the selected interval;
- whether the interval was combat-driven, economy-driven, preparation-driven, scouting-driven, recovery, or low-evidence.

Selecting an interval card should make the graph look like a focused lens:

- the selected time range stays colour-rich and prominent;
- all other time ranges become lower contrast, greyed or subdued;
- interval-specific evidence appears below the graph;
- combat and development evidence are shown as separate interval sections, because both may exist in the same time range;
- unrelated fight cards no longer dominate the page unless no interval is selected.

## Proposed interval taxonomy

Initial categories:

- `OPENING_BUILDUP` — initial build-up before enough differentiated evidence exists;
- `ECONOMIC_GROWTH` — worker/economy/supply expansion dominates the interval;
- `TECH_TRANSITION` — technology/upgrades/production tech shifts are the main evidence;
- `ARMY_BUILDUP` — army value/supply rises without a detected fight;
- `MAP_CONTROL_OR_SCOUTING` — scouting/contact/map-presence evidence dominates;
- `PRESSURE_PREPARATION` — army/economy posture suggests preparation before a detected pressure window;
- `COMBAT` — one or more detected combats materially overlap the interval;
- `RECOVERY` — army/economy rebuild after losses or pressure;
- `REGROUPING_OR_LOW_ACTIVITY` — limited direct evidence, but state changes remain observable;
- `LOW_EVIDENCE` — fallback classification when the system cannot confidently label the interval.

Names may be adjusted during REVIEW, but the important rule is: no silent time gaps.

## Combat table redesign direction

The current combat evidence table is factually useful but still too hard to scan. The next design should prefer a first-pass shape like:

```text
Unit        Start   New   Lost   End   Kills
Zergling       2    +16     -3    15   unknown
Queen          3     +0      0     3   unknown
```

Then show:

- side/team total first;
- participant rows underneath;
- collateral losses separately;
- reconciliation status next to the row or participant;
- unknown kill credit as unknown, never `0`.

The final visual design should be discussed before APPLY, but this change should keep combat details tied to interval drilldown.

## Expected result on the website

For the fixed benchmark replay and `dragonDriver` perspective:

1. `Ход матча` covers the full match timeline with no unclassified gaps between adjacent intervals.
2. Every interval card has a type, time range, concise title, confidence/completeness, and evidence summary.
3. Clicking an interval card strongly highlights that interval on all participant graphs and mutes non-selected time.
4. All-player army/economy/supply graph series remain visible when no interval is selected.
5. When an interval is selected, evidence below the graph shows only interval-relevant content.
6. If the interval contains combats, those combats appear in the combat section of the drilldown.
7. If the interval contains no detected combats, the combat section says so explicitly.
8. The development section is still evaluated for every selected interval, including combat intervals.
9. If no economy, production, upgrade, tech, scouting or preparation evidence is available for the interval, the development section says so explicitly rather than disappearing.
10. Combat evidence remains team-aware and participant-attributable.
11. The UI does not add winner, efficiency, intent, hidden-vision or physical-participation claims.
12. The previously observed Narrative Analysis phase holes on the fixed artifact, including the 7:10-7:50, 12:40-16:00 and 21:30-match-end ranges, are covered by explicit match-flow intervals rather than disappearing.

## Rollout

Implement as a repository-native OpenSpec change from `develop`. The first APPLY should prioritize the continuous interval model and interval drilldown. Combat-table redesign may either be included if small enough or split into a follow-up OpenSpec change after the table UX is discussed.
