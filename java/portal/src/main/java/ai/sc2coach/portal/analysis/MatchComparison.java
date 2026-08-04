package ai.sc2coach.portal.analysis;

import ai.sc2coach.domain.ReplayAnalysis;

import java.util.Comparator;
import java.util.List;

public final class MatchComparison {

    private MatchComparison() {
    }

    public static Result compare(ReplayAnalysis analysis) {
        List<PlayerScore> ranking = analysis.players().stream()
                .map(MatchComparison::score)
                .sorted(Comparator.comparingDouble(PlayerScore::score).reversed())
                .toList();

        double gap = ranking.size() < 2 ? 0.0 : ranking.getFirst().score() - ranking.get(1).score();
        String confidence = gap >= 12.0 ? "high" : gap >= 5.0 ? "medium" : "low";
        String leader = ranking.isEmpty() ? null : ranking.getFirst().name();
        return new Result(leader, round(gap), confidence, ranking);
    }

    private static PlayerScore score(ReplayAnalysis.Player player) {
        ReplayAnalysis.PlayerStat last = player.stats().isEmpty()
                ? null
                : player.stats().getLast();

        double economy = last == null ? 0.0 : normalize(
                value(last.workersActiveCount()), 90.0,
                value(last.mineralsCollectionRate()) + value(last.vespeneCollectionRate()), 5000.0
        );
        double army = last == null ? 0.0 : normalize(
                value(last.mineralsUsedCurrentArmy()) + value(last.vespeneUsedCurrentArmy()), 30000.0,
                value(last.foodUsed()), 200.0
        );
        double efficiency = last == null ? 0.0 : 100.0 - Math.min(100.0,
                (value(last.mineralsLostArmy()) + value(last.vespeneLostArmy())) / 250.0
        );
        double activity = Math.min(100.0, value(player.apm()) / 2.0);
        double total = economy * 0.35 + army * 0.35 + efficiency * 0.20 + activity * 0.10;

        return new PlayerScore(
                player.name(), player.race(), player.team(), round(total),
                round(economy), round(army), round(efficiency), round(activity)
        );
    }

    private static double normalize(double first, double firstMax, double second, double secondMax) {
        return Math.min(100.0, ((first / firstMax) * 50.0) + ((second / secondMax) * 50.0));
    }

    private static double value(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record Result(String leader, double gapToSecond, String confidence, List<PlayerScore> ranking) {
        public Result {
            ranking = List.copyOf(ranking);
        }
    }

    public record PlayerScore(
            String name,
            String race,
            Integer team,
            double score,
            double economy,
            double army,
            double efficiency,
            double activity
    ) {
    }
}
