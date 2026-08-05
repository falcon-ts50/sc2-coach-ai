package ai.sc2coach.domain.information;

import java.time.Duration;

public record InformationObservation(
        String type,
        String subject,
        Duration time,
        InformationPoint coordinates,
        double distance,
        InformationConfidence confidence
) {
    public InformationObservation {
        if (confidence == null) confidence = InformationConfidence.of(0.0, "missing confidence");
    }
}
