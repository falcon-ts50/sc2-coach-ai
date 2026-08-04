package ai.sc2coach.domain.decision;

import ai.sc2coach.domain.model.Match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DecisionEngine {

    private final List<DecisionDetector> detectors;

    public DecisionEngine(List<DecisionDetector> detectors) {
        this.detectors = List.copyOf(detectors);
    }

    public static DecisionEngine defaults() {
        return new DecisionEngine(List.of(
                new ArmyDecisionDetector(),
                new EconomyDecisionDetector()
        ));
    }

    public List<Decision> detect(Match match) {
        var decisions = new ArrayList<Decision>();
        for (var player : match.players()) {
            for (var detector : detectors) {
                decisions.addAll(detector.detect(match, player));
            }
        }
        decisions.sort(Comparator.comparing(Decision::startedAt).thenComparing(Decision::id));
        return List.copyOf(decisions);
    }
}
