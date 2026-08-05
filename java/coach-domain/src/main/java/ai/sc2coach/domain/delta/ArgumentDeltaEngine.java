package ai.sc2coach.domain.delta;

import ai.sc2coach.domain.context.MatchContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ArgumentDeltaEngine {

    public List<ArgumentDelta> calculate(MatchContext context) {
        if (context == null || context.timeline().size() < 2) return List.of();
        var result = new ArrayList<ArgumentDelta>();
        for (int index = 1; index < context.timeline().size(); index++) {
            var before = context.timeline().get(index - 1);
            var after = context.timeline().get(index);
            Map<Integer, MatchContext.PlayerContext> previous = before.players().stream()
                    .collect(Collectors.toMap(MatchContext.PlayerContext::pid, Function.identity()));
            for (var current : after.players()) {
                var old = previous.get(current.pid());
                if (old == null) continue;
                add(result, before, after, current, ArgumentDelta.Component.ECONOMY,
                        old.economy().absoluteValue(), current.economy().absoluteValue());
                add(result, before, after, current, ArgumentDelta.Component.ARMY,
                        old.army().absoluteValue(), current.army().absoluteValue());
                add(result, before, after, current, ArgumentDelta.Component.SUPPLY,
                        old.supply().absoluteValue(), current.supply().absoluteValue());
                add(result, before, after, current, ArgumentDelta.Component.OVERALL,
                        old.overallScore(), current.overallScore());
            }
        }
        return result.stream()
                .filter(delta -> delta.significance() != ArgumentDelta.Significance.LOW)
                .toList();
    }

    private void add(List<ArgumentDelta> result,
                     MatchContext.ContextFrame before,
                     MatchContext.ContextFrame after,
                     MatchContext.PlayerContext player,
                     ArgumentDelta.Component component,
                     double oldValue,
                     double newValue) {
        double change = newValue - oldValue;
        double percent = Math.abs(oldValue) < 0.0001 ? 0 : change / Math.abs(oldValue) * 100.0;
        double magnitude = Math.abs(percent);
        var significance = magnitude >= 50 ? ArgumentDelta.Significance.CRITICAL
                : magnitude >= 25 ? ArgumentDelta.Significance.HIGH
                : magnitude >= 10 ? ArgumentDelta.Significance.MEDIUM
                : ArgumentDelta.Significance.LOW;
        result.add(new ArgumentDelta(before.at(), after.at(), player.pid(), player.name(), component,
                oldValue, newValue, change, percent, significance));
    }
}
