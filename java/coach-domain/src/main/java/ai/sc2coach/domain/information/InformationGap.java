package ai.sc2coach.domain.information;

public record InformationGap(
        String topic,
        String reason,
        InformationConfidence confidence
) {
    public InformationGap {
        if (confidence == null) confidence = InformationConfidence.of(0.0, "missing confidence");
    }
}
