package ai.sc2coach.domain.narrative.analysis;

import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Completeness;

import java.time.Duration;
import java.util.List;

public record NarrativeDashboard(
        String schemaVersion,
        List<SummaryMetric> summaryMetrics,
        List<EvidenceEpisode> evidenceEpisodes,
        List<String> limitations
) {
    public NarrativeDashboard {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? "narrative-dashboard.v1" : schemaVersion;
        summaryMetrics = summaryMetrics == null ? List.of() : List.copyOf(summaryMetrics);
        evidenceEpisodes = evidenceEpisodes == null ? List.of() : List.copyOf(evidenceEpisodes);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public static NarrativeDashboard empty() {
        return new NarrativeDashboard("narrative-dashboard.v1", List.of(), List.of(), List.of());
    }

    public record SummaryMetric(
            String id,
            String label,
            String value,
            String comparisonValue,
            String unit,
            Duration at,
            Duration from,
            Duration to,
            Completeness completeness,
            List<String> limitations
    ) {
        public SummaryMetric {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            label = label == null || label.isBlank() ? id : label;
            value = value == null ? "" : value;
            comparisonValue = comparisonValue == null ? "" : comparisonValue;
            unit = unit == null ? "" : unit;
            at = at == null ? Duration.ZERO : at;
            from = from == null ? at : from;
            to = to == null ? from : to;
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }
    }

    public record EvidenceEpisode(
            String id,
            int ordinal,
            String title,
            String category,
            Duration startedAt,
            Duration endedAt,
            double importance,
            double confidence,
            Completeness completeness,
            String summary,
            List<EpisodeMetricDelta> metricDeltas,
            List<String> relatedMatchFlowIntervalIds,
            List<String> relatedCombatIds,
            List<String> relatedTurningPointIds,
            List<String> limitations
    ) {
        public EvidenceEpisode {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            ordinal = Math.max(0, ordinal);
            title = title == null || title.isBlank() ? id : title;
            category = category == null || category.isBlank() ? "episode" : category;
            startedAt = startedAt == null ? Duration.ZERO : startedAt;
            endedAt = endedAt == null ? startedAt : endedAt;
            importance = clamp01(importance);
            confidence = clamp01(confidence);
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            summary = summary == null ? "" : summary;
            metricDeltas = metricDeltas == null ? List.of() : List.copyOf(metricDeltas);
            relatedMatchFlowIntervalIds = relatedMatchFlowIntervalIds == null ? List.of() : List.copyOf(relatedMatchFlowIntervalIds);
            relatedCombatIds = relatedCombatIds == null ? List.of() : List.copyOf(relatedCombatIds);
            relatedTurningPointIds = relatedTurningPointIds == null ? List.of() : List.copyOf(relatedTurningPointIds);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }
    }

    public record EpisodeMetricDelta(
            String participantId,
            String label,
            double startValue,
            double endValue,
            double delta,
            String unit,
            Completeness completeness
    ) {
        public EpisodeMetricDelta {
            participantId = participantId == null ? "" : participantId;
            label = label == null || label.isBlank() ? "Метрика" : label;
            startValue = finite(startValue);
            endValue = finite(endValue);
            delta = finite(delta);
            unit = unit == null ? "" : unit;
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
        }
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0;
    }

    private static double clamp01(double value) {
        return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }
}
