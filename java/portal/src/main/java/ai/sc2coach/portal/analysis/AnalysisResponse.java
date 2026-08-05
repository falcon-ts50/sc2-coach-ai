package ai.sc2coach.portal.analysis;

import ai.sc2coach.domain.ReplayAnalysis;
import ai.sc2coach.domain.coach.CoachFeed;
import ai.sc2coach.domain.combat.Combat;
import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;

import java.time.Instant;
import java.util.List;

public record AnalysisResponse(
        String schemaVersion,
        String map,
        Double gameSeconds,
        String focusPlayer,
        List<PlayerSummary> players,
        MatchComparison.Result comparison,
        MatchContext matchContext,
        List<TurningPoint> turningPoints,
        List<Combat> combats,
        CoachFeed coachFeed,
        String transcriptMarkdown,
        Diagnostics diagnostics
) {
    public AnalysisResponse {
        players = players == null ? List.of() : List.copyOf(players);
        turningPoints = turningPoints == null ? List.of() : List.copyOf(turningPoints);
        combats = combats == null ? List.of() : List.copyOf(combats);
    }

    public static AnalysisResponse from(
            ReplayAnalysis analysis,
            String focusPlayer,
            MatchContext matchContext,
            List<TurningPoint> turningPoints,
            List<Combat> combats,
            CoachFeed coachFeed,
            Diagnostics diagnostics
    ) {
        return new AnalysisResponse(
                analysis.schemaVersion(),
                analysis.replay() == null ? null : analysis.replay().map(),
                analysis.replay() == null ? null : analysis.replay().gameSeconds(),
                focusPlayer,
                analysis.players().stream().map(PlayerSummary::from).toList(),
                MatchComparison.compare(analysis),
                matchContext,
                turningPoints,
                combats,
                coachFeed,
                analysis.transcriptMarkdown(),
                diagnostics
        );
    }

    public record Diagnostics(
            String analysisId,
            String applicationVersion,
            String gitCommit,
            Instant generatedAt,
            long replaySizeBytes,
            long decodeTimeMs,
            long analysisTimeMs,
            long totalTimeMs
    ) {}

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
