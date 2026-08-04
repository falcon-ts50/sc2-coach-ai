package ai.sc2coach.domain.decision;

import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;

import java.util.List;

@FunctionalInterface
public interface DecisionDetector {
    List<Decision> detect(Match match, PlayerState player);
}
