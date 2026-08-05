package ai.sc2coach.domain.combat.v3;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record CombatCluster(
        String id,
        Duration start,
        Duration end,
        CombatRegion region,
        List<String> participants,
        List<CombatEvidence> deaths,
        List<CombatEvidence> combatCommandsAndAbilities,
        double confidence,
        boolean hasMissingSpatialData,
        int evidenceWithoutSpatialData
) {
    public CombatCluster {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.compareTo(start) < 0) throw new IllegalArgumentException("end must be >= start");
        participants = participants == null ? List.of() : List.copyOf(participants);
        deaths = deaths == null ? List.of() : List.copyOf(deaths);
        combatCommandsAndAbilities = combatCommandsAndAbilities == null
                ? List.of()
                : List.copyOf(combatCommandsAndAbilities);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        if (evidenceWithoutSpatialData < 0) {
            throw new IllegalArgumentException("evidenceWithoutSpatialData must be >= 0");
        }
    }

    public MapPoint center() {
        return region == null ? null : region.center();
    }
}
