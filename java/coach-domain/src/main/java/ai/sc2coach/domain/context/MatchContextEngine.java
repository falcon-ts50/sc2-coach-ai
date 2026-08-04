package ai.sc2coach.domain.context;

import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;
import ai.sc2coach.domain.model.PlayerState.StateSnapshot;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static ai.sc2coach.domain.context.MatchContext.*;

public final class MatchContextEngine {

    private static final double ECONOMY_WEIGHT = 0.40;
    private static final double ARMY_WEIGHT = 0.45;
    private static final double SUPPLY_WEIGHT = 0.15;

    public MatchContext analyze(Match match) {
        if (match == null || match.players().isEmpty()) {
            return new MatchContext(List.of(), MatchSummary.empty());
        }

        var seconds = new TreeSet<Double>();
        match.players().forEach(player -> player.timeline().forEach(snapshot -> seconds.add(snapshot.second())));

        List<ContextFrame> frames = seconds.stream()
                .map(second -> frameAt(match.players(), second))
                .toList();

        return new MatchContext(frames, summarize(frames));
    }

    private ContextFrame frameAt(List<PlayerState> players, double second) {
        Map<Integer, StateSnapshot> snapshots = new LinkedHashMap<>();
        for (PlayerState player : players) {
            snapshots.put(player.pid(), snapshotAt(player, second));
        }

        double averageEconomy = snapshots.values().stream().mapToDouble(this::economyValue).average().orElse(0);
        double averageArmy = snapshots.values().stream().mapToDouble(snapshot -> snapshot.army().value()).average().orElse(0);
        double averageSupply = snapshots.values().stream().mapToDouble(snapshot -> snapshot.production().supplyUsed()).average().orElse(0);

        List<PlayerContext> contexts = players.stream()
                .map(player -> {
                    StateSnapshot snapshot = snapshots.get(player.pid());
                    Component economy = component(economyValue(snapshot), averageEconomy);
                    Component army = component(snapshot.army().value(), averageArmy);
                    Component supply = component(snapshot.production().supplyUsed(), averageSupply);
                    double score = economy.relativePercent() * ECONOMY_WEIGHT
                            + army.relativePercent() * ARMY_WEIGHT
                            + supply.relativePercent() * SUPPLY_WEIGHT;
                    return new PlayerContext(player.pid(), player.name(), economy, army, supply, score, rank(score));
                })
                .sorted(Comparator.comparingDouble(PlayerContext::overallScore).reversed())
                .toList();

        PlayerContext leader = contexts.getFirst();
        double secondScore = contexts.size() > 1 ? contexts.get(1).overallScore() : 0;
        return new ContextFrame(
                Duration.ofMillis(Math.round(second * 1000)),
                contexts,
                leader.pid(),
                leader.overallScore(),
                leader.overallScore() - secondScore
        );
    }

    private MatchSummary summarize(List<ContextFrame> frames) {
        if (frames.isEmpty()) return MatchSummary.empty();

        Map<Integer, Duration> leadership = new HashMap<>();
        List<LeadSegment> segments = new ArrayList<>();
        ContextFrame first = frames.getFirst();
        Integer currentLeader = first.leaderPid();
        String currentName = playerName(first, currentLeader);
        Duration segmentStart = first.at();
        double gapSum = 0;
        int gapCount = 0;

        for (int index = 0; index < frames.size(); index++) {
            ContextFrame frame = frames.get(index);
            Duration frameEnd = index + 1 < frames.size() ? frames.get(index + 1).at() : frame.at();
            Duration duration = frameEnd.minus(frame.at());
            leadership.merge(frame.leaderPid(), duration, Duration::plus);

            if (!frame.leaderPid().equals(currentLeader)) {
                segments.add(new LeadSegment(currentLeader, currentName, segmentStart, frame.at(), average(gapSum, gapCount)));
                currentLeader = frame.leaderPid();
                currentName = playerName(frame, currentLeader);
                segmentStart = frame.at();
                gapSum = 0;
                gapCount = 0;
            }
            gapSum += frame.gapToSecond();
            gapCount++;
        }

        ContextFrame last = frames.getLast();
        segments.add(new LeadSegment(currentLeader, currentName, segmentStart, last.at(), average(gapSum, gapCount)));

        double finalGap = last.gapToSecond();
        Confidence confidence = finalGap >= 35 ? Confidence.HIGH : finalGap >= 15 ? Confidence.MEDIUM : Confidence.LOW;
        return new MatchSummary(last.leaderPid(), playerName(last, last.leaderPid()), finalGap, confidence, leadership, segments);
    }

    private StateSnapshot snapshotAt(PlayerState player, double second) {
        StateSnapshot selected = StateSnapshot.empty();
        for (StateSnapshot snapshot : player.timeline()) {
            if (snapshot.second() > second) break;
            selected = snapshot;
        }
        return selected;
    }

    private double economyValue(StateSnapshot snapshot) {
        return snapshot.economy().workers() * 50.0 + snapshot.economy().incomeRate() * 0.5;
    }

    private Component component(double value, double average) {
        double delta = value - average;
        double relative = average <= 0 ? 0 : delta / average * 100;
        return new Component(value, delta, relative, rank(relative));
    }

    private Rank rank(double relativePercent) {
        if (relativePercent >= 25) return Rank.STRONGLY_AHEAD;
        if (relativePercent >= 8) return Rank.AHEAD;
        if (relativePercent <= -25) return Rank.STRONGLY_BEHIND;
        if (relativePercent <= -8) return Rank.BEHIND;
        return Rank.EVEN;
    }

    private String playerName(ContextFrame frame, Integer pid) {
        return frame.players().stream()
                .filter(player -> player.pid() == pid)
                .map(PlayerContext::name)
                .findFirst()
                .orElse("Unknown player");
    }

    private double average(double sum, int count) {
        return count == 0 ? 0 : sum / count;
    }
}
