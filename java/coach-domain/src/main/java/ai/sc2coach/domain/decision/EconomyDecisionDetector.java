package ai.sc2coach.domain.decision;

import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class EconomyDecisionDetector implements DecisionDetector {

    private static final int MIN_WORKER_GAIN = 4;
    private static final double MIN_INCOME_GROWTH = 0.25;
    private static final double MIN_GAS_SHARE_INCREASE = 0.15;

    @Override
    public List<Decision> detect(Match match, PlayerState player) {
        var decisions = new ArrayList<Decision>();
        var timeline = player.timeline();

        for (int i = 1; i < timeline.size(); i++) {
            var before = timeline.get(i - 1);
            var after = timeline.get(i);
            Duration at = Duration.ofMillis(Math.round(after.second() * 1000));

            int workerGain = after.economy().workers() - before.economy().workers();
            double beforeIncome = before.economy().incomeRate();
            double afterIncome = after.economy().incomeRate();
            double incomeGrowth = beforeIncome <= 0 ? 0 : (afterIncome - beforeIncome) / beforeIncome;

            if (workerGain >= MIN_WORKER_GAIN && incomeGrowth >= MIN_INCOME_GROWTH) {
                decisions.add(new Decision(
                        "expand-hypothesis-" + player.pid() + "-" + i,
                        player.pid(),
                        Decision.Type.EXPAND,
                        at,
                        at,
                        new Decision.Confidence(0.55, Decision.Confidence.Basis.HEURISTIC),
                        List.of(
                                new Evidence.Delta("worker-growth", at, "workers", before.economy().workers(), after.economy().workers(), "workers"),
                                new Evidence.Delta("income-growth", at, "incomeRate", beforeIncome, afterIncome, "resourcesPerMinute")
                        ),
                        Map.of("hypothesis", true, "workerGain", workerGain, "incomeGrowthRatio", incomeGrowth)
                ));
            }

            double beforeGasShare = gasShare(before);
            double afterGasShare = gasShare(after);
            if (afterGasShare - beforeGasShare >= MIN_GAS_SHARE_INCREASE && after.economy().gasRate() > before.economy().gasRate()) {
                decisions.add(new Decision(
                        "tech-switch-hypothesis-" + player.pid() + "-" + i,
                        player.pid(),
                        Decision.Type.TECH_SWITCH,
                        at,
                        at,
                        new Decision.Confidence(0.45, Decision.Confidence.Basis.HEURISTIC),
                        List.of(new Evidence.Delta(
                                "gas-income-share",
                                at,
                                "gasIncomeShare",
                                beforeGasShare,
                                afterGasShare,
                                "ratio"
                        )),
                        Map.of("hypothesis", true, "gasShareIncrease", afterGasShare - beforeGasShare)
                ));
            }
        }
        return List.copyOf(decisions);
    }

    private static double gasShare(PlayerState.StateSnapshot snapshot) {
        double total = snapshot.economy().incomeRate();
        return total <= 0 ? 0 : snapshot.economy().gasRate() / total;
    }
}
