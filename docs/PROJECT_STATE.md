# SC2 Coach AI — Project State

Last updated: 2026-08-05
Primary integration branch: `develop`
Production branch: `main`

## Current product

SC2 Coach accepts an `.SC2Replay`, decodes it through the Python replay pipeline, builds a Java domain analysis, and renders a player-focused report in React. Reports can be rebuilt for another player and exported as Markdown or a support bundle.

## Current architecture

```text
SC2Replay
  -> Python decoder and timeline extraction
  -> Java ReplayAnalysis domain model
  -> macro and turning-point analysis
  -> Combat Engine V2
  -> Narrative Analysis vertical slice
  -> Combat Narrative Engine
  -> Coach feed/API response
  -> React report
```

## Completed

- Stateless replay upload and deletion after analysis.
- Player-perspective report rebuilding.
- Linear report layout.
- Downloadable Markdown, transcript and support bundle.
- Combat Engine V2 domain contracts.
- Combat-unit composition before and after detected fights.
- Separate worker, infrastructure and static-defence losses.
- Chronological combat history in the REST contract, browser report, Markdown export and support bundle, including per-participant additions during the engagement interval and explicit reconciliation status.
- Combat upgrades and relevant technologies in army snapshots.
- Canonical English display names for SC2 upgrades.
- Filtering of replay noise, cosmetics, beacons, larva and map objects.
- Victim-owner loss attribution.
- Multi-player and team-aware combat outcome evaluation.
- Build identity work tracked in PR #58.
- Combat Detector V3 contracts for raw combat evidence, spatial-temporal clustering and final combat assembly.
- Regression coverage for spatially separated fights, missing coordinates, victim ownership and team participants.
- Information Engine V1 domain contracts for scouting-derived potential information, gaps, response candidates and information state.
- Narrative Analysis V1 vertical slice exposes a backend-owned player-focused timeline, preliminary verdict, state transitions, cautious causal chain and match-overview chart model through REST, Markdown, support bundle and the React report.
- Narrative evidence visualization exposes backend-owned all-participant army/economy/supply comparison series, shared evidence focus references, and team-aware combat force tables through REST, Markdown, support bundle and the React report.

## Known limitations

- Production combat detection still starts primarily from Attack commands and a fixed time window.
- Combat Detector V3 clustering contracts exist, but are not wired into the production report flow yet.
- Information Engine V1 is independent from combat analysis and is not wired into the public report yet.
- Narrative Analysis V1 is preliminary and does not infer strategic result, player intent, vision certainty, or guaranteed causal outcomes; `strategicResultStatus` remains `NOT_EVALUATED`.
- Army reconstruction is lifecycle-event based and can be incomplete when replay events omit ownership or transformations.
- Combat additions mean units that became available during the engagement interval; the report does not claim those units physically joined the local fight unless future spatial evidence supports that.
- Per-unit combat kill credit is marked unavailable in Narrative Evidence because the current production combat DTO does not expose stable killer-unit identity; unknown kill credit must not be rendered as zero.
- Combat outcome is heuristic, not a full simulation of tactical value.
- Unit display-name normalization is incomplete compared with the full Blizzard localization catalog.
- Narrative quality depends on confidence of combat reconstruction and macro metrics.

## Current work sequence

1. Merge build/version display PR #58 into `develop`.
2. Validate combat participant attribution on the uploaded 2v2 support bundle.
3. Wire Combat Detector V3 into production after validating it against real replay support bundles.
4. Wire Information Engine V1 into API/report after validating potential scouting observations against real replay support bundles.
5. Introduce explicit tactical, economic and strategic outcomes.

## Starting a new ChatGPT session

Use this prompt:

> Continue development of `falcon-ts50/sc2-coach-ai`. Read `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md`, and the latest open pull requests. Work from `develop`; all feature PRs must target `develop` directly unless I explicitly request a stacked PR.

The repository documents are the source of truth. Conversation memory may preserve broad context, but not exact branch state, commits, support-bundle findings, or pending PR dependencies.

## Maintenance rule

Update this document after every material merged feature, architectural change, or newly confirmed limitation. Keep statuses factual and reference PR numbers where useful.
