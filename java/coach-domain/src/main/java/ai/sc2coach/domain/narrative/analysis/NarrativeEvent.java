package ai.sc2coach.domain.narrative.analysis;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record NarrativeEvent(
        String id,
        Kind kind,
        Duration at,
        Duration endedAt,
        String player,
        Integer playerPid,
        String title,
        String source,
        double confidence,
        List<String> evidenceRefs,
        Map<String, Object> attributes
) {
    public NarrativeEvent {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        kind = kind == null ? Kind.UNKNOWN : kind;
        at = at == null ? Duration.ZERO : at;
        endedAt = endedAt == null ? at : endedAt;
        title = title == null ? "" : title;
        source = source == null ? "unknown" : source;
        confidence = Double.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0;
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public enum Kind {
        COMBAT,
        TURNING_POINT,
        DECISION,
        RECOMMENDATION,
        INFORMATION,
        STATE_TRANSITION,
        UNKNOWN
    }
}
