package ai.sc2coach.portal.analysis;

import ai.sc2coach.domain.ReplayAnalysis;
import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;

import java.util.List;

public record AnalysisResponse(
        String schemaVersion,
        String map,
        Double gameSeconds,
        List<PlayerSummary> players,
        MatchComparison.Result comparison,
        MatchContext matchContext,
        List<TurningPoint> turningPoints
) {
    public AnalysisResponse {
        players = List.copyOf(players);
        turningPoints = turningPoints == null ? List.of() : List.copyOf(turningPoints);
    }

    public static AnalysisResponse from(
            ReplayAnalysis analysis,
            MatchContext matchContext,
            List<TurningPoint> turningPoints
    ) {
        return new AnalysisResponse(
                analysis.schemaVersion(),
                analysis.replay() == null ? null : analysis.replay().map(),
                analysis.replay() == null ? null : analysis.replay().gameSeconds(),
                analysis.players().stream().map(PlayerSummary::from).toList(),
                MatchComparison.compare(analysis),
                matchContext,
                turningPoints
        );
    }

    public record PlayerSummary(
            Integer pid,
            String name,
            String race,
            Integer team,
            String result,
            Integer mmr,
            Double apm
    ) {
        private static PlayerSummary from(ReplayAnalysis.Player player) {
            return new PlayerSummary(
                    player.pid(), player.name(), player.race(), player.team(),
                    player.result(), player.mmr(), player.apm()
            );
        }
    }
}
