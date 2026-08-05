package ai.sc2coach.domain.scouting;

import java.time.Duration;
import java.util.List;

public record ScoutingEpisode(
        String player,
        String scoutUnit,
        Duration startedAt,
        Duration endedAt,
        boolean survived,
        List<ObservedFact> potentiallyObserved,
        List<ResponseCandidate> responseCandidates,
        double confidence
) {
    public ScoutingEpisode {
        potentiallyObserved = potentiallyObserved == null ? List.of() : List.copyOf(potentiallyObserved);
        responseCandidates = responseCandidates == null ? List.of() : List.copyOf(responseCandidates);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public record ObservedFact(
            Duration at,
            String opponent,
            String kind,
            String subject,
            Double distance
    ) {}

    public record ResponseCandidate(
            Duration at,
            String action,
            long delaySeconds,
            double confidence
    ) {
        public ResponseCandidate {
            confidence = Math.max(0.0, Math.min(1.0, confidence));
        }
    }
}
