# Change: Combat History and Reinforcement Accounting

## Status

Proposed — APPLY after this change packet is merged into `develop`.

## Problem

The current fight cards show only army snapshots before and after an engagement plus categorized losses. This creates misleading output when units are produced, transformed or join during the combat window. A player can appear to lose more units than existed in the `Армия до` snapshot because the report does not expose reinforcements produced or arriving during the fight.

The site also lacks a clear chronological combat history. A user cannot reliably move through all detected engagements, understand their ordering, or compare how the same player's army changed from one engagement to the next.

## Goals

1. Add an explicit chronological combat history to the browser report and Markdown output.
2. Split each participant's combat accounting into:
   - army at engagement start;
   - reinforcements during the engagement;
   - combat-unit losses;
   - worker losses;
   - infrastructure losses;
   - static-defence losses;
   - army at engagement end.
3. Make the accounting explainable without requiring a winner or efficiency score.
4. Preserve team-aware attribution and victim-owner loss semantics.
5. Validate the complete user-visible flow against the same fixed replay/support bundle used for current manual testing.

## Non-goals

- Do not redesign Combat Detector V3 in this change unless production integration requires a narrowly scoped fix.
- Do not introduce a single army-power or combat-efficiency score.
- Do not infer that every unit produced during the time window physically participated in the fight.
- Do not implement strategic preparation between engagements in this change; only expose the factual combat history and reinforcement accounting needed by that later capability.
- Do not implement scouting UI in this change.

## Core semantic distinction

`Reinforcements during engagement` means units that became available to the player during the engagement interval and explain the transition between start state, losses and end state. It does not automatically claim that every such unit reached the battlefield.

Where replay evidence permits actual local participation, the implementation MAY distinguish:

- produced or completed during engagement;
- arrived in the combat region during engagement;
- confirmed local participant.

When this distinction cannot be supported, use neutral wording and explicit confidence rather than claiming battlefield participation.

## Required accounting invariant

For each player and unit type, the implementation shall make the state transition explainable:

`start count + additions during interval - losses during interval + explicit transformations/corrections = end count`

If exact reconciliation is impossible because lifecycle events are incomplete, the UI shall show an explicit `неполные данные` or equivalent marker and shall not silently present contradictory counts.

## Fixed validation replay

Use the same replay/support bundle currently used for manual verification, including the observed Lulu fight where the site showed:

- `Армия до`: 2 × Zergling;
- `Боевые потери`: 3 × Zergling;
- `Армия после`: 15 × Zergling.

The new report must explain this transition by exposing additions/reinforcements or by explicitly marking incomplete reconciliation. The final expected values must be derived from the replay, not hard-coded from this example.

## Completion condition

The APPLY phase is complete only when production code, REST/serialized contract, React UI, Markdown output, automated tests, project documentation and the tester handoff in `tasks.md` agree on the same semantics.