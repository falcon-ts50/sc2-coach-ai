package ai.sc2coach.domain.narrative.analysis;

import java.time.Duration;
import java.util.List;

public record MatchPhase(
        String id,
        Kind kind,
        String title,
        Duration startedAt,
        Duration endedAt,
        String summary,
        String entrySnapshotId,
        String exitSnapshotId,
        List<String> transitionIds,
        List<String> eventIds,
        double confidence,
        List<String> limitations
) {
    public MatchPhase {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        kind = kind == null ? Kind.UNKNOWN : kind;
        title = title == null ? "" : title;
        startedAt = startedAt == null ? Duration.ZERO : startedAt;
        endedAt = endedAt == null ? startedAt : endedAt;
        summary = summary == null ? "" : summary;
        transitionIds = transitionIds == null ? List.of() : List.copyOf(transitionIds);
        eventIds = eventIds == null ? List.of() : List.copyOf(eventIds);
        confidence = Double.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0;
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public enum Kind {
        OPENING,
        PRESSURE,
        STABILIZATION,
        MIDGAME,
        DETERIORATION,
        CLOSING,
        UNKNOWN
    }
}
