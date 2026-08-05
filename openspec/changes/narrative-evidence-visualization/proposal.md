# Change: Narrative Evidence Visualization

## Why

Narrative Analysis V1 connects match phases, transitions and a principal causal chain, but the evidence is still harder to verify than it should be. The current charts emphasize only the selected player's series, split related metrics into visually isolated panels, and use labels whose contrast is too low against the dark report background. Combat cards describe engagements, yet they do not provide a compact side-by-side force table that lets a reader verify what each side actually fielded, lost, added and destroyed.

The next product change should improve the report as an explanatory instrument rather than add another independent analytics subsystem. A reader must be able to move from a narrative claim to synchronized quantitative evidence, compare every participant on the same timeline, and inspect the force balance of a combat without reconstructing it mentally from several cards.

## Product value

This change makes the report easier to understand and audit for a player who does not know the internal engines:

- every graph answers "how did I compare with the other players at this moment?";
- selecting a narrative phase, combat or event reveals the same time interval across all graphs;
- combat tables answer "who brought what, what changed during the fight, what was lost, and what was destroyed?";
- the focused player remains visually dominant without hiding teammates or opponents;
- readable labels and explicit legends reduce interpretation errors;
- evidence remains deterministic and traceable to existing Context, Combat and Narrative outputs.

The product outcome is a coherent evidence layer for the narrative, not a statistics dashboard with more unrelated widgets.

## Outcome

After implementation, the browser report will provide:

- synchronized comparison graphs for army value, economy proxy and occupied supply;
- all match participants overlaid on each metric graph;
- the selected player drawn with the strongest solid line, while non-selected players use visually subordinate dashed lines;
- stable player/team colour and line-style semantics across every graph and combat table;
- shared hover/cursor, selected time range and event markers across graphs;
- phase, turning-point and combat selection that highlights the same interval everywhere;
- engagement force tables comparing sides and participants at combat start and end;
- per-unit rows for opening force, additions, losses, surviving force and available kill counts;
- explicit completeness and reconciliation indicators where replay lifecycle or kill-credit evidence is partial;
- accessible text, axis, legend and annotation contrast on the dark theme;
- equivalent analytical evidence in Markdown without requiring interactive behaviour.

## Scope

### In scope

- Extend the backend chart contract from selected-player-only defaults to participant-aware metric series.
- Preserve player ID, display name, team ID, relationship to the selected player and source completeness for every series.
- Define presentation metadata sufficient for deterministic frontend styling without hard-coding player identity rules in individual charts.
- Render all participants on army-value, economy-proxy and occupied-supply charts.
- Emphasize the selected player with a solid, thicker line and subordinate other players with dashed lines; retain additional non-colour cues for team/opponent distinction.
- Synchronize chart domain, cursor/hover timestamp, selected interval and event/phase/combat markers.
- Make phase cards, narrative events, turning points and combat cards capable of selecting or focusing a shared time range.
- Add a combat evidence table grouped by SC2 side/team and participant.
- Show combat-capable unit composition at engagement start and end, additions during the interval, categorized losses and available per-unit kill credit.
- Show team totals without replacing participant rows.
- Surface reconciliation and evidence completeness instead of forcing contradictory numbers to appear exact.
- Improve dark-theme contrast for chart axes, tick labels, legends, section labels, table text, annotations and focus states.
- Preserve responsive/mobile usability through metric tabs, horizontal scrolling, collapsible participant detail or an equivalent implementation.
- Add deterministic REST, browser, Markdown and support-bundle acceptance coverage.

### Explicit non-goals

- A new opaque army-power or combat-effectiveness score.
- Inferring unit damage dealt when the replay exposes only kill credit.
- Claiming that every unit produced during a combat interval physically joined the local fight.
- Replacing Combat Detector, Combat Engine, Context Engine or Narrative Analysis ownership.
- Recomputing combat participants, losses, kills, phases or causal links in React.
- A real-time replay scrubber or animated battlefield map.
- Full accessibility certification for the entire website outside the affected report components.
- Strategic-result inference.

## Product decisions

1. Every primary metric graph contains all participants on the same axes for that metric.
2. The selected player is emphasized by line weight and a solid stroke. Other participants use dashed strokes and lower visual prominence; colour alone is never the sole identity cue.
3. Player colours and styles remain stable across army, economy and supply graphs and within combat tables.
4. Team and relationship semantics are explicit metadata. The frontend does not infer teammate/opponent status from array order or names.
5. All metric graphs share the same match-time domain and one synchronization state.
6. A combat table is organized first by side/team, then by participant, because tactical outcome in team games is team-aware while evidence must remain attributable to individuals.
7. Per-unit kill count means replay-attributed unit kills where available. Missing or ambiguous killer attribution is shown as incomplete, not zero.
8. Combat additions retain ADR-012 semantics: units became available during the interval; they are not automatically claimed as local reinforcements.
9. Team totals are additive summaries and never hide participant-level discrepancies.
10. Text contrast must meet at least WCAG AA contrast for normal text in the affected components where technically measurable; chart grid lines may remain lower contrast but labels and interactive states may not.

## Expected result on the website

For the fixed benchmark support bundle and `dragonDriver` perspective:

- the army-value, economy-proxy and occupied-supply graphs show `dragonDriver`, teammate `Lulu`, and both opponents on the same time axis;
- `dragonDriver` is the visually dominant solid series on every graph;
- `Lulu` and both opponents remain visible with subordinate dashed styles and stable identities across graphs;
- the legend clearly identifies player, team relationship and incomplete series where applicable;
- selecting the early-decline, stabilization, mid-game-improvement or late-deterioration phase highlights that interval on all graphs;
- selecting a combat marker scrolls/focuses the corresponding combat card, and selecting the combat card highlights its interval and marker on all graphs;
- each detected combat displays a side-by-side force table with team/side totals and participant rows for start force, additions, losses, end force and available kills by unit type;
- worker, infrastructure and static-defence losses remain separate from combat-unit composition;
- any arithmetic mismatch or missing kill attribution is visibly marked partial/incomplete rather than silently normalized;
- axis labels, tick labels, metric captions, legends and table text are readable against the dark background shown in the supplied screenshot;
- the page remains understandable at mobile width by showing one metric at a time or an equivalent responsive representation without losing participant identity or evidence access.

## Affected capabilities

- narrative evidence visualization;
- participant comparison;
- combat evidence presentation;
- chart synchronization;
- REST analysis contract;
- support bundle;
- React report;
- Markdown report;
- visual accessibility of analytical evidence.

## Relationship to existing engines

The change consumes existing authoritative outputs and adds presentation-oriented aggregation only:

```text
Context time series ----+
Narrative phases/events -+--> Evidence view model --> REST / React / Markdown
Turning points ----------+
Combat history ----------+--> Combat force-table view model
```

Combat-unit ownership, loss attribution, additions and reconciliation remain owned by Combat Engine contracts. Narrative phase boundaries and causal meaning remain owned by Narrative Analysis. The frontend receives explicit identities, intervals and evidence references and only renders/interacts with them.

## Risks

- Four participant lines may become visually crowded in 2v2 and larger matches.
- Solid-versus-dashed styling alone may be insufficient for colour-vision deficiencies or dense overlaps.
- Kill credit may be unavailable or ambiguous for some replay events.
- Team totals may appear authoritative even when participant reconciliation is partial.
- Shared synchronization can cause excessive React re-rendering.
- Combat tables may become too wide on mobile.

## Mitigations

- Use stable colour plus stroke pattern, width, labels and selectable legend filtering.
- Keep the selected player dominant and allow temporary isolation without changing default all-participant visibility.
- Carry completeness at series, participant, row and combat levels.
- Derive team totals only from visible attributable rows and propagate partial status.
- Centralize synchronization state and throttle pointer-driven updates where necessary.
- Use compact unit rows, collapsible participants and horizontal scrolling on narrow screens.

## Rollout

Implement as one APPLY change on a branch created directly from current `develop`. The change is additive: existing narrative and combat payloads remain compatible, while new participant series and evidence-table fields are added. Existing detailed sections remain available.
