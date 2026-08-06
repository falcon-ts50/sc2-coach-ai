# Tasks — версия 0.9.0

## 1. Read gate

- [ ] Read `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md` and `ARCHITECTURE.md`.
- [ ] Read all OpenSpec changes already merged into `develop`.
- [ ] Confirm the fixed benchmark support bundle and record its identity.
- [ ] Capture current desktop screenshots at 1440px and 1920px before implementation.
- [ ] Capture a narrow-screen screenshot as fallback evidence, without treating mobile as the primary layout target.

## 2. Contract design

- [ ] Add additive backend contracts for match summary metrics.
- [ ] Add an evidence episode view model with stable IDs and references to existing MatchFlow, turning-point and combat entities.
- [ ] Define metric delta, evidence range, importance, completeness and limitation fields.
- [ ] Ensure team relationships and participant identities are backend-owned.
- [ ] Preserve existing public contract fields during migration.

## 3. Domain assembly

- [ ] Assemble summary metrics from existing deterministic analysis outputs.
- [ ] Assemble evidence episodes without duplicating combat detection or turning-point logic.
- [ ] Build evidence episodes as coarse regime segments targeting 4–6 episodes for a normal-length match.
- [ ] Smooth noisy metric series before choosing episode boundaries.
- [ ] Merge very short combats and intervals into neighbouring episodes unless a durable before/after regime change justifies a boundary.
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
- [ ] Build a desktop-first dashboard grid with left episode/navigation rail, central chart workspace, right insight column and lower unified timeline.
- [ ] Implement a narrow-screen fallback after the desktop layout is stable.
- [ ] Keep user-facing text readable and free from internal ADR terminology.
- [ ] Remove repeated global combat-history tables when the same evidence belongs in selected episode details.
- [ ] Align sparse selected-episode blocks to the top instead of centering them inside tall empty grid areas.

## 6. Primary chart workspace

- [ ] Render one active comparative chart with metric tabs.
- [ ] Keep participant colours, relationships and line styles stable between tabs.
- [ ] Preserve selected-player visual dominance.
- [ ] Keep shared hover and selected evidence range synchronized.
- [ ] Verify axis labels, legends, tooltips and grid contrast in the dark theme.

## 7. Key episode navigation

- [ ] Render compact key-episode cards below or adjacent to the primary chart.
- [ ] Keep the rendered episode count in the 4–6 range for the fixed benchmark unless benchmark evidence justifies an explicit exception.
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
- [ ] Hide all-empty participant rows such as upgrades/technologies when no participant has data.
- [ ] Keep detailed tables in the selected episode detail instead of repeating them in global combat history.
- [ ] Render a concise human-readable episode description from backend-owned facts without inventing intent, vision, or unsupported strategic result.

## 9. Unified timeline

- [ ] Present relevant builds, upgrades, combats, turning points and other available events on a common time scale.
- [ ] Make timeline selection update the selected evidence episode.
- [ ] Provide filtering without changing backend analytical meaning.

## 10. Visual acceptance

- [ ] Verify desktop screenshots at 1440px and 1920px against the dashboard layout contract.
- [ ] Verify the first viewport contains match summary, KPI, primary chart, episode navigation and selected-episode context.
- [ ] Verify the report does not expose ADR references, raw enum/status values, duplicated completeness labels or unsupported factual-result language.
- [ ] Verify empty or sparse economy/development blocks do not create large centered blank areas.
- [ ] Verify chart tabs, timeline markers, selected ranges, legends and tooltips remain readable in the dark theme.
- [ ] Verify no battle minimap/thumbnail is rendered without validated spatial evidence.

## 11. Documentation and delivery

- [ ] Update `docs/PROJECT_STATE.md` after implementation.
- [ ] Update `docs/DECISIONS.md` if Evidence Episode or dashboard ownership becomes a durable architectural decision.
- [ ] Attach desktop screenshots at 1440px and 1920px to the implementation PR.
- [ ] Attach narrow-screen fallback evidence when available.
- [ ] Document known limitations and deferred work.
- [ ] Open the implementation PR directly into `develop` and do not merge it automatically.
