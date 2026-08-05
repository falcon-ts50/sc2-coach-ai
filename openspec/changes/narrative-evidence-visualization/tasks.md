# Tasks: Narrative Evidence Visualization

## 1. Contract and source audit

- [ ] Identify the existing Narrative Analysis chart DTOs, Context time-series sources, combat history DTOs and Markdown serializers used in production.
- [ ] Confirm which replay events expose killer-unit identity and document the exact completeness semantics for per-unit kill credit.
- [ ] Confirm the fixed benchmark support bundle fixture/path used by current acceptance tests; do not introduce a second benchmark.
- [ ] Record current API compatibility constraints and existing frontend chart library capabilities.

## 2. Participant-aware chart domain model

- [ ] Add a deterministic participant visual identity carrying player ID, display name, team ID, relationship to selected player, focus flag and stable style key.
- [ ] Add participant-aware army-value, economy-proxy and occupied-supply series on one canonical match-time domain.
- [ ] Preserve missing ranges and completeness per participant and metric.
- [ ] Define deterministic participant ordering: selected player, teammates, opponents by team/player identity, unknown last.
- [ ] Add domain tests for 1v1, 2v2, incomplete series and equal-timestamp ordering.

## 3. Shared evidence references and synchronization contract

- [ ] Add canonical focus references for phases, narrative events, turning points and combats with stable IDs and timestamps/intervals.
- [ ] Ensure every chart marker references the authoritative source object rather than a frontend-reconstructed event.
- [ ] Add deterministic marker ordering and serialization tests.
- [ ] Preserve existing chart payload compatibility through additive fields or a compatible nested version.

## 4. Combat evidence aggregation

- [ ] Add a backend-owned combat evidence table grouped by team/side and participant.
- [ ] Populate combat-capable unit rows with start count, additions, combat losses and end count.
- [ ] Add per-unit credited-kill values only where killer-unit attribution is supported by replay evidence.
- [ ] Represent unknown or ambiguous kill attribution explicitly; never default it to zero.
- [ ] Keep worker, infrastructure and static-defence losses in separate categories.
- [ ] Derive participant and team totals without hiding participant attribution.
- [ ] Propagate partial reconciliation/completeness from unit row to participant, side and combat.
- [ ] Preserve ADR-012 wording and semantics for additions.
- [ ] Add regression tests for 1v1, 2v2, unknown killer identity, partial lifecycle evidence and team totals.

## 5. REST and support-bundle integration

- [ ] Expose participant comparison charts, evidence focus references and combat force tables through the analysis REST response.
- [ ] Include the same structures in the support bundle.
- [ ] Add serialization tests for stable IDs, ordering, completeness and additive compatibility.
- [ ] Validate the payload against the fixed benchmark support bundle from `dragonDriver` perspective.

## 6. Frontend graph group

- [ ] Replace selected-player-only rendering with all-participant overlays for each primary metric.
- [ ] Render the selected player as the thickest solid series.
- [ ] Render non-selected participants with subordinate dashed/dotted patterns and stable colour tokens.
- [ ] Use non-colour cues and accessible legend labels for selected player, teammate and opponent relationships.
- [ ] Share one legend and stable participant identity across all metrics.
- [ ] Align plot areas and use the same match-time domain.
- [ ] Add optional legend isolation while keeping all participants visible by default.

## 7. Frontend synchronization

- [ ] Centralize hover timestamp and selected evidence focus for the graph group.
- [ ] Show a shared crosshair/timestamp across visible graphs.
- [ ] Highlight phase, narrative-event, turning-point and combat intervals consistently.
- [ ] Connect phase/event/combat cards to graph focus without recomputing intervals.
- [ ] Connect graph markers back to their cards and support scrolling/focusing where appropriate.
- [ ] Avoid excessive pointer-driven re-rendering through appropriate throttling/memoization.
- [ ] Add interaction tests for hover, focus, clearing focus and linked navigation.

## 8. Combat force-table UI

- [ ] Render selected team first, then opposing teams.
- [ ] Preserve participant rows beneath each team total.
- [ ] Show unit type, start, additions, lost, end and credited kills.
- [ ] Clearly label unknown values, partial kill credit and reconciliation mismatch.
- [ ] Keep collateral loss categories visually separate.
- [ ] Add concise help text explaining additions and kill-credit semantics.
- [ ] Add responsive collapsing/scrolling for multi-player and wide unit tables.

## 9. Contrast and accessibility

- [ ] Audit the supplied dark-theme screenshot and affected CSS tokens for metric captions, axis/tick labels, legends, annotations, table text and focus states.
- [ ] Update tokens so affected normal text meets WCAG AA contrast against its rendered background where measurable.
- [ ] Ensure grids remain subordinate while axes and labels remain readable.
- [ ] Add visible keyboard focus and semantic labels for legend controls, metric tabs and linked evidence cards.
- [ ] Add automated contrast/token tests where practical and a documented manual visual check.

## 10. Responsive behaviour

- [ ] Implement one-metric-at-a-time tabs, paging or an equivalent mobile graph representation.
- [ ] Keep shared legend, participant identity and focused interval available on mobile.
- [ ] Make combat side/participant sections collapsible and all evidence columns reachable.
- [ ] Add supported mobile viewport tests.

## 11. Markdown parity

- [ ] Add an all-player metric comparison summary identifying player/team relationship, series completeness and important focused intervals.
- [ ] Add combat force tables grouped by side/team and participant.
- [ ] Include start, additions, losses, end, credited kills, collateral losses, completeness and reconciliation.
- [ ] Preserve unknown values as unknown rather than zero.
- [ ] Add parity tests against the same backend evidence model.

## 12. Benchmark acceptance

- [ ] Run the fixed benchmark support bundle from `dragonDriver` perspective.
- [ ] Verify all four participants appear on army, economy and supply charts where data exists.
- [ ] Verify `dragonDriver` is the dominant solid series and all other participants have stable subordinate styles.
- [ ] Verify early decline, stabilization, mid-game improvement and late deterioration selections synchronize all graphs.
- [ ] Verify combat markers and cards focus each other.
- [ ] Verify every detected combat shows a team-aware participant-attributable force table.
- [ ] Verify per-unit kill credit is shown only when supported and incomplete attribution is explicit.
- [ ] Verify worker, infrastructure and static-defence losses remain separate.
- [ ] Verify labels seen as low-contrast in the supplied screenshot are readable after the change.
- [ ] Capture deterministic backend/Markdown assertions and frontend screenshots or equivalent visual regression evidence.

## 13. Documentation and completion

- [ ] Update `docs/PROJECT_STATE.md` after implementation with the completed product behaviour and any confirmed limitations.
- [ ] Update `docs/DECISIONS.md` if implementation establishes new durable styling, synchronization or kill-credit semantics beyond this change.
- [ ] Run backend, frontend, Markdown, support-bundle and CI test suites.
- [ ] Keep implementation PR targeted directly to `develop` after this OpenSpec is approved.
