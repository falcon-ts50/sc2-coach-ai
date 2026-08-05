package ai.sc2coach.domain.information;

import java.time.Duration;
import java.util.List;

public record InformationEpisode(
        String scoutPlayer,
        String targetPlayer,
        Integer targetTeam,
        String scoutUnit,
        Duration start,
        Duration end,
        boolean survived,
        InformationConfidence confidence,
        List<InformationObservation> potentiallyObserved,
        List<InformationGap> missingInformation,
        List<InformationReaction> reactionCandidates
) {
    public InformationEpisode {
        if (start == null) start = Duration.ZERO;
        if (end == null) end = start;
        if (end.compareTo(start) < 0) throw new IllegalArgumentException("end must be >= start");
        if (confidence == null) confidence = InformationConfidence.of(0.0, "missing confidence");
        potentiallyObserved = potentiallyObserved == null ? List.of() : List.copyOf(potentiallyObserved);
        missingInformation = missingInformation == null ? List.of() : List.copyOf(missingInformation);
        reactionCandidates = reactionCandidates == null ? List.of() : List.copyOf(reactionCandidates);
    }
}
