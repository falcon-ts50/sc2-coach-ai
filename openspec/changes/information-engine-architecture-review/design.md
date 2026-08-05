# Information Engine — Architecture Review

## Review metadata

- Change ID: `information-engine-architecture-review`
- Lifecycle gate: `REVIEW`
- Base branch: `develop`
- Implementation branch or PR inspected: _to be completed_
- Reviewed commit: _to be completed_
- Reviewer: OpenClaw
- Review date: _to be completed_

## Existing architectural context

Summarize only repository-confirmed facts from:

- `ARCHITECTURE.md`;
- `ROADMAP.md`;
- `docs/PROJECT_STATE.md`;
- `docs/DECISIONS.md`;
- current implementation and tests.

Do not use Telegram history as evidence.

## Problem definition

Describe the user-visible or downstream analytical problem the Information Engine solves. Distinguish it from replay decoding, match context, decisions, turning points, combat detection, knowledge rules and narrative rendering.

## Responsibility boundary

### Owns

_To be completed._

### Does not own

_To be completed._

### Module placement

State the owning module and justify it against existing module boundaries.

## Inputs

For every input, specify:

- type or contract;
- producer;
- required versus optional fields;
- timestamp and player/team semantics;
- confidence/provenance semantics;
- behaviour when absent or incomplete.

## Outputs

For every output, specify:

- type or proposed contract;
- consumer;
- invariants;
- ordering and identity rules;
- serialization impact, if any;
- confidence and evidence payload.

## Processing model

Describe the processing stages in order. Mark each stage as one of:

- direct fact normalization;
- deterministic derivation;
- configurable heuristic;
- presentation-only transformation.

No stage may be described as "AI" without an explicit deterministic contract or an intentionally nondeterministic boundary outside the core pipeline.

## Integration map

Provide a diagram showing Information Engine dependencies and consumers. Explicitly evaluate its relationship to:

- Python decoder/transcript;
- Java domain mapping;
- Match Context Engine;
- Decision Engine;
- Turning Point Engine;
- Combat Engine / Detector V3;
- Knowledge Engine;
- Combat Narrative Engine;
- Coach Feed and REST contract;
- React and Markdown rendering.

## Invariants

List enforceable invariants. At minimum evaluate:

- evidence traceability;
- deterministic output for identical input/configuration;
- stable timestamps and player/team attribution;
- explicit missing-data handling;
- separation of fact, derivation and hypothesis;
- no presentation-layer causality;
- no hidden aggregate score without explainable components.

## Failure and degradation behaviour

Specify behaviour for malformed, missing or contradictory replay data. The design must prefer explicit absence or reduced confidence over invented values.

## Current implementation findings

### Matches intended architecture

_To be completed with file and test references._

### Deviations

For each deviation record:

- severity: blocker / major / minor;
- evidence: file, symbol and test;
- consequence;
- recommended correction;
- whether correction belongs to the current implementation PR or a later change.

### Open questions

Record unresolved decisions. Do not answer them through assumption.

## Alternatives considered

Evaluate at least:

1. Information Engine as a distinct domain service;
2. information extraction distributed across existing engines;
3. transcript-first normalized information model;
4. presentation-oriented aggregation outside the domain core.

State why each is accepted, rejected or deferred.

## Test strategy

Define required unit, contract, integration and real-replay validation. Include negative scenarios where information must not be emitted or confidence must fall.

## Migration and compatibility

Document impact on JSON contracts, REST, Markdown, frontend, stored support bundles and existing tests. State whether versioning is required.

## Recommendation

Finish with exactly one recommendation:

- `ACCEPT CURRENT DIRECTION`;
- `ACCEPT WITH REQUIRED CHANGES`;
- `REDESIGN BEFORE APPLY`.

Support it with repository evidence and list the minimum conditions for entering the APPLY gate.
