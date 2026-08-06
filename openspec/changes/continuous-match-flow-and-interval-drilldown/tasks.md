# Tasks: Continuous Match Flow and Interval Drilldown

## 1. Review and input audit

- [x] Complete the Read Gate from `openspec/AGENTS.md`.
  - Evidence: read `openspec/project.md`, `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md`, `ARCHITECTURE.md`, the full `openspec/changes/continuous-match-flow-and-interval-drilldown/` packet, open PR state, and relevant Narrative/Combat/frontend/Markdown files.
  - Active change: `continuous-match-flow-and-interval-drilldown`; base branch and intended PR target: `develop`.
  - Objective: refine the REVIEW packet so APPLY has a concrete backend-owned continuous match-flow contract, no-gap rules and benchmark acceptance expectations.
  - In scope for this refinement: OpenSpec proposal/design/spec/task clarity only.
  - Non-goals for this refinement: production Java/React/Markdown implementation, version bump, release, Docker/local full-suite validation.
  - Open PR state during refinement: no open PRs targeting `develop`.
- [ ] Inspect current Narrative Analysis intervals, Narrative Evidence graphs, Combat history, Markdown and support-bundle serializers.
- [ ] Confirm fixed benchmark replay/support bundle and current `dragonDriver` ACTUAL values.
- [ ] Identify which non-combat evidence is currently available from `MatchContext`, timeline events and transcript-derived data.
- [ ] Document unavailable data explicitly: exact intent, full vision, physical participation of additions, killer-unit identity where still absent.

### REVIEW refinement notes

- Current Narrative phases are not a no-gap partition: `NarrativeAnalysisEngine.phases(...)` may leave time after transition windows uncovered.
- Existing fixed artifact inspection found five old Narrative phase intervals for `dragonDriver` and visible holes including 7:10-7:50, 12:40-16:00 and 21:30-match end.
- Fixed replay artifact: `/root/openclaw-artifacts/sc2-coach-ai/replays/2026-08-05/2026_08_04_TFrontdoor_ZGuardian_VS_TdragonDriver_ZLulu.SC2Replay`.
- Observed replay SHA-256 during REVIEW refinement: `9031068478141554827027ee64b16951fec123c0c90acb2ebb6de99358e11315`.
- Existing benchmark response inspected: `/root/openclaw-artifacts/sc2-coach-ai/support-bundles/2026-08-05/narrative-analysis-vertical-slice/analysis-response.json`.
- That response has match duration about 23:11 and eight detected combat episodes.
- APPLY verification must record the new ACTUAL interval count, exact interval bounds, kind distribution, combat-to-interval mapping and no-combat empty-state examples for the same artifact.

## 2. Continuous match-flow domain model

- [ ] Add backend-owned `MatchFlow` with schema version, canonical match bounds, intervals, optional overview combat IDs and limitations.
- [ ] Add backend-owned continuous `MatchFlowInterval` or equivalent contract.
- [ ] Define interval kind taxonomy for combat and non-combat states.
- [ ] Ensure serialized intervals cover match start to match end without gaps.
- [ ] Carry confidence/completeness and limitations on every interval.
- [ ] Add deterministic ordering and stable interval IDs.
- [ ] Use half-open interval semantics `[startedAt, endedAt)` except the final interval ending at match end.
- [ ] Serialize start/end metrics and deltas by `NarrativeEvidence.ParticipantIdentity` ID.
- [ ] Add domain tests for no-gap coverage, low-evidence fallback and boundary normalization.

## 3. Non-combat classification

- [ ] Classify economic growth intervals from worker/economy/supply deltas where evidence supports it.
- [ ] Classify army buildup/recovery intervals from army value and supply changes.
- [ ] Classify tech/production/scouting/preparation intervals only where current evidence supports them.
- [ ] Preserve `LOW_EVIDENCE` fallback rather than inventing a confident label.
- [ ] Add tests for combat-free economic intervals and low-evidence intervals.

## 4. Interval-to-evidence mapping

- [ ] Map detected combats to overlapping match-flow intervals.
- [ ] Add backend-owned interval drilldown contract.
- [ ] For combat intervals, include relevant `NarrativeEvidence.CombatEvidence` references.
- [ ] For intervals with multiple overlapping combats, include all combat IDs in chronological order.
- [ ] For every interval, reject unrelated combat IDs that do not overlap the interval bounds.
- [ ] For non-combat intervals, include available macro/preparation evidence.
- [ ] When no combats are present, serialize an explicit empty state.
- [ ] Add tests for intervals with one combat, multiple combats and no combats.

## 5. Strong graph focus

- [ ] Replace weak selected-phase highlighting with strong interval focus.
- [ ] Render selected interval in normal colour/opacity.
- [ ] Mute non-selected graph time ranges using grey or lower opacity.
- [ ] Keep all participants visible and identifiable in overview mode.
- [ ] Apply the same selected interval to army, economy and supply graphs.
- [ ] Add frontend tests or component-level assertions for selected/unselected state classes.

## 6. Interval drilldown UI

- [ ] Show drilldown below the graph area for the selected interval.
- [ ] Show combat evidence only for combats inside the selected interval.
- [ ] Show `боёв в этом интервале не обнаружено` or equivalent when empty.
- [ ] Show macro/preparation evidence for non-combat intervals where available.
- [ ] Decide whether no-selection mode shows all combats or hides drilldown; document the chosen behaviour.
- [ ] Keep mobile layout readable.

## 7. Combat evidence redesign

- [ ] Prototype a row-based `start / additions / losses / end / kills` combat table.
- [ ] Show side/team total before participant detail.
- [ ] Keep participant attribution visible under each side.
- [ ] Keep worker, structure and static-defence losses separate.
- [ ] Preserve reconciliation and unknown kill-credit semantics.
- [ ] Discuss and finalize the visual shape before APPLY if the prototype is not obviously good.

## 8. Markdown and support-bundle parity

- [ ] Add continuous match-flow intervals to Markdown output.
- [ ] Add selected-interval/drilldown semantics to support bundle JSON.
- [ ] Include no-combat empty states and limitations in Markdown/support bundle.
- [ ] Add parity tests using the backend-owned interval model.

## 9. Benchmark acceptance

- [ ] Run or inspect the fixed benchmark replay/support bundle from `dragonDriver` perspective.
- [ ] Record ACTUAL interval count, interval coverage, first/last timestamps and category distribution.
- [ ] Verify no temporal gaps.
- [ ] Verify all four benchmark participants remain on primary graphs where data exists.
- [ ] Verify selected interval focus strongly mutes non-selected time.
- [ ] Verify interval drilldown shows the correct combats or no-combat empty state.
- [ ] Verify non-combat economic/preparation intervals show available evidence rather than fight cards.
- [ ] Verify combat table readability for the fixed benchmark's first multi-participant fight.

## 10. Documentation and release

- [ ] Update `docs/PROJECT_STATE.md` with completed behaviour and confirmed limitations.
- [ ] Update `docs/DECISIONS.md` if interval taxonomy, focus semantics or combat table semantics become durable architecture.
- [ ] Open PR directly to `develop`.
- [ ] Use GitHub Actions as authoritative heavy validation.
- [ ] If releasing to production, bump version in a separate release step and verify `https://nukle.nexus/api/v1/build`.
