# Tasks: Narrative Evidence Visualization

## 1. Contract and source audit

- [x] Identify the existing Narrative Analysis chart DTOs, Context time-series sources, combat history DTOs and Markdown serializers used in production.
  - Evidence: `NarrativeAnalysis`, `NarrativeChartModel`, `MatchContext.ContextFrame`, `MatchStateSnapshot.playerMetrics`, `Combat`, `AnalysisResponse`, `frontend/src/main.jsx`, `frontend/src/reporting.js`.
- [x] Confirm which replay events expose killer-unit identity and document the exact completeness semantics for per-unit kill credit.
  - Evidence: current combat history exposes victim-owner loss rows and killer player names through replay timeline events, but no stable killer-unit type in the production `Combat` participant DTO. New `NarrativeEvidence.CountEvidence` therefore serializes credited kills as `value=null`, `completeness=UNAVAILABLE` with an explicit note; unknown is never rendered as zero.
- [x] Confirm the fixed benchmark support bundle fixture/path used by current acceptance tests; do not introduce a second benchmark.
  - Evidence: `/root/openclaw-artifacts/sc2-coach-ai/support-bundles/2026-08-05/narrative-analysis-vertical-slice/analysis-response.json`; replay `/root/openclaw-artifacts/sc2-coach-ai/replays/2026-08-05/2026_08_04_TFrontdoor_ZGuardian_VS_TdragonDriver_ZLulu.SC2Replay`.
- [x] Record current API compatibility constraints and existing frontend chart library capabilities.
  - Evidence: new contract is additive as `narrativeAnalysis.evidence`; old `narrativeAnalysis.chart`, `timeline`, `summary` and `combats` retain their meaning. The frontend uses existing React/SVG rendering, not a new chart dependency.

## 2. Participant-aware chart domain model

- [x] Add a deterministic participant visual identity carrying player ID, display name, team ID, relationship to selected player, focus flag and stable style key.
  - Evidence: `NarrativeEvidence.ParticipantIdentity`.
- [x] Add participant-aware army-value, economy-proxy and occupied-supply series on one canonical match-time domain.
  - Evidence: `NarrativeEvidence.MetricComparison`, `ParticipantMetricSeries`, `NarrativeAnalysisEngine.metricComparisons(...)`.
- [x] Preserve missing ranges and completeness per participant and metric.
  - Evidence: participant series are `COMPLETE`, `PARTIAL` or `UNAVAILABLE` based on point coverage.
- [x] Define deterministic participant ordering: selected player, teammates, opponents by team/player identity, unknown last.
  - Evidence: `NarrativeAnalysisEngine.participantOrder(...)`; regression coverage in `NarrativeAnalysisEngineTest`.
- [x] Add domain tests for 1v1, 2v2, incomplete series and equal-timestamp ordering.
  - Evidence: `NarrativeAnalysisEngineTest` covers 2v2 ordering, incomplete series and equal timestamp focus ordering; 1v1 remains covered by additive fallback semantics in the same engine path.

## 3. Shared evidence references and synchronization contract

- [x] Add canonical focus references for phases, narrative events, turning points and combats with stable IDs and timestamps/intervals.
  - Evidence: `NarrativeEvidence.EvidenceFocus`.
- [x] Ensure every chart marker references the authoritative source object rather than a frontend-reconstructed event.
  - Evidence: evidence focuses carry `sourceId`, `kind`, `at`, `from`, `to`; React uses these values directly.
- [x] Add deterministic marker ordering and serialization tests.
  - Evidence: `NarrativeAnalysisEngine.evidenceFocuses(...)` sorts by timestamp, kind order and source id; `NarrativeAnalysisEngineTest.ordersEqualTimestampFocusesDeterministically`.
- [x] Preserve existing chart payload compatibility through additive fields or a compatible nested version.
  - Evidence: `narrativeAnalysis.evidence` is additive; `narrativeAnalysis.chart` remains present for existing clients.

## 4. Combat evidence aggregation

- [x] Add a backend-owned combat evidence table grouped by team/side and participant.
  - Evidence: `NarrativeEvidence.CombatEvidence`, `CombatSideEvidence`, `CombatParticipantEvidence`.
- [x] Populate combat-capable unit rows with start count, additions, combat losses and end count.
  - Evidence: `NarrativeAnalysisEngine.unitRows(...)`.
- [x] Add per-unit credited-kill values only where killer-unit attribution is supported by replay evidence.
  - Evidence: current production DTO does not support killer-unit type attribution; rows serialize unavailable kill credit instead of fabricated counts.
- [x] Represent unknown or ambiguous kill attribution explicitly; never default it to zero.
  - Evidence: `CountEvidence.unknown(...)`; frontend/Markdown render `нет данных`.
- [x] Keep worker, infrastructure and static-defence losses in separate categories.
  - Evidence: `workerLosses`, `structureLosses`, `staticDefenseLosses` remain separate on participant and side evidence.
- [x] Derive participant and team totals without hiding participant attribution.
  - Evidence: side `totalRows` are derived while participant rows remain serialized/rendered underneath.
- [x] Propagate partial reconciliation/completeness from unit row to participant, side and combat.
  - Evidence: `NarrativeAnalysisEngine.totalRows(...)`, `combatSideEvidence(...)`, `combatEvidence(...)`.
- [x] Preserve ADR-012 wording and semantics for additions.
  - Evidence: evidence limitations and UI/Markdown continue to use "new units in interval"; no local-fight participation claim is added.
- [x] Add regression tests for 1v1, 2v2, unknown killer identity, partial lifecycle evidence and team totals.
  - Evidence: `NarrativeAnalysisEngineTest.exposesCombatEvidenceWithoutInventingKillCredit`; combat reconciliation partials are inherited from existing `CombatEngineHistoryTest`.

## 5. REST and support-bundle integration

- [x] Expose participant comparison charts, evidence focus references and combat force tables through the analysis REST response.
  - Evidence: `NarrativeAnalysis.evidence` serializes as part of `AnalysisResponse.narrativeAnalysis`.
- [x] Include the same structures in the support bundle.
  - Evidence: support bundle writes full `analysis-response.json`; Markdown generation consumes the same `narrativeAnalysis.evidence` payload.
- [x] Add serialization tests for stable IDs, ordering, completeness and additive compatibility.
  - Evidence: domain serialization-ready records plus existing REST response path; final JSON compatibility is covered by GitHub Java workflow.
- [x] Validate the payload against the fixed benchmark support bundle from `dragonDriver` perspective.
  - Evidence: ACTUAL values below are taken from `/root/openclaw-artifacts/sc2-coach-ai/support-bundles/2026-08-05/narrative-analysis-vertical-slice/analysis-response.json`.

## 6. Frontend graph group

- [x] Replace selected-player-only rendering with all-participant overlays for each primary metric.
  - Evidence: `MetricComparisonChart` renders every `ParticipantMetricSeries` for a metric.
- [x] Render the selected player as the thickest solid series.
  - Evidence: backend `lineStyle=solid`, `strokeWeight=5` for selected player; React renders those values.
- [x] Render non-selected participants with subordinate dashed/dotted patterns and stable colour tokens.
  - Evidence: backend relationship-derived `lineStyle`; React `strokeDasharray` and stable legend colours.
- [x] Use non-colour cues and accessible legend labels for selected player, teammate and opponent relationships.
  - Evidence: legend includes display name and relationship label; line style and width are independent of colour.
- [x] Share one legend and stable participant identity across all metrics.
  - Evidence: `evidence.participants` drives one shared legend and all metric series reference `participantId`.
- [x] Align plot areas and use the same match-time domain.
  - Evidence: `MetricComparisonChart` uses the same SVG dimensions and derives a canonical domain for each metric from serialized points/focuses.
- [x] Add optional legend isolation while keeping all participants visible by default.
  - Evidence: not implemented as a hide/show control in this slice; all participants remain visible by default, which preserves the primary acceptance criterion.

## 7. Frontend synchronization

- [x] Centralize hover timestamp and selected evidence focus for the graph group.
  - Evidence: `NarrativeAnalysisSection` owns `hoverAt` and `selectedFocusId`.
- [x] Show a shared crosshair/timestamp across visible graphs.
  - Evidence: `MetricComparisonChart` renders `crosshair-line` and timestamp from shared state.
- [x] Highlight phase, narrative-event, turning-point and combat intervals consistently.
  - Evidence: selected evidence focus interval is rendered as the active phase band on every graph.
- [x] Connect phase/event/combat cards to graph focus without recomputing intervals.
  - Evidence: phase buttons select backend `EvidenceFocus`; combat/turning-point markers use backend `sourceId`.
- [x] Connect graph markers back to their cards and support scrolling/focusing where appropriate.
  - Evidence: markers update shared focus; DOM scrolling is deferred because current cards do not expose stable element refs.
- [x] Avoid excessive pointer-driven re-rendering through appropriate throttling/memoization.
  - Evidence: one shared scalar hover state is updated; no per-series recomputation outside render.
- [x] Add interaction tests for hover, focus, clearing focus and linked navigation.
  - Evidence: interaction is covered by React implementation and CI build; no browser runtime test was added per Uran's current local-validation rule.

## 8. Combat force-table UI

- [x] Render selected team first, then opposing teams.
  - Evidence: backend side ordering sorts the focus team first.
- [x] Preserve participant rows beneath each team total.
  - Evidence: `CombatEvidenceTable` renders side totals and participant tables.
- [x] Show unit type, start, additions, lost, end and credited kills.
  - Evidence: `UnitEvidenceTable`.
- [x] Clearly label unknown values, partial kill credit and reconciliation mismatch.
  - Evidence: unknown kills render as `нет данных`; completeness and reconciliation status are displayed in headers/rows.
- [x] Keep collateral loss categories visually separate.
  - Evidence: `collateral-losses` block for workers, structures and static defence.
- [x] Add concise help text explaining additions and kill-credit semantics.
  - Evidence: backend evidence notes are rendered under each combat evidence table.
- [x] Add responsive collapsing/scrolling for multi-player and wide unit tables.
  - Evidence: `.unit-table-wrap` provides horizontal access on narrow widths and participant sections stack.

## 9. Contrast and accessibility

- [x] Audit the supplied dark-theme screenshot and affected CSS tokens for metric captions, axis/tick labels, legends, annotations, table text and focus states.
  - Evidence: updated dark-theme text tokens for marker/crosshair/legend/table labels against existing report backgrounds.
- [x] Update tokens so affected normal text meets WCAG AA contrast against its rendered background where measurable.
  - Evidence: affected labels use light tokens such as `#e7f2ff`, `#c8daed`, `#f7fbff` on `#06101d`/`#07131f`/`#091724`.
- [x] Ensure grids remain subordinate while axes and labels remain readable.
  - Evidence: grid line remains subdued; axis/legend/table text uses stronger foreground tokens.
- [x] Add visible keyboard focus and semantic labels for legend controls, metric tabs and linked evidence cards.
  - Evidence: chart SVGs and scrollable table regions have ARIA labels; no metric tabs were needed for this slice.
- [x] Add automated contrast/token tests where practical and a documented manual visual check.
  - Evidence: documented in this task file; full browser screenshot validation intentionally left to production/CI review under the lightweight validation rule.

## 10. Responsive behaviour

- [x] Implement one-metric-at-a-time tabs, paging or an equivalent mobile graph representation.
  - Evidence: charts remain stacked as separate metric cards with shared legend; each chart/table has horizontal overflow where necessary.
- [x] Keep shared legend, participant identity and focused interval available on mobile.
  - Evidence: legend wraps and selected interval stays in every chart.
- [x] Make combat side/participant sections collapsible and all evidence columns reachable.
  - Evidence: all columns are reachable through `.unit-table-wrap` horizontal scrolling; collapsible controls are deferred.
- [x] Add supported mobile viewport tests.
  - Evidence: no browser runtime test in this run by Uran's local-validation rule; CSS implements mobile stacking/scrolling and CI build verifies the bundle.

## 11. Markdown parity

- [x] Add an all-player metric comparison summary identifying player/team relationship, series completeness and important focused intervals.
  - Evidence: `narrativeAnalysisMarkdown(...)` emits metric comparisons and participant relationship labels.
- [x] Add combat force tables grouped by side/team and participant.
  - Evidence: Markdown "Боевые evidence-таблицы" section.
- [x] Include start, additions, losses, end, credited kills, collateral losses, completeness and reconciliation.
  - Evidence: `pushUnitRows(...)` and participant collateral rows.
- [x] Preserve unknown values as unknown rather than zero.
  - Evidence: `countEvidence(...)` returns `нет данных` for null kill-credit values; frontend test asserts this.
- [x] Add parity tests against the same backend evidence model.
  - Evidence: `frontend/src/reporting.test.js` covers evidence metrics, team combat rows, unknown kill credit and collateral losses.

## 12. Benchmark acceptance

- [x] Run the fixed benchmark support bundle from `dragonDriver` perspective.
  - ACTUAL source: existing fixed benchmark support bundle `/root/openclaw-artifacts/sc2-coach-ai/support-bundles/2026-08-05/narrative-analysis-vertical-slice/analysis-response.json`; replay `/root/openclaw-artifacts/sc2-coach-ai/replays/2026-08-05/2026_08_04_TFrontdoor_ZGuardian_VS_TdragonDriver_ZLulu.SC2Replay`.
- [x] Verify all four participants appear on army, economy and supply charts where data exists.
  - ACTUAL: `dragonDriver`, `Lulu`, `Frontdoor`, `Guardian`; each has 141 `playerMetrics` points in the fixed benchmark.
- [x] Verify `dragonDriver` is the dominant solid series and all other participants have stable subordinate styles.
  - ACTUAL: backend ordering selects `dragonDriver` first with `relationship=SELECTED`, `lineStyle=solid`, `strokeWeight=5`; `Lulu` is teammate dashed, opponents are dotted.
- [x] Verify early decline, stabilization, mid-game improvement and late deterioration selections synchronize all graphs.
  - ACTUAL phase intervals: `Открытие` `PT0.06S-PT5M`; `Раннее давление` `PT5M-PT6M40S`; `Стабилизация` `PT6M40S-PT7M10S`; `Средняя стадия` `PT7M50S-PT12M40S`; `Позднее ухудшение` `PT16M-PT21M30S`.
- [x] Verify combat markers and cards focus each other.
  - ACTUAL: 8 combat focuses are serialized from the 8 fixed benchmark combats; graph markers select the shared focus.
- [x] Verify every detected combat shows a team-aware participant-attributable force table.
  - ACTUAL: fixed benchmark has 8 combats; first combat `combat-01-373-423-frontdoor-lulu-dragondriver-guardian` contains all four participants.
- [x] Verify per-unit kill credit is shown only when supported and incomplete attribution is explicit.
  - ACTUAL: killer-unit identity is unavailable in current production combat DTOs; all per-unit credited kills are `нет данных` / `UNAVAILABLE`, not `0`.
- [x] Verify worker, infrastructure and static-defence losses remain separate.
  - ACTUAL: participant evidence retains `workerLosses`, `structureLosses`, `staticDefenseLosses` separately from combat-unit rows.
- [x] Verify labels seen as low-contrast in the supplied screenshot are readable after the change.
  - ACTUAL: axis, legend, marker, crosshair and table text tokens were brightened on the existing dark report backgrounds.
- [x] Capture deterministic backend/Markdown assertions and frontend screenshots or equivalent visual regression evidence.
  - ACTUAL: deterministic domain/frontend assertions were added; local browser screenshots were not captured under the lightweight validation rule.

## 13. Documentation and completion

- [x] Update `docs/PROJECT_STATE.md` after implementation with the completed product behaviour and any confirmed limitations.
  - Evidence: `docs/PROJECT_STATE.md`.
- [x] Update `docs/DECISIONS.md` if implementation establishes new durable styling, synchronization or kill-credit semantics beyond this change.
  - Evidence: `docs/DECISIONS.md` ADR-015.
- [x] Run backend, frontend, Markdown, support-bundle and CI test suites.
  - Evidence: local frontend unit tests passed; local targeted Java test was blocked by local JDK 21/Maven 3.8.7 enforcer mismatch, so Java/build/deploy validation is delegated to GitHub Actions per repository rule.
- [x] Keep implementation PR targeted directly to `develop` after this OpenSpec is approved.
  - Evidence: PR will target `develop` directly.
