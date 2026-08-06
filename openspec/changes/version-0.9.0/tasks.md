# Tasks — версия 0.9.0

## 1. Read gate

- [ ] Read `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md` and `ARCHITECTURE.md`.
- [ ] Read all OpenSpec changes already merged into `develop`.
- [ ] Confirm the fixed benchmark support bundle and record its identity.
- [ ] Capture current desktop and mobile screenshots before implementation.

## 2. Contract design

- [ ] Add additive backend contracts for match summary metrics.
- [ ] Add an evidence episode view model with stable IDs and references to existing MatchFlow, turning-point and combat entities.
- [ ] Define metric delta, evidence range, importance, completeness and limitation fields.
- [ ] Ensure team relationships and participant identities are backend-owned.
- [ ] Preserve existing public contract fields during migration.

## 3. Domain assembly

- [ ] Assemble summary metrics from existing deterministic analysis outputs.
- [ ] Assemble evidence episodes without duplicating combat detection or turning-point logic.
- [ ] Map related combats and intervals deterministically.
- [ ] Add regression tests for 1v1 and team games.
- [ ] Add tests for unavailable values, incomplete reconciliation and missing spatial evidence.

## 4. REST, Markdown and support bundle

- [ ] Serialize summary metrics and evidence episodes through REST.
- [ ] Add equivalent Markdown sections with the same values and evidence references.
- [ ] Include the new contracts in the support bundle.
- [ ] Add parity tests for IDs, metric values, ranges and combat references.

## 5. Dashboard shell

- [ ] Replace the linear first screen with a compact match header and KPI row.
- [ ] Build a desktop dashboard grid without removing existing fallback sections prematurely.
- [ ] Implement responsive tablet and mobile layouts.
- [ ] Keep user-facing text readable and free from internal ADR terminology.

## 6. Primary chart workspace

- [ ] Render one active comparative chart with metric tabs.
- [ ] Keep participant colours, relationships and line styles stable between tabs.
- [ ] Preserve selected-player visual dominance.
- [ ] Keep shared hover and selected evidence range synchronized.
- [ ] Verify axis labels, legends, tooltips and grid contrast in the dark theme.

## 7. Key episode navigation

- [ ] Render compact key-episode cards below or adjacent to the primary chart.
- [ ] Use one `selectedEpisodeId` as the report selection state.
- [ ] Support selection from episode cards, chart focus markers and timeline.
- [ ] Synchronize chart range, episode detail, combat evidence and timeline position.

## 8. Selected episode detail

- [ ] Render backend-owned before/after metric deltas.
- [ ] Render all related combats without attaching unrelated fights.
- [ ] Render team-aware force tables with start, additions, losses and end values.
- [ ] Keep workers, infrastructure and static defence separate from combat composition.
- [ ] Hide or mark unavailable kill credit.
- [ ] Use non-spatial summaries when validated coordinates are unavailable.

## 9. Unified timeline

- [ ] Present relevant builds, upgrades, combats, turning points and other available events on a common time scale.
- [ ] Make timeline selection update the selected evidence episode.
- [ ] Provide filtering without changing backend analytical meaning.

## 10. Validation

- [ ] Run all backend tests.
- [ ] Run all frontend tests and production build.
- [ ] Run OpenSpec structural validation.
- [ ] Validate REST, React, Markdown and support-bundle parity.
- [ ] Validate the unchanged fixed benchmark support bundle.
- [ ] Perform desktop visual acceptance at representative wide resolutions.
- [ ] Perform mobile visual acceptance down to 320 CSS pixels.
- [ ] Confirm that official result, strategic state, kill credit and spatial evidence are not overstated.

## 11. Documentation and delivery

- [ ] Update `docs/PROJECT_STATE.md` after implementation.
- [ ] Update `docs/DECISIONS.md` if Evidence Episode or dashboard ownership becomes a durable architectural decision.
- [ ] Attach desktop and mobile screenshots to the implementation PR.
- [ ] Document known limitations and deferred work.
- [ ] Open the implementation PR directly into `develop` and do not merge it automatically.
