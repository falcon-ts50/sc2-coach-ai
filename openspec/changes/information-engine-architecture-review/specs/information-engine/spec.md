# Information Engine Capability Specification

## Status

Draft for architecture review. Complete during the REVIEW gate; do not treat placeholders as accepted requirements.

## Purpose

The Information Engine shall transform repository-defined replay and analysis inputs into structured, explainable information suitable for deterministic downstream analysis and presentation without inventing facts absent from replay evidence.

## Requirements

### Requirement: Explicit responsibility boundary

The Information Engine SHALL have a documented responsibility that does not duplicate replay decoding, match-context calculation, decision detection, turning-point detection, combat detection, knowledge recommendation or presentation-layer rendering.

#### Scenario: Adjacent responsibility is proposed

- GIVEN a proposed Information Engine operation already belongs to another established engine;
- WHEN the architecture is reviewed;
- THEN the operation is either rejected from Information Engine scope or the ownership change is explicitly documented and approved.

### Requirement: Evidence traceability

Every emitted information item SHALL reference sufficient replay-derived or deterministic source evidence to explain how it was produced.

#### Scenario: Information is emitted from complete evidence

- GIVEN valid source records required by an information rule;
- WHEN the rule emits an information item;
- THEN the output identifies the relevant timestamps, players or teams, source records and derivation category.

#### Scenario: Evidence cannot be identified

- GIVEN an output candidate whose supporting evidence cannot be traced;
- WHEN the engine evaluates it;
- THEN the engine does not present it as a confirmed fact.

### Requirement: Deterministic behaviour

For identical versioned input and identical configuration, the Information Engine SHALL produce semantically identical output.

#### Scenario: Analysis is repeated

- GIVEN the same replay analysis input, engine version and configuration;
- WHEN processing is repeated;
- THEN information identities, ordering, values, confidence and evidence references are semantically identical.

### Requirement: Fact and inference separation

The Information Engine SHALL distinguish direct facts, deterministic derivations and heuristic hypotheses in its output contract.

#### Scenario: A heuristic threshold is used

- GIVEN an information item depends on a configurable threshold or incomplete behavioural interpretation;
- WHEN the item is emitted;
- THEN its derivation category and confidence reflect heuristic status rather than direct fact status.

### Requirement: Explicit missing-data degradation

The Information Engine SHALL NOT silently reconstruct missing coordinates, ownership, timestamps, values or intent.

#### Scenario: Required source data is missing

- GIVEN source data required for a confirmed information item is absent;
- WHEN processing occurs;
- THEN the engine either omits the item or emits an explicitly partial result with reduced confidence and recorded missing evidence.

### Requirement: Stable attribution

Information concerning players, teams, units, structures, upgrades or combat losses SHALL use repository-defined identity and ownership semantics.

#### Scenario: Victim and killer differ

- GIVEN a death event with a victim owner and a different killer owner;
- WHEN loss-related information is produced;
- THEN the loss is attributed to the victim owner and killer identity is used only as participation evidence, consistent with ADR-006.

#### Scenario: Team game information is produced

- GIVEN a team replay;
- WHEN information represents team-level outcome or advantage;
- THEN attribution follows SC2 team identity rather than selecting an individual winner from personal losses alone.

### Requirement: Explainable aggregates

The Information Engine SHALL NOT expose an opaque aggregate score as evidence unless its components and calculation are available to downstream consumers.

#### Scenario: Composite value is emitted

- GIVEN an output uses multiple measured components;
- WHEN the composite value is exposed;
- THEN the output includes the components, units and deterministic calculation or references a versioned calculation contract.

### Requirement: Contract compatibility

Any change to serialized replay analysis, REST or report contracts caused by the Information Engine SHALL be explicitly versioned or demonstrated to be backward compatible.

#### Scenario: New required field is proposed

- GIVEN an Information Engine contract adds a field required by existing consumers;
- WHEN the design is reviewed;
- THEN migration, defaulting and version compatibility are documented before APPLY is authorized.

## Open specification items

The REVIEW must replace this section with repository-grounded decisions for:

- canonical input types;
- canonical output types;
- item identity and ordering;
- confidence representation;
- evidence reference representation;
- owning module and package;
- downstream consumers;
- failure policy;
- required tests and real-replay scenarios.
