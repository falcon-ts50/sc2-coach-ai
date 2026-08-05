# Change: Narrative Analysis Vertical Slice

## Why

SC2 Coach already produces context, decisions, turning points, combat evidence, information-state findings and recommendations. These outputs are useful but remain mostly separate. The report can answer what happened in individual moments, yet it does not reliably explain why the match developed as a connected sequence.

The next product phase must introduce a deterministic narrative layer that organizes existing evidence into meaningful match phases, measurable state transitions and a principal causal chain. The same vertical slice must restore match graphs so users can visually verify the conclusions against army, economy and supply trends.

## Outcome

After implementation, the browser and Markdown report will contain a player-focused section titled approximately `How the match developed` with:

- a concise evidence-backed preliminary verdict;
- a chronological set of meaningful match phases;
- measurable state transitions with before/after evidence;
- a conservative principal causal chain;
- a synchronized match-overview chart;
- visible confidence and uncertainty;
- links from narrative conclusions to their evidence.

This is a product vertical slice, not a contracts-only staging change.

## Scope

### In scope

- Normalize existing engine outputs into `NarrativeEvent` inputs.
- Build explainable `MatchStateSnapshot` records over the match timeline.
- Detect significant `StateTransition` records.
- Segment the match into meaningful `MatchPhase` intervals.
- Add a minimal typed `CausalLink` model.
- Select one principal causal chain without claiming unsupported intent.
- Produce a deterministic `NarrativeTimeline` and `NarrativeSummary`.
- Expose the narrative and chart data through REST and the support bundle.
- Render the narrative above the existing detailed sections in React.
- Add a match-overview chart with army value, worker/economy proxy and supply by default.
- Add combat markers and phase intervals on the shared time axis.
- Preserve equivalent analytical content in Markdown.
- Validate against the fixed benchmark support bundle used by the project.

### Explicit non-goals

- Full advantage-window analysis.
- Full recovery-arc analysis.
- Strategic-result inference or official-result conflict resolution.
- Production-capacity and reinforcement-capacity modelling.
- Full team-narrative reasoning beyond correctly retaining teammate/team context.
- LLM-generated prose.
- An interactive node-link causal graph.
- Reimplementing combat, scouting, turning-point or recommendation detection.
- Frontend inference of match phases or causality.

## Product decisions

1. The first Narrative Analysis change is a vertical slice with visible browser output.
2. The narrative section appears near the top of the report in this order:
   - verdict;
   - match-overview chart;
   - chronological phases;
   - principal causal chain;
   - existing detailed report sections.
3. The first chart shows army value, worker/economy proxy and supply by default.
4. Bases and production structures may be optional series in this change, but they are not yet used for strategic-result inference.
5. `CausalLink` is an explicit domain contract in V1. Initial supported semantics are limited to temporal precedence and conservative contribution/recovery relationships.
6. Official replay result remains visible, while strategic result is explicitly `NOT_EVALUATED` in this change.

## Affected capabilities

- deterministic match understanding;
- narrative analysis;
- report timeline visualization;
- REST analysis contract;
- support bundle;
- React report;
- Markdown report.

## Relationship to existing engines

Existing engines remain owners of their facts and detections. Narrative Analysis consumes their outputs through normalization adapters and does not duplicate their algorithms.

```text
Context / Decision / Turning Point / Combat / Information / Knowledge
                              |
                              v
                    Narrative normalization
                              |
                              v
       state timeline -> transitions -> phases -> causal chain
                              |
                              v
                 REST / React / Markdown / bundle
```

## Risks

- Phase segmentation may become arbitrary if thresholds are hidden or overfitted.
- Too many chart series may make the report unreadable.
- Temporal proximity may be mistaken for causality.
- Existing time-series inputs may have different sampling rates.
- Team matches may expose differences between player and team state that V1 cannot fully interpret.
- Narrative output may regress into another collection of independent cards.

## Mitigations

- Centralize and version thresholds in explicit configuration.
- Keep phase-boundary reasons and transition evidence visible.
- Restrict initial causal-link types and confidence semantics.
- Normalize time-series samples before narrative processing.
- Preserve player and team identifiers even where V1 remains player-focused.
- Require one ordered narrative timeline and one principal chain as acceptance criteria.

## Rollout

Implement as one APPLY change on a branch created directly from current `develop`. Keep existing report sections available beneath the new narrative section. No existing API fields are removed.

Follow-up changes are expected for:

1. advantage and recovery analysis;
2. collapse and strategic-result inference;
3. full team narrative;
4. narrative-report refinement.
