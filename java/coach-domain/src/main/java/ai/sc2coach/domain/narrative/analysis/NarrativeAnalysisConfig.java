package ai.sc2coach.domain.narrative.analysis;

import java.time.Duration;

public record NarrativeAnalysisConfig(
        String version,
        Duration earlyPhaseMax,
        Duration midPhaseMax,
        double armySwingThreshold,
        double economySwingThreshold,
        double supplySwingThreshold,
        double overallSwingThreshold,
        double phaseConfidence,
        double causalLinkConfidence
) {
    public NarrativeAnalysisConfig {
        version = version == null || version.isBlank() ? "narrative-analysis-config.v1" : version;
        earlyPhaseMax = earlyPhaseMax == null ? Duration.ofMinutes(7) : earlyPhaseMax;
        midPhaseMax = midPhaseMax == null ? Duration.ofMinutes(16) : midPhaseMax;
        armySwingThreshold = positive(armySwingThreshold, 300);
        economySwingThreshold = positive(economySwingThreshold, 350);
        supplySwingThreshold = positive(supplySwingThreshold, 12);
        overallSwingThreshold = positive(overallSwingThreshold, 18);
        phaseConfidence = bounded(phaseConfidence, 0.68);
        causalLinkConfidence = bounded(causalLinkConfidence, 0.64);
    }

    public static NarrativeAnalysisConfig defaults() {
        return new NarrativeAnalysisConfig(
                "narrative-analysis-config.v1",
                Duration.ofMinutes(7),
                Duration.ofMinutes(16),
                300,
                350,
                12,
                18,
                0.68,
                0.64
        );
    }

    private static double positive(double value, double fallback) {
        return Double.isFinite(value) && value > 0 ? value : fallback;
    }

    private static double bounded(double value, double fallback) {
        if (!Double.isFinite(value) || value < 0 || value > 1) return fallback;
        return value;
    }
}
