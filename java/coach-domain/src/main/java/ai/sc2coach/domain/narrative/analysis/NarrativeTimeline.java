package ai.sc2coach.domain.narrative.analysis;

import java.util.List;

public record NarrativeTimeline(
        List<NarrativeEvent> events,
        List<MatchStateSnapshot> snapshots,
        List<StateTransition> transitions,
        List<MatchPhase> phases,
        List<CausalLink> causalLinks
) {
    public NarrativeTimeline {
        events = events == null ? List.of() : List.copyOf(events);
        snapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
        transitions = transitions == null ? List.of() : List.copyOf(transitions);
        phases = phases == null ? List.of() : List.copyOf(phases);
        causalLinks = causalLinks == null ? List.of() : List.copyOf(causalLinks);
    }

    public static NarrativeTimeline empty() {
        return new NarrativeTimeline(List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
