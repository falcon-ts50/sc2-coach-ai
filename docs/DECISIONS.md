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
