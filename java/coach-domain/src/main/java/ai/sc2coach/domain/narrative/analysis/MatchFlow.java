package ai.sc2coach.domain.narrative.analysis;

import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Completeness;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.CombatEvidence;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MatchFlow(
        String schemaVersion,
        Duration matchStartedAt,
        Duration matchEndedAt,
        List<MatchFlowInterval> intervals,
        List<String> overviewCombatIds,
        List<String> limitations
) {
    public MatchFlow {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? "match-flow.v1" : schemaVersion;
        matchStartedAt = matchStartedAt == null ? Duration.ZERO : matchStartedAt;
        matchEndedAt = matchEndedAt == null ? matchStartedAt : matchEndedAt;
        intervals = intervals == null ? List.of() : List.copyOf(intervals);
        overviewCombatIds = overviewCombatIds == null ? List.of() : List.copyOf(overviewCombatIds);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public static MatchFlow empty() {
        return new MatchFlow("match-flow.v1", Duration.ZERO, Duration.ZERO, List.of(), List.of(), List.of());
    }

    public record MatchFlowInterval(
            String id,
            int ordinal,
            Kind kind,
            String title,
            Duration startedAt,
            Duration endedAt,
            double confidence,
            Completeness completeness,
            String summary,
            List<String> snapshotIds,
            List<String> transitionIds,
            List<String> eventIds,
            List<String> evidenceIds,
            List<String> combatIds,
            Map<String, IntervalMetrics> startMetricsByParticipantId,
            Map<String, IntervalMetrics> endMetricsByParticipantId,
            IntervalDelta delta,
            IntervalDrilldown drilldown,
            List<String> limitations
    ) {
        public MatchFlowInterval {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            ordinal = Math.max(0, ordinal);
            kind = kind == null ? Kind.LOW_EVIDENCE : kind;
            title = title == null || title.isBlank() ? kind.name() : title;
            startedAt = startedAt == null ? Duration.ZERO : startedAt;
            endedAt = endedAt == null ? startedAt : endedAt;
            confidence = Double.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0;
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            summary = summary == null ? "" : summary;
            snapshotIds = snapshotIds == null ? List.of() : List.copyOf(snapshotIds);
            transitionIds = transitionIds == null ? List.of() : List.copyOf(transitionIds);
            eventIds = eventIds == null ? List.of() : List.copyOf(eventIds);
            evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
            combatIds = combatIds == null ? List.of() : List.copyOf(combatIds);
            startMetricsByParticipantId = immutableMap(startMetricsByParticipantId);
            endMetricsByParticipantId = immutableMap(endMetricsByParticipantId);
            delta = delta == null ? IntervalDelta.empty() : delta;
            drilldown = drilldown == null ? IntervalDrilldown.empty() : drilldown;
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }
    }

    public record IntervalMetrics(
            double armyValue,
            double economyProxy,
            double supplyUsed,
            Completeness completeness,
            List<String> sourceSnapshotIds
    ) {
        public IntervalMetrics {
            armyValue = finite(armyValue);
            economyProxy = finite(economyProxy);
            supplyUsed = finite(supplyUsed);
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            sourceSnapshotIds = sourceSnapshotIds == null ? List.of() : List.copyOf(sourceSnapshotIds);
        }

        public static IntervalMetrics unavailable() {
            return new IntervalMetrics(0, 0, 0, Completeness.UNAVAILABLE, List.of());
        }
    }

    public record IntervalDelta(
            Map<String, MetricDelta> byParticipantId,
            Completeness completeness,
            List<String> limitations
    ) {
        public IntervalDelta {
            byParticipantId = immutableMap(byParticipantId);
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }

        public static IntervalDelta empty() {
            return new IntervalDelta(Map.of(), Completeness.UNAVAILABLE, List.of());
        }
    }

    public record MetricDelta(
            double armyValueDelta,
            double economyProxyDelta,
            double supplyUsedDelta
    ) {
        public MetricDelta {
            armyValueDelta = finite(armyValueDelta);
            economyProxyDelta = finite(economyProxyDelta);
            supplyUsedDelta = finite(supplyUsedDelta);
        }
    }

    public record IntervalDrilldown(
            CombatDrilldown combat,
            DevelopmentDrilldown development,
            List<String> limitations
    ) {
        public IntervalDrilldown {
            combat = combat == null ? CombatDrilldown.empty() : combat;
            development = development == null ? DevelopmentDrilldown.empty() : development;
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }

        public static IntervalDrilldown empty() {
            return new IntervalDrilldown(CombatDrilldown.empty(), DevelopmentDrilldown.empty(), List.of());
        }
    }

    public record CombatDrilldown(
            List<String> combatIds,
            List<CombatEvidence> combats,
            List<String> emptyStates,
            List<String> limitations,
            String summary
    ) {
        public CombatDrilldown {
            combatIds = combatIds == null ? List.of() : List.copyOf(combatIds);
            combats = combats == null ? List.of() : List.copyOf(combats);
            emptyStates = emptyStates == null ? List.of() : List.copyOf(emptyStates);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
            summary = summary == null ? "" : summary;
        }

        public static CombatDrilldown empty() {
            return new CombatDrilldown(List.of(), List.of(), List.of("Боёв в этом интервале не обнаружено."), List.of(), "");
        }
    }

    public record DevelopmentDrilldown(
            MacroEvidence macro,
            ProductionEvidence production,
            TechEvidence tech,
            ScoutingEvidence scouting,
            PreparationEvidence preparation,
            List<String> emptyStates,
            List<String> limitations
    ) {
        public DevelopmentDrilldown {
            macro = macro == null ? MacroEvidence.empty() : macro;
            production = production == null ? ProductionEvidence.empty() : production;
            tech = tech == null ? TechEvidence.empty() : tech;
            scouting = scouting == null ? ScoutingEvidence.empty() : scouting;
            preparation = preparation == null ? PreparationEvidence.empty() : preparation;
            emptyStates = emptyStates == null ? List.of() : List.copyOf(emptyStates);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }

        public static DevelopmentDrilldown empty() {
            return new DevelopmentDrilldown(MacroEvidence.empty(), ProductionEvidence.empty(), TechEvidence.empty(),
                    ScoutingEvidence.empty(), PreparationEvidence.empty(),
                    List.of("Экономических, производственных, технологических, разведывательных или подготовительных событий в этом интервале не обнаружено."),
                    List.of());
        }
    }

    public record MacroEvidence(
            String summary,
            List<DevelopmentMetric> metrics,
            Completeness completeness,
            List<String> evidenceIds,
            List<String> limitations
    ) {
        public MacroEvidence {
            summary = summary == null ? "" : summary;
            metrics = metrics == null ? List.of() : List.copyOf(metrics);
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }

        public static MacroEvidence empty() {
            return new MacroEvidence("", List.of(), Completeness.UNAVAILABLE, List.of(), List.of());
        }
    }

    public record DevelopmentMetric(
            String metric,
            double startValue,
            double endValue,
            double delta,
            Completeness completeness
    ) {
        public DevelopmentMetric {
            if (metric == null || metric.isBlank()) throw new IllegalArgumentException("metric is required");
            startValue = finite(startValue);
            endValue = finite(endValue);
            delta = finite(delta);
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
        }
    }

    public record ProductionEvidence(List<String> observations, List<String> limitations) {
        public ProductionEvidence {
            observations = observations == null ? List.of() : List.copyOf(observations);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }

        public static ProductionEvidence empty() {
            return new ProductionEvidence(List.of(), List.of());
        }
    }

    public record TechEvidence(List<String> observations, List<String> limitations) {
        public TechEvidence {
            observations = observations == null ? List.of() : List.copyOf(observations);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }

        public static TechEvidence empty() {
            return new TechEvidence(List.of(), List.of());
        }
    }

    public record ScoutingEvidence(List<String> observations, List<String> limitations) {
        public ScoutingEvidence {
            observations = observations == null ? List.of() : List.copyOf(observations);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }

        public static ScoutingEvidence empty() {
            return new ScoutingEvidence(List.of(), List.of());
        }
    }

    public record PreparationEvidence(List<String> observations, List<String> limitations) {
        public PreparationEvidence {
            observations = observations == null ? List.of() : List.copyOf(observations);
            limitations = limitations == null ? List.of() : List.copyOf(limitations);
        }

        public static PreparationEvidence empty() {
            return new PreparationEvidence(List.of(), List.of());
        }
    }

    public enum Kind {
        OPENING_BUILDUP,
        ECONOMIC_GROWTH,
        TECH_TRANSITION,
        ARMY_BUILDUP,
        MAP_CONTROL_OR_SCOUTING,
        PRESSURE_PREPARATION,
        COMBAT,
        RECOVERY,
        REGROUPING_OR_LOW_ACTIVITY,
        LOW_EVIDENCE
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0;
    }

    private static <T> Map<String, T> immutableMap(Map<String, T> value) {
        return value == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
