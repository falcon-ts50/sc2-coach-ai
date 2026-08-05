package ai.sc2coach.domain.episode;

import java.time.Duration;
import java.util.List;

public record Episode(
        Type type,
        Duration from,
        Duration to,
        String actor,
        String title,
        double importance,
        List<String> evidence
) {
    public Episode {
        from = from == null ? Duration.ZERO : from;
        to = to == null ? from : to;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public enum Type {
        OPENING,
        LEAD_CHANGE,
        MAJOR_FIGHT,
        RECOVERY,
        EXPANSION,
        TECH_TRANSITION,
        OTHER
    }
}
