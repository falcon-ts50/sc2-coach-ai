# Design: Continuous Match Flow and Interval Drilldown

## Decision

Make Narrative Analysis own a continuous match-flow model. The frontend renders selected intervals and drilldown content from backend-owned contracts; it does not reconstruct missing intervals, decide which combats belong to a phase, or infer macro explanations from raw chart data.

## Contract placement and versioning

Add a backend-owned match-flow contract to `NarrativeAnalysis`, tentatively as `narrativeAnalysis.matchFlow`.
The field is additive and may be ignored by older clients. The serialized contract has its own schema version so that
continuous-flow behaviour can evolve without changing the entire `NarrativeAnalysis` schema:

```java
record MatchFlow(
    String schemaVersion,
    Duration matchStartedAt,
    Duration matchEndedAt,
    List<MatchFlowInterval> intervals,
    List<String> overviewCombatIds,
    List<String> limitations
)
```

`matchStartedAt` and `matchEndedAt` are the canonical bounds used for no-gap validation, graph masking,
Markdown output and support-bundle acceptance. `overviewCombatIds` is optional presentation data for no-selection
mode; interval-specific drilldown still lives on each interval.

## Interval data model

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
    List<String> snapshotIds,
    List<String> transitionIds,
    List<String> eventIds,
    List<String> evidenceIds,
    List<String> combatIds,
    Map<String, IntervalMetrics> startMetricsByParticipantId,
    Map<String, IntervalMetrics> endMetricsByParticipantId,
    IntervalDelta delta,
    IntervalDrilldown drilldown,
    List<String> limitations
)
```

Required interval semantics:

- `id` is stable for identical deterministic inputs, for example `match-flow-000`, and is the only ID React stores for selection.
- `ordinal` is zero-based and strictly increasing in serialized order.
- Bounds are half-open `[startedAt, endedAt)` except the final interval, which ends at `matchEndedAt`.
- `startedAt < endedAt`; zero-duration intervals are invalid and must be dropped or merged before serialization.
- `confidence` is clamped to `[0, 1]`; `completeness` is never null.
- `snapshotIds`, `transitionIds`, `eventIds`, `evidenceIds` and `combatIds` are references to existing backend-owned facts.
- `startMetricsByParticipantId` and `endMetricsByParticipantId` use the same participant IDs as `NarrativeEvidence.ParticipantIdentity`.
- `delta` is computed from the serialized start/end metrics; React and Markdown do not recompute it from chart points.
- `limitations` carries missing-data caveats specific to the interval.

`Kind` must include both combat and non-combat categories. The fallback is explicit `LOW_EVIDENCE`, not a gap.

Tentative support records:

```java
record IntervalMetrics(
    double armyValue,
    double economyProxy,
    double supplyUsed,
    Completeness completeness,
    List<String> sourceSnapshotIds
)

record IntervalDelta(
    Map<String, MetricDelta> byParticipantId,
    Completeness completeness,
    List<String> limitations
)

record MetricDelta(
    double armyValueDelta,
    double economyProxyDelta,
    double supplyUsedDelta
)
```

The exact Java names may change during implementation, but the serialized meaning must remain equivalent.

## Continuous segmentation

The engine should produce a partition of the match:

```text
match start = interval[0].startedAt
interval[i].endedAt = interval[i + 1].startedAt
interval[last].endedAt = match end
```

Boundary rules:

- Candidate boundaries may come from match start/end, existing Narrative phases, combat windows and meaningful context-frame changes.
- Boundaries must be sorted, deduplicated and normalized before interval classification.
- Small overlaps from combat windows or phase boundaries must be normalized into deterministic adjacent intervals.
- Small holes introduced by missing phase boundaries must be filled by non-combat or `LOW_EVIDENCE` intervals.
- If a combat overlaps a non-combat segment, the interval can be classified as `COMBAT` or can carry combat evidence inside a broader phase, but the final serialized intervals must not leave holes.
- A combat maps to every interval it overlaps by positive duration after boundary normalization.
- Multiple combats may map to the same interval.
- No interval may reference a combat whose `[startedAt, endedAt]` has no temporal overlap with the interval.

## Classification approach

Classification should be deterministic and evidence-based:

- use existing Narrative phases and state transitions as high-level boundaries;
- use Combat history windows as combat evidence;
- use `MatchContext` player metrics for army/economy/supply deltas;
- use existing timeline/transcript events where they already support upgrades, production, scouting/contact, expansions or losses;
- degrade to `REGROUPING_OR_LOW_ACTIVITY` or `LOW_EVIDENCE` when evidence is thin.

The classification should prefer cautious labels over confident fiction. `LOW_EVIDENCE` is acceptable; unclassified time is not.

Initial classification precedence:

1. `COMBAT` when one or more detected combats materially overlap the interval.
2. `RECOVERY` when army/economy metrics rebound after a detected loss or pressure window.
3. `ECONOMIC_GROWTH` when economy or occupied supply grows materially and combat evidence is quiet.
4. `ARMY_BUILDUP` when army value or army supply grows materially without detected combat.
5. `TECH_TRANSITION` only when upgrade, technology or production-tech events exist in the current evidence sources.
6. `MAP_CONTROL_OR_SCOUTING` only when command/contact/scouting evidence exists.
7. `PRESSURE_PREPARATION` when current evidence supports preparation before a detected pressure window.
8. `REGROUPING_OR_LOW_ACTIVITY` for observable but weak state changes.
9. `LOW_EVIDENCE` when none of the above can be supported.

These rules are intentionally conservative. They can be tuned in a versioned config, but they must be deterministic and covered by tests.

## Graph focus rendering

The backend serializes interval identity and canonical interval bounds. React stores the currently selected interval ID and should:

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
    List<String> combatIds,
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

`MacroEvidence` and `PreparationEvidence` may start as compact records built from existing `MatchContext`,
timeline and transcript-derived facts. If a data source is absent, the interval carries a limitation or empty state;
it does not invent bases, production queues, hidden vision, intent or unit movement.

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

No-selection browser mode should keep the all-player overview graph visible. The implementation may either show all combats
below the graph or hide interval drilldown until a card is selected, but the choice must be documented in `tasks.md` and
Markdown/support bundle output must still include all interval drilldowns.

## Fixed benchmark acceptance notes

The fixed artifact currently used by previous Narrative Analysis work is:

- replay: `/root/openclaw-artifacts/sc2-coach-ai/replays/2026-08-05/2026_08_04_TFrontdoor_ZGuardian_VS_TdragonDriver_ZLulu.SC2Replay`
- support bundle response: `/root/openclaw-artifacts/sc2-coach-ai/support-bundles/2026-08-05/narrative-analysis-vertical-slice/analysis-response.json`
- replay SHA-256 observed during REVIEW refinement: `9031068478141554827027ee64b16951fec123c0c90acb2ebb6de99358e11315`

The older Narrative phase model on that artifact has five phase intervals and leaves visible holes, including:

- 7:10-7:50;
- 12:40-16:00;
- 21:30-match end, where match duration is about 23:11.

The APPLY implementation must record the new ACTUAL match-flow interval count, exact interval bounds, kind distribution,
combat-to-interval mapping and no-combat interval empty states for this same artifact.

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
