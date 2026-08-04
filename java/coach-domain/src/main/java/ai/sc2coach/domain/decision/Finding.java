package ai.sc2coach.domain.decision;

import java.util.List;
import java.util.Objects;

public record Finding(
        String id,
        Severity severity,
        String titleKey,
        String explanationKey,
        String recommendationKey,
        Decision decision,
        Decision.Confidence confidence,
        List<Evidence> evidence
) {
    public Finding {
        id = Objects.requireNonNull(id, "id");
        severity = severity == null ? Severity.INFO : severity;
        titleKey = Objects.requireNonNull(titleKey, "titleKey");
        explanationKey = Objects.requireNonNull(explanationKey, "explanationKey");
        recommendationKey = Objects.requireNonNull(recommendationKey, "recommendationKey");
        confidence = confidence == null ? Decision.Confidence.unknown() : confidence;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public enum Severity {
        INFO,
        MINOR,
        MAJOR,
        CRITICAL
    }
}
