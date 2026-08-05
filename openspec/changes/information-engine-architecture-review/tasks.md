# Tasks — Information Engine Architecture Review

## Gate

This change authorizes `REVIEW` only. Do not modify production code.

## 1. Read gate

- [ ] 1.1 Read every mandatory source listed in `openspec/AGENTS.md`.
- [ ] 1.2 Identify the current Information Engine branch or PR and exact commit reviewed.
- [ ] 1.3 Post the Read Gate report before editing any review artifact.

Acceptance: the report names all files read, branch/commit, scope, non-goals, contradictions and next action.

## 2. Repository evidence

- [ ] 2.1 Locate all Information Engine production code, tests, API contracts and documentation.
- [ ] 2.2 Locate adjacent engine contracts that it consumes or duplicates.
- [ ] 2.3 Build a responsibility and dependency map with file/symbol references.
- [ ] 2.4 Record unavailable evidence rather than inferring it.

Acceptance: every material architectural claim in `design.md` is grounded in a repository path, symbol, test or current PR diff.

## 3. Architecture review

- [ ] 3.1 Complete the problem definition and responsibility boundary.
- [ ] 3.2 Specify inputs, outputs, invariants and degradation behaviour.
- [ ] 3.3 Classify processing stages as facts, deterministic derivations, heuristics or presentation transforms.
- [ ] 3.4 Evaluate integration with existing engines and module boundaries.
- [ ] 3.5 Compare at least four architectural alternatives.
- [ ] 3.6 Record deviations with severity and recommended ownership.

Acceptance: `design.md` ends with one supported recommendation and explicit conditions for APPLY.

## 4. Capability specification

- [ ] 4.1 Complete `specs/information-engine/spec.md` with normative requirements.
- [ ] 4.2 Add positive, missing-data, contradiction and determinism scenarios.
- [ ] 4.3 Ensure each requirement is testable and avoids implementation-detail wording where possible.

Acceptance: a future implementer can derive tests and contracts without Telegram history.

## 5. Follow-up plan

- [ ] 5.1 Add concrete APPLY tasks required by the review, but leave them unchecked and clearly marked as not authorized.
- [ ] 5.2 Identify ADR, architecture, roadmap or project-state updates that would be required if the design is accepted.
- [ ] 5.3 Summarize blockers, major findings and open questions in the PR description or review report.

Acceptance: no production-code task is executed under this REVIEW change.

## Evidence log

Add concise evidence beneath each completed task. Include commands, files, tests and PR/commit identifiers. Do not replace evidence with statements such as "reviewed" or "looks correct".
