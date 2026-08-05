package ai.sc2coach.domain.narrative.analysis;

import java.util.List;

public record NarrativeSummary(
        String verdict,
        String strategicResultStatus,
        List<String> keyPoints,
        List<String> limitations
) {
    public NarrativeSummary {
        verdict = verdict == null || verdict.isBlank() ? "Недостаточно данных для связного сценария." : verdict;
        strategicResultStatus = strategicResultStatus == null || strategicResultStatus.isBlank()
                ? "NOT_EVALUATED"
                : strategicResultStatus;
        keyPoints = keyPoints == null ? List.of() : List.copyOf(keyPoints);
        limitations = limitations == null ? List.of() : List.copyOf(limitations);
    }

    public static NarrativeSummary empty() {
        return new NarrativeSummary(
                "Недостаточно данных для связного сценария.",
                "NOT_EVALUATED",
                List.of(),
                List.of()
        );
    }
}
