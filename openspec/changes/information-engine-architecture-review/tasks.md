# Tasks - Information Engine Architecture Review

## Gate

This change authorizes `REVIEW` only. Do not modify production code.

## 1. Read gate

- [x] 1.1 Read every mandatory source listed in `openspec/AGENTS.md`.
  - Evidence: read `openspec/AGENTS.md`, `openspec/project.md`, `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md`, `ARCHITECTURE.md`, all files under `openspec/changes/information-engine-architecture-review/`, relevant implementation/tests, and PR state for #63/#66.
- [x] 1.2 Identify the current Information Engine branch or PR and exact commit reviewed.
  - Evidence: current implementation is merged PR #63 (`agent/information-engine-v1` -> `develop`), head commit `4bf4322fbeb1cc18a22a13b71851c74de7f9a316`, merge commit `8e3728b1b083c44efc48aadbd8ca67efb50586a5`, current `origin/develop` `5fc9150251d418811f7c5d2dc515fb6d83a56858`.
- [x] 1.3 Post the Read Gate report before editing any review artifact.
  - Evidence: Read Gate report sent in Telegram before modifying `design.md`, `spec.md` or `tasks.md`.

## 2. Repository evidence

- [x] 2.1 Locate all Information Engine production code, tests, API contracts and documentation.
  - Evidence: `java/coach-domain/src/main/java/ai/sc2coach/domain/information/*.java`; `java/coach-domain/src/test/java/ai/sc2coach/domain/information/InformationEngineTest.java`; no portal/frontend references found by `rg`; docs references in `docs/PROJECT_STATE.md:41`, `docs/DECISIONS.md:93`.
- [x] 2.2 Locate adjacent engine contracts that it consumes or duplicates.
  - Evidence: adjacent scouting detector in `java/coach-domain/src/main/java/ai/sc2coach/domain/scouting/ScoutingEpisodeDetector.java`; combat/narrative/portal wiring found in `AnalysisEngineConfiguration.java`, `AnalysisService.java`, `CoachFeedEngine.java`, `CombatEngine.java`, `CombatNarrativeEngine.java`.
- [x] 2.3 Build a responsibility and dependency map with file/symbol references.
  - Evidence: completed in `design.md` sections `Responsibility boundary` and `Integration map`.
- [x] 2.4 Record unavailable evidence rather than inferring it.
  - Evidence: `design.md` records unavailable real-replay validation and missing source event identities under deviations/open questions/test strategy.

## 3. Architecture review

- [x] 3.1 Complete the problem definition and responsibility boundary.
  - Evidence: `design.md` sections `Problem definition` and `Responsibility boundary`.
- [x] 3.2 Specify inputs, outputs, invariants and degradation behaviour.
  - Evidence: `design.md` sections `Inputs`, `Outputs`, `Invariants`, and `Failure and degradation behaviour`.
- [x] 3.3 Classify processing stages as facts, deterministic derivations, heuristics or presentation transforms.
  - Evidence: `design.md` section `Processing model`.
- [x] 3.4 Evaluate integration with existing engines and module boundaries.
  - Evidence: `design.md` section `Integration map`.
- [x] 3.5 Compare at least four architectural alternatives.
  - Evidence: `design.md` section `Alternatives considered`.
- [x] 3.6 Record deviations with severity and recommended ownership.
  - Evidence: `design.md` section `Current implementation findings / Deviations`.

## 4. Capability specification

- [x] 4.1 Complete `specs/information-engine/spec.md` with normative requirements.
  - Evidence: spec now defines independent responsibility, canonical inputs, versioned configuration, episode/observation/gap/reaction/state/evidence/determinism/compatibility/real-replay requirements.
- [x] 4.2 Add positive, missing-data, contradiction and determinism scenarios.
  - Evidence: scenarios include Roach Warren -> Bunker, no scouting, missing coordinates, scout death, repeated analysis determinism, REST compatibility and private replay validation.
- [x] 4.3 Ensure each requirement is testable and avoids implementation-detail wording where possible.
  - Evidence: requirements are written as SHALL statements with GIVEN/WHEN/THEN scenarios; implementation file names are limited to evidence in `design.md`, not normative behaviour except current canonical input.

## 5. Follow-up plan

- [x] 5.1 Add concrete APPLY tasks required by the review, but leave them unchecked and clearly marked as not authorized.
  - Evidence: see `Follow-up APPLY tasks (not authorized under REVIEW)` below.
- [x] 5.2 Identify ADR, architecture, roadmap or project-state updates that would be required if the design is accepted.
  - Evidence: follow-up tasks include ADR/spec synchronization and public integration documentation.
- [x] 5.3 Summarize blockers, major findings and open questions in the PR description or review report.
  - Evidence: `design.md` recommendation is `ACCEPT WITH REQUIRED CHANGES`; deviations and open questions are listed.

## Follow-up APPLY tasks (not authorized under REVIEW)

- [ ] A.1 Add source-evidence references to `InformationEpisode`, `InformationObservation`, `InformationGap` and `InformationReaction`.
- [ ] A.2 Introduce versioned `InformationEngineConfig` for scout units, thresholds, response windows, output limits and confidence weights.
- [ ] A.3 Resolve overlap between `domain/scouting` and `domain/information` by deprecating, folding or reusing `ScoutingEpisodeDetector`.
- [ ] A.4 Add target player/team scope to `InformationState` and `InformationAdvantage`.
- [ ] A.5 Define whether `InformationNarrative` remains structured domain data or moves rendered prose to a narrative/presentation layer.
- [ ] A.6 Add deterministic identity/order tests for repeated analysis.
- [ ] A.7 Add missing-coordinate, missing-owner, ambiguous same-unit scout and multi-opponent team-game tests.
- [ ] A.8 Validate against private real replay artifacts outside git and record outputs/deviations.
- [ ] A.9 Before public integration, document REST/support-bundle compatibility and update `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md` or `ARCHITECTURE.md` if the accepted architecture changes.

## Evidence log

- OpenSpec source branch: PR #66 `agent/openspec-workflow` -> `develop`, head `aef1e7e846b196831011325b4b4e7250609a7efc` before review updates.
- Information implementation source: PR #63 merged into `develop`; head `4bf4322fbeb1cc18a22a13b71851c74de7f9a316`, merge `8e3728b1b083c44efc48aadbd8ca67efb50586a5`, reviewed from `origin/develop` `5fc9150251d418811f7c5d2dc515fb6d83a56858`.
- Production code changes under this REVIEW: none.
