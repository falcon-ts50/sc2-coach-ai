# Tasks — Combat History and Reinforcement Accounting

## Gate

Lifecycle gate: `APPLY`.

Before implementation, complete the Read Gate from `openspec/AGENTS.md`. Work from current `develop`; target `develop` directly.

## 1. Repository and replay evidence

- [ ] 1.1 Identify the current production combat pipeline, REST contracts, React components and Markdown renderer.
- [ ] 1.2 Inspect the fixed validation replay/support bundle and record every detected engagement in chronological order.
- [ ] 1.3 For the Lulu example, identify the lifecycle events that explain `2 Zergling -> 3 lost -> 15 remaining`.
- [ ] 1.4 Record missing or contradictory lifecycle evidence rather than forcing reconciliation.

Acceptance: the PR contains evidence references to production symbols, tests and the fixed replay observations.

## 2. Domain and accounting model

- [ ] 2.1 Add or extend domain contracts for chronological combat history.
- [ ] 2.2 Add explicit per-participant additions/reinforcements during the combat interval.
- [ ] 2.3 Preserve separate combat-unit, worker, infrastructure and static-defence losses.
- [ ] 2.4 Represent transformations, ownership changes or unresolved reconciliation explicitly when relevant.
- [ ] 2.5 Enforce deterministic ordering and stable engagement identity.
- [ ] 2.6 Avoid winner, efficiency and opaque power-score requirements.

Acceptance: for every unit type the report can explain the start-to-end transition or mark it incomplete.

## 3. Production integration

- [ ] 3.1 Wire the new accounting through the production combat analysis flow.
- [ ] 3.2 Extend serialized/REST contracts with backward-compatible defaults or versioning.
- [ ] 3.3 Add the chronological history and reinforcement fields to support bundles.
- [ ] 3.4 Update Markdown output with the same semantics as the browser report.

Acceptance: browser, Markdown and support bundle describe the same engagements in the same order.

## 4. Browser UI

- [ ] 4.1 Add a visible section titled `История боёв`.
- [ ] 4.2 Show every detected engagement as a chronological item/card with start/end time or a clear timestamp.
- [ ] 4.3 Give each fight a stable ordinal label such as `Бой 1`, `Бой 2`, etc.
- [ ] 4.4 For each participant show:
  - army at start;
  - reinforcements/additions during fight;
  - combat losses;
  - worker losses;
  - building losses;
  - static-defence losses;
  - army at end;
  - army-value transition when available.
- [ ] 4.5 On mobile, keep labels and values readable without horizontal scrolling or clipped cards.
- [ ] 4.6 Do not show `нет данных` for an empty category when the system knows the value is zero; use a consistent `нет`/empty-state distinction. Reserve `нет данных` for genuinely unavailable evidence.

Acceptance: the fixed replay can be read as a coherent sequence of fights on an iPhone-sized viewport.

## 5. Automated verification

- [ ] 5.1 Add domain unit tests for additions, losses and end-state reconciliation.
- [ ] 5.2 Add tests for units produced during a fight where losses exceed the start snapshot.
- [ ] 5.3 Add tests for incomplete lifecycle evidence and explicit degradation.
- [ ] 5.4 Add team-game attribution tests.
- [ ] 5.5 Add REST/serialization tests.
- [ ] 5.6 Add frontend tests for combat history ordering and reinforcement rendering.
- [ ] 5.7 Run Python, Java and frontend validation required by the repository.

## 6. Documentation and completion

- [ ] 6.1 Update `docs/PROJECT_STATE.md` with factual completion/limitations.
- [ ] 6.2 Update `ARCHITECTURE.md` or `docs/DECISIONS.md` only if contracts or ownership materially change.
- [ ] 6.3 Update this task file with actual observed values from the fixed replay under the tester section below.
- [ ] 6.4 Open a PR to `develop` and include the fixed-replay verification evidence.

---

# Expected result on the website — tester handoff

This section is part of the task contract. The implementation agent MUST update placeholders with actual values from the fixed validation replay before requesting review. The tester agent shall use this same section as its primary acceptance script.

## Test input

- Replay/support bundle: the same fixed replay currently used for all manual tests.
- Focus participant/example: Lulu fight previously showing `2 × Zergling` before, `3 × Zergling` lost, `15 × Zergling` after.
- Deployed version/commit under test: **IMPLEMENTER MUST FILL**.

## Expected top-level site behaviour

1. A visible section named **`История боёв`** exists in the generated report.
2. The section contains **IMPLEMENTER MUST FILL: expected number of fights** chronological fight entries for the fixed replay.
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

- fight label/time: **IMPLEMENTER MUST FILL**;
- army in beginning: **IMPLEMENTER MUST FILL**;
- additions/reinforcements during fight: **IMPLEMENTER MUST FILL**;
- combat losses: **IMPLEMENTER MUST FILL**;
- workers lost: **IMPLEMENTER MUST FILL**;
- buildings lost: **IMPLEMENTER MUST FILL**;
- static defence lost: **IMPLEMENTER MUST FILL**;
- army in end: **IMPLEMENTER MUST FILL**;
- reconciliation status: **exact / partial due to missing data**;
- evidence source: **IMPLEMENTER MUST FILL support-bundle paths/records**.

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