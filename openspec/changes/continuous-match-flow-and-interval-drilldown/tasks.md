# Tasks: Continuous Match Flow and Interval Drilldown

## 1. Review and input audit

- [x] Complete the Read Gate from `openspec/AGENTS.md`.
  - Evidence: read `openspec/project.md`, `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md`, `ARCHITECTURE.md`, the full `openspec/changes/continuous-match-flow-and-interval-drilldown/` packet, open PR state, and relevant Narrative/Combat/frontend/Markdown files.
  - Active change: `continuous-match-flow-and-interval-drilldown`; base branch and intended PR target: `develop`.
  - Objective: refine the REVIEW packet so APPLY has a concrete backend-owned continuous match-flow contract, no-gap rules and benchmark acceptance expectations.
  - In scope for this refinement: OpenSpec proposal/design/spec/task clarity only.
  - Non-goals for this refinement: production Java/React/Markdown implementation, version bump, release, Docker/local full-suite validation.
  - Open PR state during refinement: no open PRs targeting `develop`.
- [x] Inspect current Narrative Analysis intervals, Narrative Evidence graphs, Combat history, Markdown and support-bundle serializers.
  - Evidence: reviewed `NarrativeAnalysisEngine`, `NarrativeAnalysis`, frontend `main.jsx`, `reporting.js`, `reporting.test.js`, and combat-history related tests before production edits.
- [ ] Confirm fixed benchmark replay/support bundle and current `dragonDriver` ACTUAL values.
- [x] Identify which non-combat evidence is currently available from `MatchContext`, timeline events and transcript-derived data.
  - Evidence: APPLY uses macro frame metrics, known combat additions, combat snapshot upgrades/technologies and derived preparation signals; transcript/vision-derived scouting remains unavailable to this implementation.
- [x] Document unavailable data explicitly: exact intent, full vision, physical participation of additions, killer-unit identity where still absent.
  - Evidence: interval limitations, ADR-016 and `docs/PROJECT_STATE.md` document the unavailable claims.

### REVIEW refinement notes

- Current Narrative phases are not a no-gap partition: `NarrativeAnalysisEngine.phases(...)` may leave time after transition windows uncovered.
- Existing fixed artifact inspection found five old Narrative phase intervals for `dragonDriver` and visible holes including 7:10-7:50, 12:40-16:00 and 21:30-match end.
- Fixed replay artifact: `/root/openclaw-artifacts/sc2-coach-ai/replays/2026-08-05/2026_08_04_TFrontdoor_ZGuardian_VS_TdragonDriver_ZLulu.SC2Replay`.
- Observed replay SHA-256 during REVIEW refinement: `9031068478141554827027ee64b16951fec123c0c90acb2ebb6de99358e11315`.
- Existing benchmark response inspected: `/root/openclaw-artifacts/sc2-coach-ai/support-bundles/2026-08-05/narrative-analysis-vertical-slice/analysis-response.json`.
- That response has match duration about 23:11 and eight detected combat episodes.
- APPLY verification must record the new ACTUAL interval count, exact interval bounds, kind distribution, combat-to-interval mapping and no-combat empty-state examples for the same artifact.

## 2. Continuous match-flow domain model

- [x] Add backend-owned `MatchFlow` with schema version, canonical match bounds, intervals, optional overview combat IDs and limitations.
- [x] Add backend-owned continuous `MatchFlowInterval` or equivalent contract.
- [x] Define interval kind taxonomy for combat and non-combat states.
- [x] Ensure serialized intervals cover match start to match end without gaps.
- [x] Carry confidence/completeness and limitations on every interval.
- [x] Add deterministic ordering and stable interval IDs.
- [x] Use half-open interval semantics `[startedAt, endedAt)` except the final interval ending at match end.
- [x] Serialize start/end metrics and deltas by `NarrativeEvidence.ParticipantIdentity` ID.
- [x] Add domain tests for no-gap coverage, low-evidence fallback and boundary normalization.
  - Evidence: `MatchFlow.java`, `NarrativeAnalysisEngine.matchFlow(...)`, `NarrativeAnalysisEngineTest.emitsContinuousMatchFlowWithoutTemporalGaps`.
- [x] Replace raw event-boundary slicing with coarse episode segmentation using smoothed all-participant metric features.
  - Evidence: `NarrativeAnalysisEngine.coarseEpisodeBoundaries(...)` uses smoothed level/slope/integral features and dynamic-programming piecewise-linear cost, with 4-6 target episodes and minimum readable duration constraints.

## 3. Non-combat classification

- [x] Classify economic growth intervals from worker/economy/supply deltas where evidence supports it.
- [x] Classify army buildup/recovery intervals from army value and supply changes.
- [x] Classify tech/production/scouting/preparation intervals only where current evidence supports them.
- [x] Preserve `LOW_EVIDENCE` fallback rather than inventing a confident label.
- [x] Add tests for combat-free economic intervals and low-evidence intervals.
  - Evidence: deterministic classification in `NarrativeAnalysisEngine.classifyInterval(...)` with low-evidence fallback; tests cover no-gap and no-development empty-state intervals.

## 4. Interval-to-evidence mapping

- [x] Map detected combats to overlapping match-flow intervals.
- [x] Add backend-owned interval drilldown contract.
- [x] Model interval drilldown as two independent sections: combat and development.
- [x] Ensure every interval serializes both sections, even when one or both are empty.
- [x] For combat intervals, include relevant `NarrativeEvidence.CombatEvidence` references.
- [x] For intervals with multiple overlapping combats, include all combat IDs in chronological order.
- [x] For every interval, reject unrelated combat IDs that do not overlap the interval bounds.
- [x] For every interval, include available economy, production, upgrade, tech, scouting or preparation evidence where current data supports it.
- [x] When no combats are present, serialize an explicit combat empty state.
- [x] When no development evidence is present, serialize an explicit development empty state.
- [x] Add tests for intervals with one combat, multiple combats, no combats, combat plus development evidence, and no development evidence.
  - Evidence: `IntervalDrilldown`, `CombatDrilldown`, `DevelopmentDrilldown`, `NarrativeAnalysisEngineTest.mapsCombatAndDevelopmentEvidenceToTheSameInterval`, and `NarrativeAnalysisEngineTest.serializesSeparateEmptyStatesForNoCombatAndNoDevelopmentEvidence`.

## 5. Strong graph focus

- [x] Replace weak selected-phase highlighting with strong interval focus.
- [x] Render selected interval in normal colour/opacity.
- [x] Mute non-selected graph time ranges using grey or lower opacity.
- [x] Keep all participants visible and identifiable in overview mode.
- [x] Apply the same selected interval to army, economy and supply graphs.
- [ ] Add frontend tests or component-level assertions for selected/unselected state classes.
  - Evidence: `MetricComparisonChart` renders muted full-range paths and selected clipped paths across all narrative evidence charts. Existing frontend tests cover Markdown parity only; component-level DOM assertions remain future work.

## 6. Interval drilldown UI

- [x] Show drilldown below the graph area for the selected interval.
- [x] Show separate combat and development sections for the selected interval.
- [x] Show combat evidence only for combats inside the selected interval.
- [x] Show `боёв в этом интервале не обнаружено` or equivalent in the combat section when empty.
- [x] Show macro/preparation/development evidence for any interval where available, including combat intervals.
- [x] Show `экономических/технологических событий в этом интервале не обнаружено` or equivalent in the development section when empty.
- [x] Decide whether no-selection mode shows all combats or hides drilldown; document the chosen behaviour.
- [x] Keep mobile layout readable.
  - Evidence: React renders interval drilldown only after interval selection; no-selection mode keeps the overview uncluttered and hides drilldown.

## 7. Combat evidence redesign

- [x] Prototype a row-based `start / additions / losses / end / kills` combat table.
- [x] Show side/team total before participant detail.
- [x] Keep participant attribution visible under each side.
- [x] Keep worker, structure and static-defence losses separate.
- [x] Preserve reconciliation and unknown kill-credit semantics.
- [x] Hide kill columns in user-facing tables when every row has unavailable kill attribution.
- [x] Move combat evidence tables out of global combat history and into selected interval drilldown.
- [x] Hide participant-card row types such as upgrades when every participant has no value for that row type.
- [x] Add human-readable combat summary text to selected interval drilldown.

## 8. Markdown and support-bundle parity

- [x] Add continuous match-flow intervals to Markdown output.
- [x] Add selected-interval/drilldown semantics to support bundle JSON.
- [x] Include separate combat/development sections in Markdown/support bundle.
- [x] Include no-combat and no-development-evidence empty states and limitations in Markdown/support bundle.
- [x] Add parity tests using the backend-owned interval model.
  - Evidence: `reporting.js` prints match-flow intervals and both drilldown sections; support bundle includes the same backend-owned JSON through `analysis-response.json`; `reporting.test.js` covers the Markdown output.

## 9. Benchmark acceptance

- [ ] Run or inspect the fixed benchmark replay/support bundle from `dragonDriver` perspective.
- [ ] Record ACTUAL interval count, interval coverage, first/last timestamps and category distribution.
- [ ] Verify no temporal gaps.
- [ ] Verify all four benchmark participants remain on primary graphs where data exists.
- [ ] Verify selected interval focus strongly mutes non-selected time.
- [ ] Verify interval drilldown shows the correct combats or no-combat empty state.
- [ ] Verify development sections show available economy/tech/production/scouting/preparation evidence in both combat and non-combat intervals.
- [ ] Verify intervals without development evidence show an explicit development empty state.
- [ ] Verify combat table readability for the fixed benchmark's first multi-participant fight.

## 10. Documentation and release

- [x] Update `docs/PROJECT_STATE.md` with completed behaviour and confirmed limitations.
- [x] Update `docs/DECISIONS.md` if interval taxonomy, focus semantics or combat table semantics become durable architecture.
- [ ] Open PR directly to `develop`.
- [ ] Use GitHub Actions as authoritative heavy validation.
- [ ] If releasing to production, bump version in a separate release step and verify `https://nukle.nexus/api/v1/build`.
