package ai.sc2coach.domain.knowledge;

import ai.sc2coach.domain.decision.Evidence;

import java.util.List;
import java.util.Objects;

public record Recommendation(
        String id,
        Category category,
        Priority priority,
        double confidence,
        String title,
        String explanation,
        String nextAction,
        List<Evidence> evidence
) {
    public Recommendation {
        id = Objects.requireNonNull(id, "id");
        category = category == null ? Category.GENERAL : category;
        priority = priority == null ? Priority.MEDIUM : priority;
        if (Double.isNaN(confidence) || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
        title = Objects.requireNonNull(title, "title");
        explanation = Objects.requireNonNull(explanation, "explanation");
        nextAction = Objects.requireNonNull(nextAction, "nextAction");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public enum Category { ARMY, ECONOMY, MACRO, TECH, RECOVERY, GENERAL }
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
}
