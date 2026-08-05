# SC2 Coach AI — Architectural Decisions

This document records decisions that should survive individual chats and implementation rewrites.

## ADR-001 — Integration branch

**Status:** Accepted

All ordinary feature and bug-fix pull requests target `develop` directly. `main` is the production branch and receives guarded release PRs from `develop`.

Stacked PRs are allowed only when explicitly agreed in advance. After a stacked base merges, dependent PRs must be retargeted to `develop` before merge.

## ADR-002 — Combat army composition

**Status:** Accepted

Army composition contains combat-capable units only. Workers, structures and static defence are excluded from the army snapshot but retained as separate loss categories.

Reason: workers and buildings affect strategic outcome, but mixing them into army composition produces misleading reports such as larva, beacons and production structures being presented as fighting units.

## ADR-003 — Combat collateral damage

**Status:** Accepted

A combat episode records at least four distinct loss categories:

- combat units;
- workers;
- infrastructure and production structures;
- static defence.

Tactical army trade and strategic episode result may therefore disagree.

## ADR-004 — Upgrades are part of the army snapshot

**Status:** Accepted

Completed weapon, armour and shield levels, plus composition-relevant technologies such as Stimpack, Blink or Metabolic Boost, are captured at the beginning of a fight.

No opaque single-number army-power score is used until it can be made explainable.

## ADR-005 — Canonical names

**Status:** Accepted

The report uses canonical English StarCraft II names for upgrades and technologies. Raw replay identifiers and cosmetic reward events must never be displayed.

A future official Russian localization should be generated from Blizzard client localization assets rather than maintained as an incomplete manual table.

## ADR-006 — Victim ownership

**Status:** Accepted

For death events, losses are attributed to the owner of the victim. The killer is evidence of active participation, not ownership of the loss.

This distinction is mandatory for participant resolution and loss accounting.

## ADR-007 — Team-aware combat evaluation

**Status:** Accepted

In team games, combat outcome is evaluated by SC2 team. The engine must not declare an individual winner merely because that player personally lost fewer units than every other participant.

## ADR-008 — Build identity

**Status:** Accepted

Every production deployment has a visible identity composed of:

- semantic application version;
- increasing UTC build number;
- Git commit;
- build time.

The same identity is exposed through an API and displayed on the site so stale deployments can be identified immediately.

## ADR-009 — Project continuity

**Status:** Accepted

`docs/PROJECT_STATE.md`, this file, `ROADMAP.md`, and current GitHub PR state are the source of truth between ChatGPT sessions. Conversational memory is helpful context but is not relied upon for exact technical state.

## ADR-010 — Combat Detector V3 staging

**Status:** Accepted

Combat Detector V3 is introduced behind separate domain contracts before it replaces the production `CombatEngine`.

The detector separates raw combat evidence extraction, spatial-temporal clustering, and final `Combat` assembly. Clustering uses configurable time and map-distance thresholds as explicit heuristics. Missing spatial data reduces confidence and is surfaced on the cluster rather than silently inferred.

This staged approach keeps the live report stable while regression tests lock in the expected behaviour for simultaneous fights, team-game participants, and victim-owner attribution.

## ADR-011 — Information Engine is independent from Combat Engine

**Status:** Accepted

Scouting and information analysis are owned by a separate Information Engine, not by Combat Engine or Combat Detector V3.

The Information Engine answers what a player potentially could know, what information was missing, and which later actions are response candidates. Combat analysis may later consume this information as context, but information analysis must not depend on combat outcomes.

Replay data does not contain a complete vision log or player intent. User-facing and narrative contracts therefore use `Potentially Observed` and `Response Candidate` language, never causal claims such as "the player saw" or "the player decided because".

## ADR-012 — Combat additions and reconciliation

**Status:** Accepted

Combat history exposes a participant's army at the beginning and end of each engagement, plus combat-unit additions during the engagement interval and categorized losses.

The additions row means units became available during the time window. It does not claim that every produced unit physically reached or participated in the local fight unless later spatial evidence explicitly supports that conclusion.

For each combat unit type, the report should reconcile:

`army at start + additions - combat losses = army at end`

When replay lifecycle data cannot support exact reconciliation, the participant must be marked partial/incomplete instead of silently presenting contradictory counts.

## ADR-013 — Release versioning policy

**Status:** Accepted

Semantic version identifies a released product, not an individual feature merge or CI attempt. It uses strict `MAJOR.MINOR.PATCH` format and changes only in a release PR from `develop` to `main`. Every such release must increase the version by at least PATCH; the release author explicitly chooses PATCH, MINOR or MAJOR.

The root `VERSION` file is authoritative. Committed Maven, frontend and container version references must remain synchronized with it and CI rejects drift.

After a successful merge to `main`, the validated commit receives immutable annotated tag `v<version>` before production deployment. Rebuilding the same commit preserves semantic version and Git commit while the independent build number may change.

## ADR-014 — Narrative Analysis is an additive deterministic layer

**Status:** Accepted

Narrative Analysis is introduced as a backend-owned vertical slice that consumes existing domain outputs instead of reimplementing combat detection, turning-point scoring, recommendation rules or scouting detection.

The public report may show a preliminary verdict, phases, a principal chain and a match-overview chart, but the domain contract must preserve source references, confidence and limitations. Temporal order may support cautious links such as `PRECEDED` or `RECOVERED_FROM`; it must not be presented as proof of player intent, visibility, a forced winner, or a strategic result.

Strategic result is explicitly out of scope for this slice and is serialized as `NOT_EVALUATED`.
