package ai.sc2coach.domain.decision;

import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArmyDecisionDetector implements DecisionDetector {

    private static final double MIN_LOSS_DELTA = 400.0;
    private static final double MIN_VALUE_DROP_RATIO = 0.15;
    private static final double REBUILD_TARGET_RATIO = 0.60;

    @Override
    public List<Decision> detect(Match match, PlayerState player) {
        var decisions = new ArrayList<Decision>();
        var timeline = player.timeline();

        for (int i = 1; i < timeline.size(); i++) {
            var before = timeline.get(i - 1);
            var after = timeline.get(i);
            double lossDelta = after.army().losses() - before.army().losses();
            double beforeValue = before.army().value();
            double afterValue = after.army().value();
            double dropRatio = beforeValue <= 0 ? 0 : (beforeValue - afterValue) / beforeValue;

            if (lossDelta >= MIN_LOSS_DELTA && dropRatio >= MIN_VALUE_DROP_RATIO) {
                Duration at = Duration.ofMillis(Math.round(after.second() * 1000));
                decisions.add(new Decision(
                        "attack-" + player.pid() + "-" + i,
                        player.pid(),
                        Decision.Type.ATTACK,
                        at,
                        at,
                        new Decision.Confidence(0.88, Decision.Confidence.Basis.DETERMINISTIC_RULE),
                        List.of(
                                new Evidence.Delta("army-value-drop", at, "armyValue", beforeValue, afterValue, "resources"),
                                new Evidence.Delta("army-loss-increase", at, "armyLosses", before.army().losses(), after.army().losses(), "resources")
                        ),
                        Map.of("lossDelta", lossDelta, "armyValueDropRatio", dropRatio)
                ));
                addRebuildIfFound(decisions, player, i, beforeValue);
            }
        }
        return List.copyOf(decisions);
    }

    private static void addRebuildIfFound(List<Decision> decisions, PlayerState player, int lossIndex, double preLossValue) {
        if (preLossValue <= 0) return;
        var timeline = player.timeline();
        double target = preLossValue * REBUILD_TARGET_RATIO;
        for (int i = lossIndex + 1; i < timeline.size(); i++) {
            var snapshot = timeline.get(i);
            if (snapshot.army().value() >= target) {
                var start = timeline.get(lossIndex);
                Duration startedAt = Duration.ofMillis(Math.round(start.second() * 1000));
                Duration endedAt = Duration.ofMillis(Math.round(snapshot.second() * 1000));
                decisions.add(new Decision(
                        "rebuild-" + player.pid() + "-" + lossIndex,
                        player.pid(),
                        Decision.Type.REBUILD,
                        startedAt,
                        endedAt,
                        new Decision.Confidence(0.82, Decision.Confidence.Basis.DETERMINISTIC_RULE),
                        List.of(new Evidence.Delta(
                                "army-recovery",
                                endedAt,
                                "armyValue",
                                start.army().value(),
                                snapshot.army().value(),
                                "resources"
                        )),
                        Map.of("targetRatio", REBUILD_TARGET_RATIO, "rebuildSeconds", endedAt.minus(startedAt).toSeconds())
                ));
                return;
            }
        }
    }
}
