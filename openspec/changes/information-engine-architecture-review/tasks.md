# Tasks — Information Engine Architecture Review

## Gate

This change authorizes `REVIEW` only. Do not modify production code.

## 1. Read gate

- [ ] 1.1 Read every mandatory source listed in `openspec/AGENTS.md`.
- [ ] 1.2 Identify the current Information Engine branch or PR and exact commit reviewed.
- [ ] 1.3 Inspect the reference support bundle behaviour described in `design.md` or obtain equivalent repository evidence.
- [ ] 1.4 Post the Read Gate report before editing any review artifact.

Acceptance: the report names all files read, branch/commit, scope, non-goals, contradictions, available replay evidence and next action.

## 2. Repository evidence

- [ ] 2.1 Locate all Information Engine production code, tests, API contracts and documentation.
- [ ] 2.2 Locate adjacent engine contracts that it consumes or duplicates.
- [ ] 2.3 Locate decoder/transcript evidence for commands, targets, positions, scans, visibility, workers, production, upgrades, expansions, resources and combat boundaries.
- [ ] 2.4 Determine what player-perspective scouting can be proven from current replay data and what cannot.
- [ ] 2.5 Build a responsibility and dependency map with file/symbol references.
- [ ] 2.6 Record unavailable evidence rather than inferring it.

Acceptance: every material architectural claim is grounded in a repository path, symbol, test, PR diff or named support-bundle artifact.

## 3. Architecture review

- [ ] 3.1 Complete the problem definition and responsibility boundary.
- [ ] 3.2 Specify omniscient facts, observations, knowledge state and hypothesis semantics.
- [ ] 3.3 Define strategic interval boundaries and explain alternatives.
- [ ] 3.4 Define preparation-allocation categories for economy, army, production, technology, expansion, defence, information and banking/spending.
- [ ] 3.5 Specify individual and team comparison semantics.
- [ ] 3.6 Specify inputs, outputs, invariants and degradation behaviour.
- [ ] 3.7 Classify stages as facts, deterministic derivations, heuristics or presentation transforms.
- [ ] 3.8 Evaluate integration with all existing engines and module boundaries.
- [ ] 3.9 Compare at least five architectural alternatives.
- [ ] 3.10 Record deviations with severity and recommended ownership.

Acceptance: `design.md` explains how the system can compare strategic preparation between fights and how it prevents omniscient information from being presented as player knowledge.

## 4. Capability specification

- [ ] 4.1 Complete `specs/information-engine/spec.md` with normative requirements.
- [ ] 4.2 Add scenarios for scouting, no scouting, stale information and incomplete visibility evidence.
- [ ] 4.3 Add scenarios comparing immediate army investment with workers, production, expansion and delayed technology.
- [ ] 4.4 Add team synchronization and non-combat interval scenarios.
- [ ] 4.5 Add missing-data, contradiction and determinism scenarios.
- [ ] 4.6 Ensure each requirement is testable and avoids unsupported intent claims.

Acceptance: a future implementer can derive contracts and tests without Telegram history.

## 5. Current implementation assessment

- [ ] 5.1 Verify whether current code emits any explicit scouting/observation/knowledge-state output.
- [ ] 5.2 Verify whether current code compares player investments between engagements.
- [ ] 5.3 Verify whether combat outcome or army-loss scoring has incorrectly become the Information Engine's primary responsibility.
- [ ] 5.4 Verify whether current output leaks omniscient enemy state into player-perspective conclusions.
- [ ] 5.5 Classify the direction as acceptable, correctable or requiring redesign.

Acceptance: findings include exact code/test/report references and do not rely on absence from one UI view alone.

## 6. Follow-up plan

- [ ] 6.1 Add concrete APPLY tasks required by the review, left unchecked and explicitly unauthorized.
- [ ] 6.2 Split follow-up work into decoder gaps, domain contracts, analysis logic, API/report integration and replay validation.
- [ ] 6.3 Identify ADR, architecture, roadmap or project-state updates required if accepted.
- [ ] 6.4 Summarize blockers, major findings and open questions in the PR description or review report.

Acceptance: no production-code task is executed under this REVIEW change.

## Evidence log

Add concise evidence beneath each completed task. Include commands, files, tests, support-bundle paths and PR/commit identifiers. Statements such as `reviewed` or `looks correct` are not evidence.