package ai.sc2coach.domain.delta;

import ai.sc2coach.domain.context.MatchContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class ArgumentDeltaEngineTest {

    @Test
    void reportsMaterialArmyCollapse() {
        var beforePlayer = player(1, "Alpha", 100, 100, 80, 95);
        var afterPlayer = player(1, "Alpha", 105, 45, 72, 61);
        var context = new MatchContext(List.of(
                new MatchContext.ContextFrame(Duration.ofMinutes(10), List.of(beforePlayer), 1, 95, 0),
                new MatchContext.ContextFrame(Duration.ofMinutes(11), List.of(afterPlayer), 1, 61, 0)
        ), null);

        var deltas = new ArgumentDeltaEngine().calculate(context);

        assertThat(deltas).anySatisfy(delta -> {
            assertThat(delta.component()).isEqualTo(ArgumentDelta.Component.ARMY);
            assertThat(delta.relativeChangePercent()).isCloseTo(-55.0, offset(0.0001));
            assertThat(delta.significance()).isEqualTo(ArgumentDelta.Significance.CRITICAL);
        });
    }

    private MatchContext.PlayerContext player(int pid, String name, double economy, double army,
                                               double supply, double overall) {
        return new MatchContext.PlayerContext(pid, name,
                component(economy), component(army), component(supply), overall, MatchContext.Rank.EVEN);
    }

    private MatchContext.Component component(double value) {
        return new MatchContext.Component(value, 0, 0, MatchContext.Rank.EVEN);
    }
}
