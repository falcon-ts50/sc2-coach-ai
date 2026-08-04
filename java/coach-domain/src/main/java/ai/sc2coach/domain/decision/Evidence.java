package ai.sc2coach.domain.decision;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public sealed interface Evidence permits Evidence.Metric, Evidence.Event, Evidence.Delta {

    String code();
    Duration at();

    record Metric(
            String code,
            Duration at,
            String metric,
            double value,
            String unit,
            Map<String, Object> context
    ) implements Evidence {
        public Metric {
            code = Objects.requireNonNull(code, "code");
            at = at == null ? Duration.ZERO : at;
            metric = Objects.requireNonNull(metric, "metric");
            unit = Objects.requireNonNullElse(unit, "value");
            context = context == null ? Map.of() : Map.copyOf(context);
        }
    }

    record Event(
            String code,
            Duration at,
            String eventType,
            String subject,
            Map<String, Object> attributes
    ) implements Evidence {
        public Event {
            code = Objects.requireNonNull(code, "code");
            at = at == null ? Duration.ZERO : at;
            eventType = Objects.requireNonNull(eventType, "eventType");
            subject = Objects.requireNonNullElse(subject, "unknown");
            attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        }
    }

    record Delta(
            String code,
            Duration at,
            String metric,
            double before,
            double after,
            String unit
    ) implements Evidence {
        public Delta {
            code = Objects.requireNonNull(code, "code");
            at = at == null ? Duration.ZERO : at;
            metric = Objects.requireNonNull(metric, "metric");
            unit = Objects.requireNonNullElse(unit, "value");
        }

        public double change() { return after - before; }
    }
}
