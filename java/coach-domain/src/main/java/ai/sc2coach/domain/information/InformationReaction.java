package ai.sc2coach.domain.information;

import java.time.Duration;

public record InformationReaction(
        String player,
        String action,
        Duration time,
        long delaySeconds,
        String basis,
        InformationConfidence confidence
) {
    public InformationReaction {
        if (confidence == null) confidence = InformationConfidence.of(0.0, "missing confidence");
    }
}
