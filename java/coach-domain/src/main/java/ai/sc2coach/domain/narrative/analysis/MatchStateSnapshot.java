package ai.sc2coach.domain.narrative.analysis;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record MatchStateSnapshot(
        String id,
        Duration at,
        String player,
        Integer playerPid,
        List<String> teamPlayers,
        Metrics metrics,
        Map<String, Metrics> playerMetrics,
        double confidence,
        List<String> limitations
) {
    public MatchStateSnapshot {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        at = at == null ? Duration.ZERO : at;
        teamPlayers = teamPlayers == null ? List.of() : List.copyOf(teamPlayers);
        metrics = metrics == null ? Metrics.empty() : metrics;
        playerMetrics = playerMetrics == null ? Map.of() : Map.copyOf(playerMetrics);
        confidence = Double.isFinite(confidence) ? Math.max(0, Math.min(1, confidence)) : 0;
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public record Metrics(
            double armyValue,
            double economyProxy,
            double supplyUsed,
            double supplyCap,
            double overallScore
    ) {
        public static Metrics empty() {
            return new Metrics(0, 0, 0, 0, 0);
        }
    }
}
