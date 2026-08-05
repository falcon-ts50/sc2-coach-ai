# Tasks - Information Engine Architecture Review

## Gate

This change authorizes `REVIEW` only. Do not modify production code.

## 1. Read gate

- [x] 1.1 Read every mandatory source listed in `openspec/AGENTS.md`.
  - Evidence: read `openspec/AGENTS.md`, `openspec/project.md`, `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md`, `ARCHITECTURE.md`, all files under `openspec/changes/information-engine-architecture-review/`, relevant implementation/tests, and PR state for #63/#67.
- [x] 1.2 Identify the current Information Engine branch or PR and exact commit reviewed.
  - Evidence: current implementation is merged PR #63 (`agent/information-engine-v1` -> `develop`), head commit `4bf4322fbeb1cc18a22a13b71851c74de7f9a316`, merge commit `8e3728b1b083c44efc48aadbd8ca67efb50586a5`, current `origin/develop` `6a1e226a0ed4ef513f999807ee44e6f6df6c4d54`.
- [x] 1.3 Post the Read Gate report before editing any review artifact.
  - Evidence: Read Gate report sent in Telegram before modifying `design.md`, `spec.md` or `tasks.md`.
- [x] 1.4 Re-run Read Gate after the follow-up REVIEW request added player-perspective state, between-fight preparation and support-bundle 0.7.0 review.
  - Evidence: re-read `openspec/AGENTS.md`, `openspec/project.md`, active change files, project docs from `origin/develop` `6a1e226a0ed4ef513f999807ee44e6f6df6c4d54`, implementation/tests, `b30d8ce4d450` support-bundle/report path, and PR state for #63/#67; sent updated Read Gate report before editing follow-up artifacts.

## 2. Repository evidence

- [x] 2.1 Locate all Information Engine production code, tests, API contracts and documentation.
  - Evidence: `java/coach-domain/src/main/java/ai/sc2coach/domain/information/*.java`; `java/coach-domain/src/test/java/ai/sc2coach/domain/information/InformationEngineTest.java`; no portal/frontend references found by `rg`; docs references in `docs/PROJECT_STATE.md:41`, `docs/DECISIONS.md:93`.
- [x] 2.2 Locate adjacent engine contracts that it consumes or duplicates.
  - Evidence: adjacent scouting detector in `java/coach-domain/src/main/java/ai/sc2coach/domain/scouting/ScoutingEpisodeDetector.java`; combat/narrative/portal wiring found in `AnalysisEngineConfiguration.java`, `AnalysisService.java`, `CoachFeedEngine.java`, `CombatEngine.java`, `CombatNarrativeEngine.java`.
- [x] 2.3 Build a responsibility and dependency map with file/symbol references.
  - Evidence: completed in `design.md` sections `Responsibility boundary` and `Integration map`.
- [x] 2.4 Record unavailable evidence rather than inferring it.
  - Evidence: `design.md` records unavailable real-replay validation and missing source event identities under deviations/open questions/test strategy.
- [x] 2.5 Inspect support-bundle/report behaviour for version `0.7.0`, commit `b30d8ce4d450`.
  - Evidence: `design.md` section `Support-bundle/report behaviour at 0.7.0` cites `AnalysisService`, `AnalysisResponse`, `AnalysisEngineConfiguration`, `CoachFeedEngine`, `frontend/src/main.jsx`, `review_bundle.py`, current `ReplayAnalysis`, `PlayerState`, `MatchContextEngine`, `ArgumentDeltaEngine`, and `DecisionEngine`.
- [x] 2.6 Identify repository evidence for strategic preparation inputs and current gaps.
  - Evidence: `ReplayAnalysis.PlayerStat`/`TimelineEvent`, `ReplayDomainMapper`, `PlayerState`, `MatchContextEngine`, `ArgumentDeltaEngine`, `DecisionEngine`, `EconomyDecisionDetector`, `ArmyDecisionDetector`; no `StrategicPreparationInterval`/`PreparationProfile` contract exists.

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
- [x] 3.7 Define player-perspective information-state boundaries.
  - Evidence: `design.md` sections `ReplayFact / omniscient fact layer`, `Player-perspective InformationState`, `Processing model`, `Invariants`, and deviations for missing omniscient-vs-perspective boundary and missing staleness lifecycle.
- [x] 3.8 Define between-engagement strategic preparation scope and interval model.
  - Evidence: `design.md` sections `StrategicPreparationInterval`, `PreparationProfile`, `PreparationComparison`, processing stages, alternatives, test strategy and follow-up APPLY tasks.
- [x] 3.9 Determine why 0.7.0 reports lack scouting and preparation.
  - Evidence: `design.md` concludes the cause is combined: Information Engine not wired into portal/REST/Coach Feed/frontend/support bundle, decoder/domain inputs are partial but lack exact visibility/provenance, and strategic preparation has no domain model.

## 4. Capability specification

- [x] 4.1 Complete `specs/information-engine/spec.md` with normative requirements.
  - Evidence: spec now defines independent responsibility, canonical inputs, versioned configuration, episode/observation/gap/reaction/state/evidence/determinism/compatibility/real-replay requirements.
- [x] 4.2 Add positive, missing-data, contradiction and determinism scenarios.
  - Evidence: scenarios include Roach Warren -> Bunker, no scouting, missing coordinates, scout death, repeated analysis determinism, REST compatibility and private replay validation.
- [x] 4.3 Ensure each requirement is testable and avoids implementation-detail wording where possible.
  - Evidence: requirements are written as SHALL statements with GIVEN/WHEN/THEN scenarios; implementation file names are limited to evidence in `design.md`, not normative behaviour except current canonical input.
- [x] 4.4 Add player-perspective, acquisition/staleness and omniscient-fact separation requirements.
  - Evidence: spec requirements `Replay facts are separate from player perspective`, `Information State and Advantage`, and scenarios for unscouted enemy tech, unavailable visibility evidence, stale information and ally team sharing.
- [x] 4.5 Add strategic preparation interval and category requirements.
  - Evidence: spec requirements `Strategic preparation intervals`, `Preparation profile categories`, `Readiness against next engagement or later power spike`, and support-bundle compatibility scenarios.

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
- [ ] A.10 Add `ReplayFact` or equivalent source-fact model separating omniscient replay facts from player-perspective information.
- [ ] A.11 Add acquisition/staleness fields to player-perspective information state, including stale-after and missing-visibility markers.
- [ ] A.12 Decide whether between-engagement preparation is owned by Information Engine or a sibling `PreparationEngine`; document the decision as an ADR if ownership changes.
- [ ] A.13 Implement `StrategicPreparationInterval`, `PreparationProfile` and `PreparationComparison` contracts for workers/economy, immediate army, production capacity, tech/upgrades, expansions, static defence, scouting, resource bank/spend conversion and allied synchronization.
- [ ] A.14 Add support-bundle/REST artifacts for information and preparation outputs with versioned backward compatibility.
- [ ] A.15 Add tests proving omniscient replay facts do not leak into player-perspective knowledge without scouting/contact evidence.
- [ ] A.16 Add tests for preparation trade-offs, ally synchronization and readiness against the next engagement or later power spike without causal claims.

## Evidence log

- OpenSpec review branch: PR #67 `agent/information-engine-architecture-review` -> `develop`, head `14fe819e3df8251af191505a9afb2fce0c8e60f1` before follow-up review updates.
- Information implementation source: PR #63 merged into `develop`; head `4bf4322fbeb1cc18a22a13b71851c74de7f9a316`, merge `8e3728b1b083c44efc48aadbd8ca67efb50586a5`, reviewed from `origin/develop` `6a1e226a0ed4ef513f999807ee44e6f6df6c4d54`.
- Support-bundle/report behaviour source: production `main` commit `b30d8ce4d450`, application version `0.7.0`.
- Production code changes under this REVIEW: none.
