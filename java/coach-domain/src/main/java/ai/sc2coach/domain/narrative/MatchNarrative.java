package ai.sc2coach.domain.narrative;

import java.time.Duration;
import java.util.List;

public record MatchNarrative(
        String summary,
        List<Beat> beats
) {
    public MatchNarrative {
        beats = beats == null ? List.of() : List.copyOf(beats);
    }

    public record Beat(
            Duration at,
            Kind kind,
            String title,
            String statement,
            List<String> evidence
    ) {
        public Beat {
            at = at == null ? Duration.ZERO : at;
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }
    }

    public enum Kind { OPENING, ADVANTAGE, FIGHT, RECOVERY, COLLAPSE, TRANSITION, CONCLUSION }
}
