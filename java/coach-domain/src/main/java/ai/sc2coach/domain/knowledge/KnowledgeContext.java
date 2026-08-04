package ai.sc2coach.domain.knowledge;

import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.decision.Decision;
import ai.sc2coach.domain.model.Match;

import java.util.List;

public record KnowledgeContext(
        Match match,
        MatchContext matchContext,
        List<TurningPoint> turningPoints,
        List<Decision> decisions
) {
    public KnowledgeContext {
        turningPoints = turningPoints == null ? List.of() : List.copyOf(turningPoints);
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
    }
}
