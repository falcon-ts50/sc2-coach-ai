package ai.sc2coach.domain.delta;

import java.time.Duration;

public record ArgumentDelta(
        Duration from,
        Duration to,
        int pid,
        String playerName,
        Component component,
        double before,
        double after,
        double absoluteChange,
        double relativeChangePercent,
        Significance significance
) {
    public enum Component { ECONOMY, ARMY, SUPPLY, OVERALL }
    public enum Significance { LOW, MEDIUM, HIGH, CRITICAL }
}
