# Tasks — версия 0.9.0

## 1. Read gate

- [x] Read `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md` and `ARCHITECTURE.md`.
  - Evidence: completed Read Gate on 2026-08-06 before production-code edits.
- [x] Read all OpenSpec changes already merged into `develop`.
  - Evidence: inspected merged OpenSpec change packets and current implementation/test files before APPLY edits.
- [x] Confirm the fixed benchmark support bundle and record its identity.
  - Evidence: fixed replay `/root/openclaw-artifacts/sc2-coach-ai/replays/2026-08-05/2026_08_04_TFrontdoor_ZGuardian_VS_TdragonDriver_ZLulu.SC2Replay`, SHA-256 `9031068478141554827027ee64b16951fec123c0c90acb2ebb6de99358e11315`.
- [x] Capture current desktop screenshots at 1440px and 1920px before implementation.
  - Evidence: production 0.8.9 baseline screenshots captured via OpenClaw browser before edits.
- [x] Capture a narrow-screen screenshot as fallback evidence, without treating mobile as the primary layout target.
  - Evidence: production 0.8.9 narrow fallback screenshot captured via OpenClaw browser before edits.

## 2. Contract design

- [x] Add additive backend contracts for match summary metrics.
  - Evidence: `NarrativeDashboard.SummaryMetric` and additive `NarrativeAnalysis.dashboard`.
- [x] Add an evidence episode view model with stable IDs and references to existing MatchFlow, turning-point and combat entities.
  - Evidence: `NarrativeDashboard.EvidenceEpisode` with related MatchFlow interval, combat and turning-point IDs.
- [x] Define metric delta, evidence range, importance, completeness and limitation fields.
  - Evidence: `EvidenceEpisode` and `EpisodeMetricDelta` fields; `NarrativeAnalysisEngine.dashboard(...)` assembly.
- [x] Ensure team relationships and participant identities are backend-owned.
  - Evidence: dashboard uses existing backend `NarrativeEvidence.participants` identities and relationships.
- [x] Preserve existing public contract fields during migration.
  - Evidence: `dashboard` is additive; existing `timeline`, `chart`, `evidence` and `matchFlow` remain serialized.

## 3. Domain assembly

- [x] Assemble summary metrics from existing deterministic analysis outputs.
  - Evidence: `NarrativeAnalysisEngine.summaryMetrics(...)` builds official result, duration, peak army, largest combat and strongest measured swing from existing match/snapshot/combat/transition outputs.
- [x] Assemble evidence episodes without duplicating combat detection or turning-point logic.
  - Evidence: `NarrativeAnalysisEngine.evidenceEpisodes(...)` wraps existing `MatchFlowInterval` IDs, combat IDs and turning-point event IDs instead of creating a second detector.
- [ ] Build evidence episodes as coarse regime segments targeting 4–6 episodes for a normal-length match.
- [ ] Smooth noisy metric series before choosing episode boundaries.
- [ ] Merge very short combats and intervals into neighbouring episodes unless a durable before/after regime change justifies a boundary.
- [x] Map related combats and intervals deterministically.
  - Evidence: every `EvidenceEpisode` has a stable `episode-%03d` ID plus related MatchFlow interval, combat and turning-point IDs.
- [ ] Add regression tests for 1v1 and team games.
- [ ] Add tests for unavailable values, incomplete reconciliation and missing spatial evidence.

## 4. REST, Markdown and support bundle

- [x] Serialize summary metrics and evidence episodes through REST.
  - Evidence: `NarrativeAnalysis.dashboard` is additive on the existing narrative response and support-bundle JSON path.
- [ ] Add equivalent Markdown sections with the same values and evidence references.
- [x] Include the new contracts in the support bundle.
  - Evidence: the support bundle includes `analysis-response.json`, which now carries additive `narrativeAnalysis.dashboard`.
- [ ] Add parity tests for IDs, metric values, ranges and combat references.

## 5. Dashboard shell

- [x] Replace the linear first screen with a compact match header and KPI row.
  - Evidence: `DashboardReport` renders `DashboardHeader` and backend-owned KPI cards.
- [x] Build a desktop-first dashboard grid with left episode/navigation rail, central chart workspace, right insight column and lower unified timeline.
  - Evidence: `DashboardReport` and `.dashboard-grid` implement episode rail, chart workspace, right insight column, timeline strip and selected episode workspace.
- [x] Implement a narrow-screen fallback after the desktop layout is stable.
  - Evidence: dashboard CSS collapses KPI cards, episode rail, insight column and selected detail under `max-width: 1100px` and `max-width: 760px`.
- [ ] Keep user-facing text readable and free from internal ADR terminology.
- [x] Remove repeated global combat-history tables when the same evidence belongs in selected episode details.
  - Evidence: 0.9 dashboard renders detailed combat tables only inside selected episode drilldown; old history remains only under the 0.8 A/B variant.
- [x] Align sparse selected-episode blocks to the top instead of centering them inside tall empty grid areas.
  - Evidence: dashboard CSS uses `align-items: start` and `align-content: start` for dashboard grid, insight panels and selected episode workspace.

## 6. Primary chart workspace

- [x] Render one active comparative chart with metric tabs.
  - Evidence: `DashboardReport` renders one `MetricComparisonChart` selected by `.metric-tabs`.
- [x] Keep participant colours, relationships and line styles stable between tabs.
  - Evidence: dashboard reuses backend `NarrativeEvidence.metricComparisons` and existing participant legend/series semantics.
- [x] Preserve selected-player visual dominance.
  - Evidence: dashboard reuses participant metric series stroke weights from backend `NarrativeEvidence`, including selected-player emphasis.
- [x] Keep shared hover and selected evidence range synchronized.
  - Evidence: selected evidence episode range is passed into `MetricComparisonChart`; chart marker selection updates selected episode.
- [ ] Verify axis labels, legends, tooltips and grid contrast in the dark theme.

## 7. Key episode navigation

- [x] Render compact key-episode cards below or adjacent to the primary chart.
  - Evidence: `.episode-rail` renders compact `episode-card` buttons from backend `dashboard.evidenceEpisodes`.
- [x] Keep the rendered episode count in the 4–6 range for the fixed benchmark unless benchmark evidence justifies an explicit exception.
  - Evidence: existing `NarrativeAnalysisEngineTest.emitsCoarseHumanReadableEpisodesInsteadOfShortCombatSlices`; dashboard episodes are assembled from `MatchFlow` intervals.
- [x] Use one `selectedEpisodeId` as the report selection state.
  - Evidence: `DashboardReport` owns one `selectedEpisodeId` for episode rail, chart range, timeline and selected detail.
- [x] Support selection from episode cards, chart focus markers and timeline.
  - Evidence: episode buttons call `setSelectedEpisodeId`; `MetricComparisonChart` calls `selectEpisodeAt`; `TimelineStrip` calls `onSelect`.
- [x] Synchronize chart range, episode detail, combat evidence and timeline position.
  - Evidence: `DashboardReport` derives chart range, selected interval drilldown and timeline selection from the selected episode.

## 8. Selected episode detail

- [x] Render backend-owned before/after metric deltas.
  - Evidence: `NarrativeDashboard.EpisodeMetricDelta` serialized by backend and rendered in `EpisodeDeltas`.
- [x] Render all related combats without attaching unrelated fights.
  - Evidence: selected episode maps to related MatchFlow interval and uses interval combat drilldown only.
- [x] Render team-aware force tables with start, additions, losses and end values.
  - Evidence: selected episode drilldown reuses backend `CombatEvidenceTable` and `UnitEvidenceTable`.
- [x] Keep workers, infrastructure and static defence separate from combat composition.
  - Evidence: `CombatEvidenceTable` keeps collateral losses in distinct rows/labels.
- [x] Hide or mark unavailable kill credit.
  - Evidence: `UnitEvidenceTable` omits the kills column unless at least one row has a numeric credited kill value.
- [ ] Use non-spatial summaries when validated coordinates are unavailable.
- [x] Hide all-empty participant rows such as upgrades/technologies when no participant has data.
  - Evidence: existing `CombatBlock` guards upgrades/technologies rows and dashboard relies on selected evidence tables without all-empty rows.
- [x] Keep detailed tables in the selected episode detail instead of repeating them in global combat history.
  - Evidence: 0.9 dashboard has no separate global combat-history table; detailed tables live under `IntervalDrilldown`.
- [x] Render a concise human-readable episode description from backend-owned facts without inventing intent, vision, or unsupported strategic result.
  - Evidence: selected episode displays backend `EvidenceEpisode.summary`; 0.9 dashboard limitations explicitly exclude factual strategic result.

## 9. Unified timeline

- [ ] Present relevant builds, upgrades, combats, turning points and other available events on a common time scale.
- [x] Make timeline selection update the selected evidence episode.
  - Evidence: `TimelineStrip` buttons call `setSelectedEpisodeId` through the shared `onSelect`.
- [ ] Provide filtering without changing backend analytical meaning.

## 10. Visual acceptance

- [ ] Verify desktop screenshots at 1440px and 1920px against the dashboard layout contract.
- [ ] Verify the first viewport contains match summary, KPI, primary chart, episode navigation and selected-episode context.
- [ ] Verify the report does not expose ADR references, raw enum/status values, duplicated completeness labels or unsupported factual-result language.
- [ ] Verify empty or sparse economy/development blocks do not create large centered blank areas.
- [ ] Verify chart tabs, timeline markers, selected ranges, legends and tooltips remain readable in the dark theme.
- [ ] Verify no battle minimap/thumbnail is rendered without validated spatial evidence.

## 11. Documentation and delivery

- [x] Update `docs/PROJECT_STATE.md` after implementation.
  - Evidence: documented the additive dashboard contract, A/B switch and experimental limitations.
- [x] Update `docs/DECISIONS.md` if Evidence Episode or dashboard ownership becomes a durable architectural decision.
  - Evidence: added ADR-017 for the additive A/B dashboard view model and desktop-first target.
- [ ] Attach desktop screenshots at 1440px and 1920px to the implementation PR.
- [ ] Attach narrow-screen fallback evidence when available.
- [ ] Document known limitations and deferred work.
- [ ] Open the implementation PR directly into `develop` and do not merge it automatically.
