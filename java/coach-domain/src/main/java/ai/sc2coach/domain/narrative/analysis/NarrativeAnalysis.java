package ai.sc2coach.domain.narrative.analysis;

import java.util.List;

public record NarrativeAnalysis(
        String schemaVersion,
        String focusPlayer,
        Integer focusPlayerPid,
        List<String> focusTeamPlayers,
        String officialReplayResult,
        String status,
        String strategicResultStatus,
        NarrativeTimeline timeline,
        NarrativeSummary summary,
        NarrativeChartModel chart,
        NarrativeEvidence evidence,
        MatchFlow matchFlow,
        NarrativeDashboard dashboard,
        List<String> limitations
) {
    public NarrativeAnalysis {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? "narrative-analysis.v1" : schemaVersion;
        focusTeamPlayers = focusTeamPlayers == null ? List.of() : List.copyOf(focusTeamPlayers);
        status = status == null || status.isBlank() ? "PRELIMINARY" : status;
        strategicResultStatus = strategicResultStatus == null || strategicResultStatus.isBlank()
                ? "NOT_EVALUATED"
                : strategicResultStatus;
        timeline = timeline == null ? NarrativeTimeline.empty() : timeline;
        summary = summary == null ? NarrativeSummary.empty() : summary;
        chart = chart == null ? NarrativeChartModel.empty() : chart;
        evidence = evidence == null ? NarrativeEvidence.empty() : evidence;
        matchFlow = matchFlow == null ? MatchFlow.empty() : matchFlow;
        dashboard = dashboard == null ? NarrativeDashboard.empty() : dashboard;
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public static NarrativeAnalysis empty(String focusPlayer) {
        return new NarrativeAnalysis(
                "narrative-analysis.v1",
                focusPlayer,
                null,
                List.of(),
                null,
                "INSUFFICIENT_DATA",
                "NOT_EVALUATED",
                NarrativeTimeline.empty(),
                NarrativeSummary.empty(),
                NarrativeChartModel.empty(),
                NarrativeEvidence.empty(),
                MatchFlow.empty(),
                NarrativeDashboard.empty(),
                List.of("Not enough normalized match context frames to build a narrative timeline.")
        );
    }
}
