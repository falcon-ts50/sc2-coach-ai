package ai.sc2coach.domain.narrative.analysis;

import ai.sc2coach.domain.combat.Combat;
import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.decision.Decision;
import ai.sc2coach.domain.information.InformationReport;
import ai.sc2coach.domain.knowledge.Recommendation;
import ai.sc2coach.domain.model.Match;

import java.util.List;

public record NarrativeAnalysisInput(
        Match match,
        String focusPlayer,
        MatchContext matchContext,
        List<TurningPoint> turningPoints,
        List<Decision> decisions,
        List<Combat> combats,
        InformationReport informationReport,
        List<Recommendation> recommendations
) {
    public NarrativeAnalysisInput {
        turningPoints = turningPoints == null ? List.of() : List.copyOf(turningPoints);
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        combats = combats == null ? List.of() : List.copyOf(combats);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
