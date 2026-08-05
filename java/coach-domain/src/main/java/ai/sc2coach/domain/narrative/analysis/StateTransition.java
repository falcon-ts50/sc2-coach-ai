package ai.sc2coach.domain.narrative.analysis;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record StateTransition(
        String id,
        Kind kind,
        Duration from,
        Duration to,
        String beforeSnapshotId,
        String afterSnapshotId,
        Map<String, Double> metricDelta,
        String interpretation,
        double confidence,
        List<String> evidenceRefs,
        List<String> limitations
) {
    public StateTransition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        kind = kind == null ? Kind.UNKNOWN : kind;
        from = from == null ? Duration.ZERO : from;
        to = to == null ? from : to;
        metricDelta = metricDelta == null ? Map.of() : Map.copyOf(metricDelta);
        interpretation = interpretation == null ? "" : interpretation;
        confidence = Double.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0;
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public enum Kind {
        EARLY_DECLINE,
        DEFENSIVE_ADAPTATION,
        MIDGAME_IMPROVEMENT,
        LATE_DETERIORATION,
        STABLE,
        UNKNOWN
    }
}
