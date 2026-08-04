package ai.sc2coach.domain.context;

import java.time.Duration;
import java.util.List;

public record TurningPoint(
        Duration at,
        Integer previousLeaderPid,
        String previousLeaderName,
        Integer newLeaderPid,
        String newLeaderName,
        double scoreSwing,
        Severity severity,
        List<Reason> reasons
) {
    public TurningPoint {
        at = at == null ? Duration.ZERO : at;
        severity = severity == null ? Severity.NOTABLE : severity;
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }

    public record Reason(String component, String playerName, double change) { }

    public enum Severity { NOTABLE, MAJOR, CRITICAL }
}
