package ai.sc2coach.domain.narrative.analysis;

import java.time.Duration;
import java.util.List;

public record NarrativeChartModel(
        Duration startedAt,
        Duration endedAt,
        List<Series> series,
        List<Marker> markers,
        List<Interval> phaseIntervals,
        List<String> limitations
) {
    public NarrativeChartModel {
        startedAt = startedAt == null ? Duration.ZERO : startedAt;
        endedAt = endedAt == null ? startedAt : endedAt;
        series = series == null ? List.of() : List.copyOf(series);
        markers = markers == null ? List.of() : List.copyOf(markers);
        phaseIntervals = phaseIntervals == null ? List.of() : List.copyOf(phaseIntervals);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public static NarrativeChartModel empty() {
        return new NarrativeChartModel(Duration.ZERO, Duration.ZERO, List.of(), List.of(), List.of(), List.of());
    }

    public record Series(
            String id,
            String label,
            String unit,
            String source,
            Completeness completeness,
            List<Point> points
    ) {
        public Series {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            label = label == null ? id : label;
            unit = unit == null ? "" : unit;
            source = source == null ? "matchContext" : source;
            completeness = completeness == null ? Completeness.COMPLETE : completeness;
            points = points == null ? List.of() : List.copyOf(points);
        }
    }

    public record Point(Duration at, double value) {
        public Point {
            at = at == null ? Duration.ZERO : at;
            value = Double.isFinite(value) ? value : 0;
        }
    }

    public record Marker(String id, String label, Kind kind, Duration at, String eventId) {
        public Marker {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            label = label == null ? "" : label;
            kind = kind == null ? Kind.EVENT : kind;
            at = at == null ? Duration.ZERO : at;
        }
    }

    public record Interval(String id, String phaseId, String label, Duration from, Duration to) {
        public Interval {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            label = label == null ? "" : label;
            from = from == null ? Duration.ZERO : from;
            to = to == null ? from : to;
        }
    }

    public enum Completeness { COMPLETE, PARTIAL, UNAVAILABLE }
    public enum Kind { COMBAT, TURNING_POINT, PHASE_BOUNDARY, EVENT }
}
