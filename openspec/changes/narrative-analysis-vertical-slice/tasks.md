# Tasks: Narrative Analysis Vertical Slice

Lifecycle gate for implementation: `APPLY`

## Read Gate

- [ ] Read `openspec/AGENTS.md` and complete the mandatory Read Gate report.
- [ ] Read `openspec/project.md`, `docs/PROJECT_STATE.md`, `docs/DECISIONS.md`, `ROADMAP.md`, and `ARCHITECTURE.md`.
- [ ] Read this entire change directory and every referenced existing change.
- [ ] Inspect relevant current implementation, tests, open PRs and the fixed benchmark support bundle.
- [ ] Confirm the branch is based directly on current `origin/develop` and the PR will target `develop`.

## 1. Domain contracts

- [ ] Add deterministic domain contracts for `NarrativeEvent`, `MatchStateSnapshot`, `StateTransition`, `MatchPhase`, `CausalLink`, `NarrativeTimeline`, `NarrativeSummary`, and `NarrativeChartModel` in `java/coach-domain`.
- [ ] Preserve source engine references, evidence, derivation type, confidence and limitations.
- [ ] Add `strategicResultStatus = NOT_EVALUATED`; do not infer strategic result in this change.
- [ ] Add centralized versioned `NarrativeAnalysisConfig`.

## 2. Input normalization and timeline

- [ ] Normalize context, decision, turning-point, combat, information and recommendation outputs without duplicating their detection logic.
- [ ] Define deterministic time-grid and missing-sample behaviour.
- [ ] Preserve player/team identities and stable IDs.
- [ ] Add unit tests for stable ordering and incomplete inputs.

## 3. State-transition detection

- [ ] Implement explainable before/after state transitions for material army, economy, supply and pressure changes.
- [ ] Document actual thresholds and rationale in `design.md`.
- [ ] Ensure isolated commands/deaths do not create unsupported transitions.
- [ ] Add positive and negative tests.

## 4. Match-phase segmentation

- [ ] Segment by material state changes, combat/turning-point evidence and stabilization, not fixed-duration buckets.
- [ ] Emit entry/exit states, phase-boundary reason, evidence, confidence and limitations.
- [ ] Merge adjacent equivalent phase candidates deterministically.
- [ ] Add regression tests for phase ordering and boundary stability.

## 5. Minimal causal-link assembly

- [ ] Implement `PRECEDED`, `CONTRIBUTED_TO`, `ENABLED`, and `RECOVERED_FROM`.
- [ ] Enforce that temporal proximity alone yields only `PRECEDED`.
- [ ] Prevent unsupported intent, vision and certainty claims.
- [ ] Select a principal chain only when at least two defensible links exist.
- [ ] Add wording and confidence tests.

## 6. REST and support bundle

- [ ] Add an additive versioned narrative payload to the analysis response.
- [ ] Include timeline, summary, chart model, evidence and limitations in the support bundle.
- [ ] Preserve all existing API fields and report sections.
- [ ] Add serialization and compatibility tests.

## 7. React report and chart

- [ ] Add the narrative section near the top of the report: verdict, chart, phases, causal chain, then existing detail.
- [ ] Render a synchronized match-overview line chart.
- [ ] Show army value, workers/economy proxy and supply by default.
- [ ] Provide optional bases and production-structure series when data exists.
- [ ] Overlay combat markers, turning points and phase intervals/boundaries.
- [ ] Synchronize phase selection with the chart interval.
- [ ] Keep frontend logic presentational; do not infer phases or causality in React.
- [ ] Cover the narrative layout and interactions with automated frontend tests where the current frontend test stack supports them.

## 8. Markdown parity

- [ ] Render the same preliminary verdict, ordered phases and principal causal chain.
- [ ] Include phase ranges and the key before/after values that substitute for interactive chart inspection.
- [ ] Include evidence, confidence, limitations and `strategic result: not evaluated`.

## 9. Fixed-bundle regression coverage

- [ ] Use the project's existing benchmark support bundle only.
- [ ] Add deterministic fixture or integration assertions for the expected semantics below.
- [ ] Assert the structured narrative payload, chart series, phases, causal links and Markdown output without requiring a separate browser session.
- [ ] Record material deviations in the implementation PR; do not weaken the expected semantics to match implementation output.

GitHub Actions executes all test suites and build jobs for this change. The implementation agent must add or update the required automated tests, but must not run local Python suites, Maven verification, frontend production builds or Docker image builds unless the user explicitly requests it.

## Expected result on the website

The implementation SHALL use the same fixed benchmark support bundle already used by the project. Another replay is not an acceptable substitute for regression coverage.

The criteria below define required product behaviour. They SHALL be verified through automated tests and generated artifacts wherever practical; this change does not require a separate manual browser-verification stage or a separate tester agent.

### 1. Report placement and structure

The report presents, near the top and before the existing detailed cards:

1. official replay result and Narrative Analysis status;
2. a preliminary narrative verdict;
3. the match-overview graph;
4. chronological phases;
5. one principal causal chain;
6. the existing detailed sections.

Acceptance fails if the feature appears only as additional independent cards without an ordered narrative.

### 2. Result semantics

For dragonDriver, the report continues to show the official replay result as a win.

The report also shows:

- `Strategic result: Not evaluated` or an equivalent explicit status;
- no claim that the official win proves a favourable strategic end state;
- no inferred strategic defeat yet, because that belongs to the later collapse/strategic-result change.

### 3. Chronological phases

The timeline identifies evidence-supported phases approximately equivalent to:

- technology-heavy opening and limited early combat mass;
- early mobile-army/Reaper loss and subsequent pressure;
- defensive adaptation using Hellion, Marine and Siege Tank evidence where present;
- stabilization and recovery;
- competitive or improved mid-game state;
- deterioration after the recovered period;
- late severe decline.

Exact labels and boundaries may differ, but the ordering and state changes must be recognizable, deterministic and evidence-backed.

### 4. Match-overview graph

A readable time-series graph is present in the primary report.

Default visible series:

- dragonDriver army value;
- worker count or the documented economy proxy;
- supply.

Required overlays:

- combat markers;
- narrative phase intervals or boundaries;
- turning-point markers where the source output provides them.

Bases and production structures are available as optional series if sufficiently complete. Incomplete series are labelled as such and are not silently interpolated.

The structured chart payload exposes series names, units, sample interval, completeness and notable values around accepted phase boundaries so these properties can be asserted without visual inspection.

### 5. Graph and narrative synchronization

Selecting or expanding a phase highlights the corresponding graph interval. The phase-to-chart relationship is represented through stable IDs and time ranges in the API contract and covered by automated frontend or contract tests.

Responsive behaviour SHALL preserve access to series selection, labels and evidence. A separate manual mobile-browser sign-off is not required by this change.

### 6. Early decline

The narrative identifies a measurable early deterioration associated with the loss of early mobile combat strength/Reapers and later pressure.

The causal wording remains conservative:

- confirmed ordering may use `preceded`;
- a supported hypothesis may use `contributed to`;
- the system must not state that the loss certainly caused the opponent's attack or prove player intent.

Regression assertions include the transition time range, before/after army values and combat evidence references.

### 7. Successful adaptation and recovery

The report explicitly recognizes a later improvement or stabilization rather than describing the whole match as continuous failure.

Where supported by the bundle, it connects Hellion, Marine and Siege Tank changes to the defensive adaptation using `ENABLED`, `RECOVERED_FROM`, or cautious equivalent semantics.

Regression assertions include the recovery transition, supporting units, values, timestamps and confidence.

### 8. Mid-game improvement

The report shows a competitive or favourable trajectory during the recovered mid-game, approximately within the previously reviewed 12:00–16:00 region when the actual data supports it.

This change does not label it a formal `AdvantageWindow` and does not claim a guaranteed win. The chart data and phase evidence must nevertheless make the improvement explicit.

### 9. Late deterioration

The narrative and graph show a clear late decline after the recovered period, including the strongest available army-value and supply evidence.

The section may describe severe deterioration or late decline, but it must not yet infer strategic defeat, production collapse or inability to recover unless those later capabilities have been implemented through a separate approved change.

### 10. Principal causal chain

The report displays one ordered principal chain broadly equivalent to:

```text
early army loss
→ increased vulnerability / subsequent pressure
→ defensive adaptation
→ stabilization and recovery
→ improved mid-game state
→ later deterioration
```

Each arrow exposes:

- link type;
- confidence;
- evidence;
- limitations.

Acceptance fails if temporal proximity is represented as proven causality.

### 11. Team context

The report identifies Lulu as dragonDriver's teammate and retains team context in relevant phases and evidence.

V1 may remain player-focused, but it must not misidentify Lulu as an opponent or present the 2v2 as four unrelated 1v1 matches.

### 12. Evidence and uncertainty

Every major phase and principal-chain link exposes timestamps and source evidence.

The report explicitly preserves uncertainty about:

- exact player vision;
- player intent;
- exact army positioning when coordinates are incomplete;
- whether scouting caused a later decision;
- whether one event exclusively caused another.

### 13. Markdown and support-bundle parity

Downloaded Markdown contains the same:

- preliminary verdict;
- ordered phase list;
- principal causal chain;
- phase ranges;
- key before/after graph values;
- evidence and uncertainty;
- strategic-result `NOT_EVALUATED` status.

The support-bundle JSON contains the structured narrative timeline and chart model used by the report.

### 14. No regressions

Existing combat, context, recommendations, support-bundle generation, rebuild-for-player flow and report downloads still function.

No existing result is silently removed or replaced by the narrative section.
