package ai.sc2coach.domain.narrative.analysis;

import java.util.List;

public record CausalLink(
        String id,
        Kind kind,
        String fromId,
        String toId,
        String statement,
        double confidence,
        List<String> evidenceRefs,
        List<String> limitations
) {
    public CausalLink {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        kind = kind == null ? Kind.PRECEDED : kind;
        statement = statement == null ? "" : statement;
        confidence = Double.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0;
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public enum Kind {
        PRECEDED,
        CONTRIBUTED_TO,
        ENABLED,
        RECOVERED_FROM
    }
}
