package ai.sc2coach.domain.combat.v3;

import java.time.Duration;

public record CombatClusteringConfig(
        Duration continuationGap,
        double maxSpatialDistance,
        double missingSpatialPenalty
) {
    public CombatClusteringConfig {
        if (continuationGap == null || continuationGap.isNegative() || continuationGap.isZero()) {
            throw new IllegalArgumentException("continuationGap must be positive");
        }
        if (maxSpatialDistance <= 0) throw new IllegalArgumentException("maxSpatialDistance must be positive");
        if (missingSpatialPenalty < 0 || missingSpatialPenalty > 1) {
            throw new IllegalArgumentException("missingSpatialPenalty must be between 0 and 1");
        }
    }

    public static CombatClusteringConfig defaults() {
        return new CombatClusteringConfig(Duration.ofSeconds(12), 28.0, 0.18);
    }
}
