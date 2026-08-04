package ai.sc2coach.domain.context;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TurningPointEngine {

    private static final double MIN_SWING = 12.0;
    private static final int MAX_RESULTS = 5;

    public List<TurningPoint> detect(MatchContext context) {
        if (context == null || context.timeline().size() < 2) return List.of();

        var candidates = new ArrayList<TurningPoint>();
        for (int i = 1; i < context.timeline().size(); i++) {
            var before = context.timeline().get(i - 1);
            var after = context.timeline().get(i);
            boolean leaderChanged = !after.leaderPid().equals(before.leaderPid());
            double swing = leaderChanged
                    ? Math.abs(before.leaderScore()) + Math.abs(after.leaderScore())
                    : Math.abs(after.gapToSecond() - before.gapToSecond());
            if (!leaderChanged && swing < MIN_SWING) continue;

            Integer focusPid = after.leaderPid();
            var beforePlayer = player(before, focusPid);
            var afterPlayer = player(after, focusPid);
            var reasons = new ArrayList<TurningPoint.Reason>();
            addReason(reasons, "economy", afterPlayer.name(), beforePlayer.economy().relativePercent(), afterPlayer.economy().relativePercent());
            addReason(reasons, "army", afterPlayer.name(), beforePlayer.army().relativePercent(), afterPlayer.army().relativePercent());
            addReason(reasons, "supply", afterPlayer.name(), beforePlayer.supply().relativePercent(), afterPlayer.supply().relativePercent());
            reasons.sort(Comparator.comparingDouble((TurningPoint.Reason r) -> Math.abs(r.change())).reversed());

            candidates.add(new TurningPoint(
                    after.at(), before.leaderPid(), player(before, before.leaderPid()).name(),
                    after.leaderPid(), afterPlayer.name(), swing, severity(swing), reasons.stream().limit(3).toList()
            ));
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble(TurningPoint::scoreSwing).reversed())
                .limit(MAX_RESULTS)
                .sorted(Comparator.comparing(TurningPoint::at))
                .toList();
    }

    private MatchContext.PlayerContext player(MatchContext.ContextFrame frame, Integer pid) {
        return frame.players().stream().filter(p -> p.pid() == pid).findFirst().orElse(frame.players().getFirst());
    }

    private void addReason(List<TurningPoint.Reason> reasons, String component, String player, double before, double after) {
        double change = after - before;
        if (Math.abs(change) >= 4) reasons.add(new TurningPoint.Reason(component, player, change));
    }

    private TurningPoint.Severity severity(double swing) {
        if (swing >= 45) return TurningPoint.Severity.CRITICAL;
        if (swing >= 25) return TurningPoint.Severity.MAJOR;
        return TurningPoint.Severity.NOTABLE;
    }
}
