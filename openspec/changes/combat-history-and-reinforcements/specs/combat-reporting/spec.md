# Combat Reporting Capability Specification

## Status

Proposed for APPLY.

## Requirement: Chronological combat history

The report SHALL expose all production-detected engagements as one deterministic chronological history.

### Scenario: Multiple engagements are detected

- GIVEN a replay with multiple detected engagements;
- WHEN the report is generated;
- THEN each engagement appears exactly once in chronological order with a stable identity, ordinal label and timestamp/time range.

### Scenario: Report is rebuilt

- GIVEN the same replay, selected perspective and configuration;
- WHEN the report is rebuilt;
- THEN engagement identity, count and ordering remain semantically identical.

## Requirement: Explicit participant state transition

Each engagement participant SHALL expose army state at the beginning and end of the engagement plus additions and categorized losses during the interval.

### Scenario: Units are added during combat

- GIVEN a player starts an engagement with fewer units of a type than are lost during the interval;
- AND additional units of that type become available during the interval;
- WHEN the participant card is produced;
- THEN the additions are displayed separately so the transition is not presented as an unexplained contradiction.

### Scenario: No additions occur

- GIVEN no supported additions occur during an engagement;
- WHEN the participant card is produced;
- THEN the additions row displays a known empty state and the rest of the card remains visible.

## Requirement: Neutral reinforcement semantics

The report SHALL NOT claim that units produced or completed during the engagement physically participated unless local participation evidence supports that claim.

### Scenario: Production is known but arrival is not

- GIVEN a unit completed during the engagement interval;
- AND its presence in the combat region cannot be confirmed;
- WHEN the report renders the addition;
- THEN wording identifies it as an addition or available reinforcement, not a confirmed battlefield participant.

## Requirement: Reconciliation or explicit degradation

For each player and unit type, the report SHALL either provide an explainable start-to-end transition or mark reconciliation as incomplete.

### Scenario: Lifecycle evidence is complete

- GIVEN complete creation, transformation, ownership and death evidence;
- WHEN accounting is performed;
- THEN beginning state, additions, losses, corrections and ending state reconcile.

### Scenario: Lifecycle evidence is incomplete

- GIVEN missing or contradictory lifecycle evidence;
- WHEN accounting is performed;
- THEN the report records partial/incomplete status and does not silently fabricate a balancing value.

## Requirement: Separate loss categories

The report SHALL retain distinct categories for combat units, workers, infrastructure and static defence.

### Scenario: Worker dies during engagement

- GIVEN a worker is killed during an engagement;
- WHEN participant losses are shown;
- THEN the worker appears under worker losses and not in combat army composition.

### Scenario: Building or static defence dies

- GIVEN infrastructure or static defence is destroyed;
- WHEN participant losses are shown;
- THEN each appears in its own established category.

## Requirement: Known zero versus unavailable evidence

The report SHALL distinguish a measured zero/empty category from unavailable evidence.

### Scenario: No structures were lost

- GIVEN complete evidence shows zero structure losses;
- WHEN the report renders structure losses;
- THEN it displays a known empty state such as `нет`.

### Scenario: Structure evidence is unavailable

- GIVEN structure-loss evidence is incomplete;
- WHEN the report renders structure losses;
- THEN it displays `нет данных`, `неполные данные` or an equivalent unavailable state.

## Requirement: Cross-output consistency

Browser, Markdown and support-bundle representations SHALL use the same engagement ordering and accounting semantics.

### Scenario: Fixed replay is exported

- GIVEN the fixed validation replay;
- WHEN browser, Markdown and support-bundle outputs are produced;
- THEN fight count, timestamps, participants, additions and categorized losses agree semantically.

## Requirement: Mobile readability

The combat history SHALL remain usable on an iPhone-sized viewport without required information being clipped or requiring horizontal scrolling.

### Scenario: Participant card contains all categories

- GIVEN a participant card with army, additions, four loss categories and end state;
- WHEN rendered on a narrow viewport;
- THEN labels and values wrap vertically and remain readable.