# Design: Continuous Match Flow and Interval Drilldown

## Decision

Make Narrative Analysis own a continuous match-flow model. The frontend renders selected intervals and drilldown content from backend-owned contracts; it does not reconstruct missing intervals, decide which combats belong to a phase, or infer macro explanations from raw chart data.

## Data model

Add a backend-owned interval contract, tentatively:

```java
record MatchFlowInterval(
    String id,
    int ordinal,
    Kind kind,
    String title,
    Duration startedAt,
    Duration endedAt,
    double confidence,
    Completeness completeness,
    String summary,
    List<String> evidenceIds,
    List<String> combatIds,
    IntervalMetrics startMetrics,
    IntervalMetrics endMetrics,
    IntervalDelta delta,
    IntervalDrilldown drilldown,
    List<String> limitations
)
```

`Kind` must include both combat and non-combat categories. The fallback is explicit `LOW_EVIDENCE`, not a gap.

## Continuous segmentation

The engine should produce a partition of the match:

```text
match start = interval[0].startedAt
interval[i].endedAt = interval[i + 1].startedAt
interval[last].endedAt = match end
```

Small overlaps from combat windows or phase boundaries must be normalized into deterministic adjacent intervals. If a combat overlaps a non-combat segment, the interval can be classified as `COMBAT` or can carry combat evidence inside a broader phase, but the final serialized intervals must not leave holes.

## Classification approach

Classification should be deterministic and evidence-based:

- use existing Narrative phases and state transitions as high-level boundaries;
- use Combat history windows as combat evidence;
- use `MatchContext` player metrics for army/economy/supply deltas;
- use existing timeline/transcript events where they already support upgrades, production, scouting/contact, expansions or losses;
- degrade to `REGROUPING_OR_LOW_ACTIVITY` or `LOW_EVIDENCE` when evidence is thin.

The classification should prefer cautious labels over confident fiction. `LOW_EVIDENCE` is acceptable; unclassified time is not.

## Graph focus rendering

The backend serializes the selected interval identity and canonical interval bounds. React should:

- keep all-player series available;
- draw the selected interval with full colour/opacity;
- render non-selected time in grey or low opacity;
- apply the same selected interval to every graph;
- keep selected player emphasis from the previous evidence visualization change;
- preserve keyboard and pointer interaction for interval cards and graph markers.

When no interval is selected, all intervals and all participant series may remain visible in normal mode.

## Drilldown model

The selected interval drilldown should be serialized by the backend:

```java
record IntervalDrilldown(
    List<CombatEvidence> combats,
    MacroEvidence macro,
    PreparationEvidence preparation,
    List<String> emptyStates,
    List<String> limitations
)
```

Required semantics:

- if no combats overlap the selected interval, say so explicitly;
- show macro/preparation evidence when available;
- do not fabricate economy, production or scouting data when the decoder does not expose it;
- reuse existing `NarrativeEvidence.CombatEvidence` for combat drilldown.

## Combat table UX direction

The combat table should optimize for comparison:

1. Side/team total summary.
2. Per-participant rows under the side.
3. One unit type per row: start, additions, losses, end, credited kills.
4. Collateral worker/structure/static-defence losses outside combat-unit rows.
5. Reconciliation and unknown kill credit visible but visually subordinate to the counts.

The backend remains authoritative for totals and reconciliation. React can collapse/expand sections but cannot recompute the totals.

## Compatibility

This should be additive:

- existing `narrativeAnalysis.timeline.phases` remains available;
- existing `narrativeAnalysis.evidence` remains available;
- new `matchFlow` or `narrativeAnalysis.matchFlow` field can be ignored by older clients;
- Markdown/support bundle must include the same interval and drilldown data.

## Risks

- Continuous segmentation can look falsely confident.
- Too many interval categories can make the report feel noisy.
- Strong focus rendering can hide useful context if the muted background is too faint.
- Combat tables can become wide and dense on mobile.

## Mitigations

- Carry confidence/completeness on every interval.
- Keep category set small and deterministic.
- Preserve a no-selection overview mode.
- Use mobile-friendly drilldown sections and horizontal table scrolling only where needed.
- Keep unknown/missing data explicit.
