package ai.sc2coach.domain.context;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record MatchContext(
        List<ContextFrame> timeline,
        MatchSummary summary
) {
    public MatchContext {
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        summary = summary == null ? MatchSummary.empty() : summary;
    }

    public record ContextFrame(
            Duration at,
            List<PlayerContext> players,
            Integer leaderPid,
            double leaderScore,
            double gapToSecond
    ) {
        public ContextFrame {
            at = at == null ? Duration.ZERO : at;
            players = players == null ? List.of() : List.copyOf(players);
        }
    }

    public record PlayerContext(
            int pid,
            String name,
            Component economy,
            Component army,
            Component supply,
            double overallScore,
            Rank overallRank
    ) { }

    public record Component(
            double absoluteValue,
            double deltaToFieldAverage,
            double relativePercent,
            Rank rank
    ) { }

    public enum Rank {
        STRONGLY_AHEAD,
        AHEAD,
        EVEN,
        BEHIND,
        STRONGLY_BEHIND
    }

    public record MatchSummary(
            Integer finalLeaderPid,
            String finalLeaderName,
            double finalGap,
            Confidence confidence,
            Map<Integer, Duration> leadershipDuration,
            List<LeadSegment> leadHistory
    ) {
        public MatchSummary {
            confidence = confidence == null ? Confidence.LOW : confidence;
            leadershipDuration = leadershipDuration == null ? Map.of() : Map.copyOf(leadershipDuration);
            leadHistory = leadHistory == null ? List.of() : List.copyOf(leadHistory);
        }

        public static MatchSummary empty() {
            return new MatchSummary(null, null, 0, Confidence.LOW, Map.of(), List.of());
        }
    }

    public record LeadSegment(
            Integer leaderPid,
            String leaderName,
            Duration from,
            Duration to,
            double averageGap
    ) { }

    public enum Confidence { LOW, MEDIUM, HIGH }
}
