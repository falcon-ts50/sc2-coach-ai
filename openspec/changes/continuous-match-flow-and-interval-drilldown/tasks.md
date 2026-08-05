# Tasks: Continuous Match Flow and Interval Drilldown

## 1. Review and input audit

- [ ] Complete the Read Gate from `openspec/AGENTS.md`.
- [ ] Inspect current Narrative Analysis intervals, Narrative Evidence graphs, Combat history, Markdown and support-bundle serializers.
- [ ] Confirm fixed benchmark replay/support bundle and current `dragonDriver` ACTUAL values.
- [ ] Identify which non-combat evidence is currently available from `MatchContext`, timeline events and transcript-derived data.
- [ ] Document unavailable data explicitly: exact intent, full vision, physical participation of additions, killer-unit identity where still absent.

## 2. Continuous match-flow domain model

- [ ] Add backend-owned continuous `MatchFlowInterval` or equivalent contract.
- [ ] Define interval kind taxonomy for combat and non-combat states.
- [ ] Ensure serialized intervals cover match start to match end without gaps.
- [ ] Carry confidence/completeness and limitations on every interval.
- [ ] Add deterministic ordering and stable interval IDs.
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
