# SC2 Coach Roadmap

## Vision

SC2 Coach is an open-source StarCraft II replay analysis system focused on explaining why games were won or lost, not merely displaying statistics.

Every conclusion should be:

- reproducible from replay-derived facts;
- explainable to the player;
- deterministic in the core analysis pipeline;
- usable offline through the CLI;
- reusable by future web and API surfaces.

## Architecture

```text
SC2Replay
    |
    v
Replay Decoder
    |
    v
Replay Analysis Model
    |
    +-------------------+
    |                   |
    v                   v
Engagement Engine   Strategic Engine
    |                   |
    +---------+---------+
              |
              v
         Report Model
        /      |      \
       v       v       v
     JSON      PDF     HTML
```

JSON remains the machine-readable source of truth. Markdown, PDF, HTML, and future UI views are renderings of the same report model.

## Design principles

### Evidence before advice

Every recommendation must include the facts that caused it, such as timestamps, resource values, army-value changes, worker losses, or engagement outcomes.

### Stable machine contracts

JSON field names, categories, severities, metrics, and rule identifiers remain language-independent and stable across renderers.

### Explicit processing

A replay is analyzed only after a user explicitly supplies or uploads it. Background directory watching is not a product goal.

### Deterministic core

The core engine must not depend on nondeterministic LLM output. Optional AI interpretation may be added later as a separate layer over structured evidence.

### Separation of concerns

Replay decoding, engagement detection, strategic rules, report composition, and output rendering must remain independent modules.

## Version roadmap

### Completed

- **v0.1 — Replay Decoder:** structured replay extraction.
- **v0.2 — Charts:** economy, army, income, bank, and loss visualizations.
- **v0.3 — Engagement Engine:** battles, skirmishes, harassment, segmentation, and diagnostics.
- **v0.4 — Coach Engine:** explainable strategic rules with English and Russian reports.

### v0.5 — Professional Report Engine

**Goal:** produce a polished, coach-ready PDF from the existing analysis outputs.

**Definition of Done**

- a renderer-independent `ReportDocument` model;
- deterministic PDF generation;
- English and Russian output;
- cover page and match metadata;
- executive summary;
- strategic findings and coach plan;
- engagement summary and tables;
- embedded high-resolution charts;
- table of contents and page numbering;
- PDF included in the review bundle;
- tests for report composition and basic PDF generation.

**Future extensions**

- HTML renderer using the same report model;
- compact four-page report mode;
- shareable replay-card PNG.

### v0.6 — Build Order Intelligence

**Goal:** compare a player's build against curated reference replays.

**Definition of Done**

- versioned reference-pack format;
- matching by race, matchup, map context, and opening family;
- comparison of structures, upgrades, workers, army milestones, and key units;
- configurable timing tolerances;
- phase-based similarity score;
- evidence-backed explanations of material deviations;
- support for professional, league-average, and custom reference packs.

### v0.7 — Personal Replay Database

**Goal:** compare current performance with the player's own historical games.

**Definition of Done**

- local replay index and metadata database;
- personal baselines by race and matchup;
- trend charts for macro, timings, engagements, and outcomes;
- comparison against recent wins and personal best games;
- regression detection and improvement tracking.

### v1.0 — Coach Portal

**Goal:** provide an explicit upload-and-analyze web workflow.

**Definition of Done**

- replay upload;
- player selection;
- analysis job execution;
- browser report and charts;
- PDF download;
- replay history;
- safe resource limits and file validation.

## Reference packs

Reference data should be versioned and auditable.

```text
references/
  terran/
    tvt/
      battlecruiser/
        manifest.json
        replays/
        profiles/
```

A manifest should record source, patch, league or player, matchup, map, replay checksum, extraction schema, and licensing or redistribution constraints.

## Plugin direction

Future race- or strategy-specific rules should implement a stable interface rather than modifying the core engine directly.

Conceptual API:

```python
class CoachRule:
    rule_id: str

    def applies(self, context) -> bool: ...
    def evaluate(self, context): ...
```

Potential plugins include inject efficiency, chrono usage, marine splitting, Battlecruiser control, scouting quality, drop defense, and race-specific build-order rules.

## Non-goals

SC2 Coach does not aim to:

- predict winners using opaque probabilities;
- process replays without explicit user action;
- invent events or intent not supported by replay data;
- hide the evidence behind recommendations;
- replace professional coaching;
- make an LLM part of the deterministic analysis core.

## Future ideas

- opening recognition;
- scouting evaluation;
- cheese detection;
- camera and attention analysis;
- spatial engagement clustering;
- drop and harassment analysis;
- APM and command-efficiency heatmaps;
- optional AI narrative generation over structured evidence;
- replay search and cohort analysis.
