# Change: Information Engine Architecture Review

## Status

Proposed — REVIEW only. Production implementation is not authorized by this change packet.

## Problem

The Information Engine is being implemented while task instructions have been transferred through multiple Telegram messages. The agent began work before receiving the complete instruction set. This creates a material risk that implementation scope, contracts and architectural placement differ from the intended design.

The repository currently defines strong boundaries for decoder extraction, deterministic domain analysis, portal orchestration and presentation, but it does not yet contain a repository-native specification for the Information Engine.

## Desired outcome

Produce a reviewable, evidence-backed architecture description for the Information Engine based on the actual repository and current implementation branch or PR.

The review must establish:

- the exact responsibility of the Information Engine;
- its inputs, outputs and invariants;
- its relationship to Context, Decision, Turning Point, Knowledge, Combat and narrative components;
- which module owns it;
- deterministic versus heuristic behaviour;
- confidence and evidence semantics;
- failure and partial-data behaviour;
- compatibility and migration impact;
- test strategy and acceptance scenarios;
- discrepancies between intended architecture and current code.

## Scope

- Inspect current Information Engine implementation and tests.
- Identify the implementation branch or PR and its base.
- Map current code to existing architecture and ADRs.
- Complete `design.md` with concrete findings and proposed contracts.
- Create or update `specs/information-engine/spec.md` with normative requirements and scenarios.
- Update `tasks.md` with evidence and any required follow-up implementation tasks.
- Report contradictions, hidden coupling, duplicated responsibilities and premature abstractions.

## Non-goals

- Do not modify production code.
- Do not refactor existing engines.
- Do not add new product features.
- Do not merge or retarget existing PRs.
- Do not redefine accepted ADRs without proposing a separate ADR change.
- Do not claim the design is validated when relevant code, tests or replay evidence were unavailable.

## Completion condition

The REVIEW phase is complete when the design and capability specification are sufficiently precise that a separate APPLY task can be implemented without relying on Telegram history or unstated assumptions.
