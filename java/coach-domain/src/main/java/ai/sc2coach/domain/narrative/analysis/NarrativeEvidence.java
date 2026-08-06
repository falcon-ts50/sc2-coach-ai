package ai.sc2coach.domain.narrative.analysis;

import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Completeness;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Point;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record NarrativeEvidence(
        String schemaVersion,
        List<ParticipantIdentity> participants,
        List<MetricComparison> metricComparisons,
        List<EvidenceFocus> focuses,
        List<CombatEvidence> combats,
        List<String> limitations
) {
    public NarrativeEvidence {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? "narrative-evidence.v1"
                : schemaVersion;
        participants = participants == null ? List.of() : List.copyOf(participants);
        metricComparisons = metricComparisons == null ? List.of() : List.copyOf(metricComparisons);
        focuses = focuses == null ? List.of() : List.copyOf(focuses);
        combats = combats == null ? List.of() : List.copyOf(combats);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public static NarrativeEvidence empty() {
        return new NarrativeEvidence("narrative-evidence.v1", List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public record ParticipantIdentity(
            String id,
            int playerId,
            String displayName,
            Integer teamId,
            Relationship relationship,
            boolean selected,
            String styleKey,
            int order
    ) {
        public ParticipantIdentity {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            displayName = displayName == null || displayName.isBlank() ? id : displayName;
            relationship = relationship == null ? Relationship.UNKNOWN : relationship;
            styleKey = styleKey == null || styleKey.isBlank() ? id : styleKey;
        }
    }

    public record MetricComparison(
            String id,
            String label,
            String unit,
            String source,
            Completeness completeness,
            List<ParticipantMetricSeries> series
    ) {
        public MetricComparison {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            label = label == null || label.isBlank() ? id : label;
            unit = unit == null ? "" : unit;
            source = source == null ? "MatchContext" : source;
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            series = series == null ? List.of() : List.copyOf(series);
        }
    }

    public record ParticipantMetricSeries(
            String id,
            String participantId,
            Completeness completeness,
            String lineStyle,
            int strokeWeight,
            List<Point> points
    ) {
        public ParticipantMetricSeries {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            if (participantId == null || participantId.isBlank()) throw new IllegalArgumentException("participantId is required");
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            lineStyle = lineStyle == null || lineStyle.isBlank() ? "solid" : lineStyle;
            strokeWeight = strokeWeight <= 0 ? 2 : strokeWeight;
            points = points == null ? List.of() : List.copyOf(points);
        }
    }

    public record EvidenceFocus(
            String id,
            FocusKind kind,
            String label,
            Duration at,
            Duration from,
            Duration to,
            String sourceId
    ) {
        public EvidenceFocus {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            kind = kind == null ? FocusKind.NARRATIVE_EVENT : kind;
            label = label == null ? "" : label;
            at = at == null ? Duration.ZERO : at;
            from = from == null ? at : from;
            to = to == null ? from : to;
            sourceId = sourceId == null ? id : sourceId;
        }
    }

    public record CombatEvidence(
            String id,
            String label,
            Duration startedAt,
            Duration endedAt,
            Completeness completeness,
            List<CombatSideEvidence> sides,
            List<String> notes
    ) {
        public CombatEvidence {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            label = label == null || label.isBlank() ? id : label;
            startedAt = startedAt == null ? Duration.ZERO : startedAt;
            endedAt = endedAt == null ? startedAt : endedAt;
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            sides = sides == null ? List.of() : List.copyOf(sides);
            notes = notes == null ? List.of() : List.copyOf(notes);
        }
    }

    public record CombatSideEvidence(
            String id,
            String label,
            Integer teamId,
            Relationship relationship,
            Completeness completeness,
            List<UnitEvidenceRow> totalRows,
            Map<String, Integer> workerLosses,
            Map<String, Integer> structureLosses,
            Map<String, Integer> staticDefenseLosses,
            List<CombatParticipantEvidence> participants
    ) {
        public CombatSideEvidence {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            label = label == null || label.isBlank() ? id : label;
            relationship = relationship == null ? Relationship.UNKNOWN : relationship;
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            totalRows = totalRows == null ? List.of() : List.copyOf(totalRows);
            workerLosses = workerLosses == null ? Map.of() : Map.copyOf(workerLosses);
            structureLosses = structureLosses == null ? Map.of() : Map.copyOf(structureLosses);
            staticDefenseLosses = staticDefenseLosses == null ? Map.of() : Map.copyOf(staticDefenseLosses);
            participants = participants == null ? List.of() : List.copyOf(participants);
        }
    }

    public record CombatParticipantEvidence(
            String participantId,
            String player,
            Completeness completeness,
            List<UnitEvidenceRow> rows,
            Map<String, Integer> workerLosses,
            Map<String, Integer> structureLosses,
            Map<String, Integer> staticDefenseLosses,
            String reconciliationStatus,
            List<String> reconciliationIssues
    ) {
        public CombatParticipantEvidence {
            if (participantId == null || participantId.isBlank()) throw new IllegalArgumentException("participantId is required");
            player = player == null || player.isBlank() ? participantId : player;
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            rows = rows == null ? List.of() : List.copyOf(rows);
            workerLosses = workerLosses == null ? Map.of() : Map.copyOf(workerLosses);
            structureLosses = structureLosses == null ? Map.of() : Map.copyOf(structureLosses);
            staticDefenseLosses = staticDefenseLosses == null ? Map.of() : Map.copyOf(staticDefenseLosses);
            reconciliationStatus = reconciliationStatus == null ? "UNKNOWN" : reconciliationStatus;
            reconciliationIssues = reconciliationIssues == null ? List.of() : List.copyOf(reconciliationIssues);
        }
    }

    public record UnitEvidenceRow(
            String unit,
            int startCount,
            int additions,
            int losses,
            int endCount,
            CountEvidence creditedKills,
            Completeness completeness,
            String reconciliationStatus
    ) {
        public UnitEvidenceRow {
            if (unit == null || unit.isBlank()) throw new IllegalArgumentException("unit is required");
            creditedKills = creditedKills == null ? CountEvidence.unknown("Авторство убийств по типу юнита недоступно в текущих боевых данных.") : creditedKills;
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            reconciliationStatus = reconciliationStatus == null ? "UNKNOWN" : reconciliationStatus;
        }
    }

    public record CountEvidence(Integer value, Completeness completeness, String note) {
        public CountEvidence {
            completeness = completeness == null ? Completeness.UNAVAILABLE : completeness;
            note = note == null ? "" : note;
        }

        public static CountEvidence unknown(String note) {
            return new CountEvidence(null, Completeness.UNAVAILABLE, note);
        }
    }

    public enum Relationship { SELECTED, TEAMMATE, OPPONENT, UNKNOWN }
    public enum FocusKind { PHASE, NARRATIVE_EVENT, TURNING_POINT, COMBAT }
}
