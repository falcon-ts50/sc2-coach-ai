# Design: Narrative Evidence Visualization

## Context

Narrative Analysis already provides a backend-owned chart model, phase intervals and event markers. Combat history already exposes participant snapshots, additions, categorized losses and reconciliation. The missing layer is a coherent evidence view model and interaction contract that lets the browser compare players and synchronize all evidence without independently interpreting the match.

## Goals

- Make all-player comparison the default for primary match metrics.
- Keep selected-player focus obvious and stable.
- Synchronize phases, events, combats and charts through explicit IDs and intervals.
- Present combat force evidence in a team-aware, participant-attributable table.
- Preserve uncertainty and partial replay evidence.
- Improve readability and contrast without changing analytical conclusions.

## Proposed contracts

### Participant identity

Introduce or extend a reusable participant descriptor:

```text
ParticipantVisualIdentity
- playerId
- displayName
- teamId
- relationship: SELECTED | TEAMMATE | OPPONENT | UNKNOWN
- focus: boolean
- stableStyleKey
```

`stableStyleKey` is deterministic for the same report and is shared by every metric and combat view. The backend provides identity and relationship semantics; concrete CSS colours may remain frontend theme tokens, but identity mapping must not vary per chart.

### Metric comparison model

```text
ComparisonMetricChart
- metric: ARMY_VALUE | ECONOMY_PROXY | OCCUPIED_SUPPLY
- unitLabel
- matchStartSecond
- matchEndSecond
- participants[]
- phaseIntervals[]
- markers[]
- completeness

ParticipantMetricSeries
- participant
- samples[]
- completeness
- missingRanges[]
```

All metric charts use the same match-time domain. Samples are not silently interpolated across missing ranges unless the source contract already defines interpolation.

### Shared evidence focus

```text
EvidenceFocus
- sourceType: PHASE | NARRATIVE_EVENT | TURNING_POINT | COMBAT | MANUAL_RANGE
- sourceId
- startSecond
- endSecond
- anchorSecond
```

The backend serializes canonical source IDs and intervals. React owns only transient selection state and applies the selected `EvidenceFocus` to every graph and relevant card.

Interactions:

- selecting a phase or card sets shared focus;
- selecting a graph marker sets shared focus and scrolls to the referenced card when present;
- hover exposes one shared timestamp/crosshair across all visible metric graphs;
- clearing focus restores the full domain while retaining all participant series;
- legend toggles may temporarily isolate series but do not mutate the backend result.

### Combat force table model

```text
CombatEvidenceTable
- combatId
- startSecond
- endSecond
- sides[]
- completeness
- limitations[]

CombatSideEvidence
- teamId
- relationshipToSelectedTeam
- participants[]
- totals
- completeness

CombatParticipantEvidence
- playerId
- displayName
- startArmy[]
- additions[]
- combatLosses[]
- endArmy[]
- killsByUnitType[]
- workerLosses
- infrastructureLosses
- staticDefenceLosses
- reconciliation
- completeness

UnitEvidenceRow
- unitType
- startCount
- additionsCount
- lostCount
- endCount
- creditedKills
- killCreditCompleteness
- reconciliationStatus
```

A row may omit unavailable values or mark them unknown; unknown is not rendered as zero. `creditedKills` counts replay-attributed kills by units of that type where the source data supports killer-unit identity. It does not represent damage dealt, efficiency or causal importance.

Team totals are derived from attributable participant rows. If any required participant evidence is partial, the side total is also partial.

## Styling semantics

Default visual hierarchy:

- selected player: solid stroke, greatest width, full opacity;
- teammate: dashed stroke, medium width;
- opponents: distinct dashed patterns or dash offsets plus stable colour tokens;
- unknown relationship: dotted or explicitly labelled fallback;
- focused series/card: additional halo or emphasis that does not rely on colour alone.

The exact palette belongs to the theme, but affected normal text and interactive labels must meet WCAG AA contrast against their actual background. Axis lines and grids may use lower contrast than labels. Focus rings must remain visible.

## Layout

Desktop:

- one shared legend above the graph group;
- vertically stacked metric graphs with aligned plot areas and time axes;
- shared hover timestamp and highlighted interval;
- combat evidence table directly within or beneath each combat card.

Mobile:

- one metric visible at a time through tabs/segmented control or a horizontally pageable equivalent;
- shared legend remains accessible;
- combat sides and participants collapse into sections;
- unit rows may scroll horizontally, but player, unit and status columns remain understandable.

## Backend/frontend responsibility

Backend:

- participant and team identity;
- relationship to selected player;
- metric samples and completeness;
- canonical phase/event/combat intervals and source IDs;
- combat force rows, kill-credit evidence and reconciliation status;
- deterministic ordering.

Frontend:

- theme token selection from `stableStyleKey`;
- rendering line patterns and hierarchy;
- transient hover/focus state;
- scrolling/focusing linked cards;
- responsive layout and accessible interaction.

Frontend SHALL NOT derive teammate relationships, calculate combat totals, reinterpret missing kill credit, or infer phase/combat intervals.

## Ordering

- participant series: selected player, teammates ordered by stable player ID, opponents ordered by team ID then stable player ID, unknown last;
- combat sides: selected team first, then other teams by stable team ID;
- participants within a side: selected player first, then stable player ID;
- unit rows: canonical display-name order after deterministic combat relevance ordering, documented in implementation;
- markers: timestamp, marker-type order, stable source ID.

## Compatibility

The existing narrative chart payload remains readable by old clients. New comparison structures are additive or versioned inside the narrative evidence object. Existing combat history fields remain authoritative and available.

## Testing strategy

- domain tests for deterministic identities, participant ordering and team relationship;
- contract tests for all-player metric series and missing-range handling;
- combat aggregation tests for 1v1 and 2v2, including partial reconciliation and unknown killer attribution;
- frontend tests for line-style hierarchy, synchronized focus and legend behaviour;
- accessibility tests for semantic labels, keyboard focus and measured contrast tokens;
- benchmark support-bundle snapshot/fixture assertions;
- Markdown parity tests.

## Rejected alternatives

### Selected player only by default

Rejected because it prevents direct comparative verification and makes the graph a personal trend chart rather than evidence for match development.

### One combined graph with three y-axes

Rejected because army value, economy proxy and supply have different scales and would produce misleading visual comparisons. Metrics remain separate but synchronized.

### Frontend-generated combat tables

Rejected because attribution, team grouping, completeness and reconciliation are domain semantics, not presentation-only calculations.

### Single army-strength score

Rejected under ADR-004 because it would hide composition, upgrades and uncertainty behind an opaque number.
