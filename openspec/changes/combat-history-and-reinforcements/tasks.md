# Tasks — Combat History and Reinforcement Accounting

## Gate

Lifecycle gate: `APPLY`.

Before implementation, complete the Read Gate from `openspec/AGENTS.md`. Work from current `develop`; target `develop` directly.

## 1. Repository and replay evidence

- [x] 1.1 Identify the current production combat pipeline, REST contracts, React components and Markdown renderer.
  - Evidence: `AnalysisService` wires `CombatEngine.detect(...)` into `AnalysisResponse.combats`; `frontend/src/main.jsx` renders the report and downloads Markdown/support bundle via `frontend/src/reporting.js`.
- [x] 1.2 Inspect the fixed validation replay/support bundle and record every detected engagement in chronological order.
  - Evidence: fixed replay `/root/openclaw-artifacts/sc2-coach-ai/replays/2026-08-05/2026_08_04_TFrontdoor_ZGuardian_VS_TdragonDriver_ZLulu.SC2Replay`; generated API/support-bundle data under `/root/openclaw-artifacts/sc2-coach-ai/support-bundles/2026-08-05/combat-history-reinforcements/`.
  - Detected fights for focus `Lulu`: 8 entries, `Бой 1` through `Бой 8`, ordered from `4:55-5:45` to `10:10-11:00`.
- [x] 1.3 For the Lulu example, identify the lifecycle events that explain `2 Zergling -> 3 lost -> 15 remaining`.
  - Evidence: `analysis-response.json` combat `combat-02-373-423-frontdoor-lulu-dragondriver-guardian`, Lulu participant: `armyBefore.Zergling=2`, `additions.Zergling=16`, `unitsLost.Zergling=3`, `armyAfter.Zergling=15`, `reconciliationStatus=EXACT`.
- [x] 1.4 Record missing or contradictory lifecycle evidence rather than forcing reconciliation.
  - Evidence: `Combat.ReconciliationStatus.PARTIAL` and `Combat.ReconciliationIssue`; regression `CombatEngineHistoryTest.marksIncompleteLifecycleEvidenceWhenTheTransitionDoesNotReconcile`.

Acceptance: the PR contains evidence references to production symbols, tests and the fixed replay observations.

## 2. Domain and accounting model

- [x] 2.1 Add or extend domain contracts for chronological combat history.
  - Evidence: `Combat.id`, `Combat.ordinalLabel`, `Combat.startedAt`, `Combat.endedAt`.
- [x] 2.2 Add explicit per-participant additions/reinforcements during the combat interval.
  - Evidence: `Combat.Participant.additions`; `CombatEngine.additionsDuring(...)`.
- [x] 2.3 Preserve separate combat-unit, worker, infrastructure and static-defence losses.
  - Evidence: existing `unitsLost`, `workersLost`, `structuresLost`, `staticDefenseLost` remain separate in domain, REST, UI and Markdown.
- [x] 2.4 Represent transformations, ownership changes or unresolved reconciliation explicitly when relevant.
  - Evidence: `Combat.ReconciliationStatus` and `Combat.ReconciliationIssue`; exact unsupported transformations are not fabricated.
- [x] 2.5 Enforce deterministic ordering and stable engagement identity.
  - Evidence: sorted combat detection plus `withStableIdentity(...)` generates `combat-%02d-start-end-participants` ids and `Бой N` labels.
- [x] 2.6 Avoid winner, efficiency and opaque power-score requirements.
  - Evidence: this change does not add any new winner, efficiency or power-score field; browser/Markdown combat history no longer narrates a required winner.

Acceptance: for every unit type the report can explain the start-to-end transition or mark it incomplete.

## 3. Production integration

- [x] 3.1 Wire the new accounting through the production combat analysis flow.
  - Evidence: `AnalysisService` returns `CombatEngine.detect(...)` results; `CombatEngine` now computes `additions`, stable ids and reconciliation.
- [x] 3.2 Extend serialized/REST contracts with backward-compatible defaults or versioning.
  - Evidence: `Combat` compact constructors keep old call sites valid; `AnalysisControllerTest` asserts `id`, `ordinalLabel`, `additions` and `reconciliationStatus` in JSON.
- [x] 3.3 Add the chronological history and reinforcement fields to support bundles.
  - Evidence: support bundle `analysis-response.json` contains `combats[].id`, `ordinalLabel`, `participants[].additions`, `reconciliationStatus` and `reconciliationIssues`; `report.md` contains the same history.
- [x] 3.4 Update Markdown output with the same semantics as the browser report.
  - Evidence: `frontend/src/reporting.js` is used by both Markdown download and support-bundle `report.md`.

Acceptance: browser, Markdown and support bundle describe the same engagements in the same order.

## 4. Browser UI

- [x] 4.1 Add a visible section titled `История боёв`.
  - Evidence: `frontend/src/main.jsx`.
- [x] 4.2 Show every detected engagement as a chronological item/card with start/end time or a clear timestamp.
  - Evidence: `CombatBlock` renders `startedAt-endedAt`; fixed replay API produced 8 combat entries.
- [x] 4.3 Give each fight a stable ordinal label such as `Бой 1`, `Бой 2`, etc.
  - Evidence: `Combat.ordinalLabel`; `frontend/src/main.jsx`.
- [x] 4.4 For each participant show:
  - army at start;
  - reinforcements/additions during fight;
  - combat losses;
  - worker losses;
  - building losses;
  - static-defence losses;
  - army at end;
  - army-value transition when available.
  - Evidence: `CombatBlock` renders these rows, and `frontend/src/reporting.js` emits the same rows in Markdown/support-bundle `report.md`.
- [x] 4.5 On mobile, keep labels and values readable without horizontal scrolling or clipped cards.
  - Evidence: CSS uses `minmax(0, 1fr)`, wrapping values and single-column participant rows at narrow widths. Browser runtime verification was skipped at Uran's request because root Chromium requires `--no-sandbox` in this OpenClaw environment.
- [x] 4.6 Do not show `нет данных` for an empty category when the system knows the value is zero; use a consistent `нет`/empty-state distinction. Reserve `нет данных` for genuinely unavailable evidence.
  - Evidence: `frontend/src/reporting.js` `composition(...)` returns `нет` for known empty values and supports explicit unavailable state; covered by `frontend/src/reporting.test.js`.

Acceptance: the fixed replay can be read as a coherent sequence of fights on an iPhone-sized viewport.

## 5. Automated verification

- [x] 5.1 Add domain unit tests for additions, losses and end-state reconciliation.
  - Evidence: `CombatEngineHistoryTest.exposesAdditionsWhenLossesExceedTheStartSnapshotButReconcile`.
- [x] 5.2 Add tests for units produced during a fight where losses exceed the start snapshot.
  - Evidence: same test covers `2 Zergling + 16 - 3 = 15`.
- [x] 5.3 Add tests for incomplete lifecycle evidence and explicit degradation.
  - Evidence: `CombatEngineHistoryTest.marksIncompleteLifecycleEvidenceWhenTheTransitionDoesNotReconcile`.
- [x] 5.4 Add team-game attribution tests.
  - Evidence: `CombatEngineHistoryTest.keepsTeamGameParticipantsAndVictimOwnerLossesSeparate`.
- [x] 5.5 Add REST/serialization tests.
  - Evidence: `AnalysisControllerTest` asserts new JSON fields.
- [x] 5.6 Add frontend tests for combat history ordering and reinforcement rendering.
  - Evidence: `frontend/src/reporting.test.js`.
- [x] 5.7 Run Python, Java and frontend validation required by the repository.
  - Evidence: `python3 -m pytest -q` passed 42 tests; Docker/JDK25 `mvn verify` passed 55 Java tests; `npm test` passed 3 tests; `npm run build` passed.

## 6. Documentation and completion

- [x] 6.1 Update `docs/PROJECT_STATE.md` with factual completion/limitations.
  - Evidence: `docs/PROJECT_STATE.md`.
- [x] 6.2 Update `ARCHITECTURE.md` or `docs/DECISIONS.md` only if contracts or ownership materially change.
  - Evidence: `docs/DECISIONS.md` ADR-012 records addition/reconciliation semantics.
- [x] 6.3 Update this task file with actual observed values from the fixed replay under the tester section below.
  - Evidence: tester handoff below.
- [x] 6.4 Open a PR to `develop` and include the fixed-replay verification evidence.
  - Evidence: PR #73 `https://github.com/falcon-ts50/sc2-coach-ai/pull/73` targets `develop` directly and includes the fixed replay/support-bundle verification notes.

---

# Expected result on the website — tester handoff

This section is part of the task contract. The implementation agent MUST update placeholders with actual values from the fixed validation replay before requesting review. The tester agent shall use this same section as its primary acceptance script.

## Test input

- Replay/support bundle: the same fixed replay currently used for all manual tests.
- Focus participant/example: Lulu fight previously showing `2 × Zergling` before, `3 × Zergling` lost, `15 × Zergling` after.
- Deployed version/commit under test: local APPLY image `0.8.0-SNAPSHOT`, implementation commit `3ec8fcd2816c` (`Add combat history reinforcement accounting`). The replay/API/support-bundle smoke run was performed from the same implementation tree before this task-handoff commit.

## Expected top-level site behaviour

1. A visible section named **`История боёв`** exists in the generated report.
2. The section contains **8** chronological fight entries for the fixed replay.
3. Entries are ordered by game time from earliest to latest.
4. Every entry has a stable label (`Бой 1`, `Бой 2`, ...), timestamp/time range and participant cards.
5. Reloading/rebuilding the report for the same player does not reorder or duplicate fights.
6. Mobile layout fits the viewport; no required field is hidden behind horizontal scrolling.

## Expected participant-card behaviour

For each participant in each fight, the card displays distinct rows for:

- `Армия в начале`;
- `Подкрепления во время боя` or an equally precise approved label;
- `Боевые потери`;
- `Рабочие`;
- `Здания`;
- `Оборона`;
- `Армия в конце`;
- `Стоимость армии`, when available.

The labels must not imply that every unit produced during the interval physically reached the battlefield unless local participation is supported by evidence.

## Expected Lulu reconciliation

For the identified Lulu fight, the page must no longer present the transition as an unexplained contradiction.

The implementer must fill the exact replay-derived values here:

- fight label/time: `Бой 2`, `6:13-7:03` (`startedAt=PT6M13.12S`, `endedAt=PT7M3.12S`), id `combat-02-373-423-frontdoor-lulu-dragondriver-guardian`;
- army in beginning: `4 x Overlord, 3 x Queen, 2 x Zergling`;
- additions/reinforcements during fight: `16 x Zergling`;
- combat losses: `3 x Zergling`;
- workers lost: `1 x Drone`;
- buildings lost: `нет`;
- static defence lost: `нет`;
- army in end: `15 x Zergling, 4 x Overlord, 3 x Queen`;
- reconciliation status: `exact`;
- evidence source:
  - replay: `/root/openclaw-artifacts/sc2-coach-ai/replays/2026-08-05/2026_08_04_TFrontdoor_ZGuardian_VS_TdragonDriver_ZLulu.SC2Replay`;
  - support bundle directory: `/root/openclaw-artifacts/sc2-coach-ai/support-bundles/2026-08-05/combat-history-reinforcements/`;
  - structured API: `analysis-response.json`, combat id `combat-02-373-423-frontdoor-lulu-dragondriver-guardian`, participant `Lulu`;
  - Markdown: `report.md`, section `### Бой 2 · 6:13-7:03`;
  - bundle zip: `support-bundle-lulu.zip`;
  - replay SHA-256: `replay-sha256.txt`.

At minimum, the visible report must explain how a participant can lose 3 Zerglings despite starting with 2, or explicitly state that replay lifecycle data is insufficient. Silent contradiction is a failure.

## Empty versus unavailable data

Tester checks:

- a known zero loss displays `нет` or an equivalent zero state;
- unavailable evidence displays `нет данных`/`неполные данные`;
- the two states are not conflated;
- an empty reinforcement list does not cause the whole participant card to disappear.

## Cross-output consistency

For the fixed replay:

- browser fight count equals Markdown fight count;
- ordering and timestamps match;
- participant names match;
- additions and categorized losses match;
- support bundle contains sufficient structured data to reproduce the displayed values.

## Regression expectations

- player-perspective report rebuilding still works;
- team attribution remains correct;
- workers/buildings/static defence are not mixed into army composition;
- no new combat winner or efficiency score is introduced;
- report generation and Markdown download still succeed.

## Tester result format

The tester agent should return:

- deployed version and commit;
- replay identifier;
- PASS/FAIL for every numbered expectation above;
- screenshots for the full `История боёв` section and Lulu participant card;
- browser values compared with support-bundle/Markdown values;
- exact mismatch descriptions, without proposing code changes unless asked.
