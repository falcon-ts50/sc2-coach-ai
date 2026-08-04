package ai.sc2coach.domain.decision;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Decision(
        String id,
        int playerPid,
        Type type,
        Duration startedAt,
        Duration endedAt,
        Confidence confidence,
        List<Evidence> evidence,
        Map<String, Object> attributes
) {
    public Decision {
        id = Objects.requireNonNull(id, "id");
        type = Objects.requireNonNull(type, "type");
        startedAt = startedAt == null ? Duration.ZERO : startedAt;
        confidence = confidence == null ? Confidence.unknown() : confidence;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public enum Type {
        EXPAND,
        ATTACK,
        RETREAT,
        DEFEND,
        HARASS,
        SCOUT,
        TECH_SWITCH,
        TIMING_PUSH,
        REBUILD,
        GREED,
        TURTLE,
        ALL_IN,
        UNKNOWN
    }

    public record Confidence(double value, Basis basis) {
        public Confidence {
            if (Double.isNaN(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("confidence must be between 0 and 1");
            }
            basis = basis == null ? Basis.HEURISTIC : basis;
        }

        public static Confidence unknown() { return new Confidence(0, Basis.INSUFFICIENT_DATA); }

        public enum Basis {
            DIRECT_EVENT,
            DETERMINISTIC_RULE,
            HEURISTIC,
            STATISTICAL_MODEL,
            INSUFFICIENT_DATA
        }
    }
}
