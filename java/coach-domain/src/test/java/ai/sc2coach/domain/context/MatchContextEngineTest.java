package ai.sc2coach.domain.context;

import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchContextEngineTest {

    private final MatchContextEngine engine = new MatchContextEngine();

    @Test
    void identifiesLeaderAndRelativeComponentAdvantages() {
        Match match = new Match("Test Map", "1v1", Duration.ofMinutes(10), List.of("Alpha"), List.of(
                player(1, "Alpha", snapshot(0, 12, 600, 300, 20), snapshot(60, 30, 2000, 900, 65)),
                player(2, "Beta", snapshot(0, 12, 600, 300, 20), snapshot(60, 22, 1100, 500, 42))
        ));

        MatchContext context = engine.analyze(match);
        MatchContext.ContextFrame finalFrame = context.timeline().getLast();
        MatchContext.PlayerContext alpha = finalFrame.players().getFirst();

        assertThat(finalFrame.leaderPid()).isEqualTo(1);
        assertThat(alpha.name()).isEqualTo("Alpha");
        assertThat(alpha.economy().rank()).isIn(MatchContext.Rank.AHEAD, MatchContext.Rank.STRONGLY_AHEAD);
        assertThat(alpha.army().relativePercent()).isPositive();
        assertThat(context.summary().finalLeaderName()).isEqualTo("Alpha");
    }

    @Test
    void recordsLeadChangesAsReadableSegments() {
        Match match = new Match("Test Map", "1v1", Duration.ofMinutes(3), List.of("Beta"), List.of(
                player(1, "Alpha", snapshot(0, 20, 1200, 800, 35), snapshot(60, 35, 2500, 1200, 70), snapshot(120, 36, 900, 700, 72)),
                player(2, "Beta", snapshot(0, 20, 1200, 800, 35), snapshot(60, 25, 1300, 700, 48), snapshot(120, 42, 3600, 1600, 95))
        ));

        MatchContext context = engine.analyze(match);

        assertThat(context.summary().leadHistory()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(context.summary().leadHistory().getFirst().leaderName()).isEqualTo("Alpha");
        assertThat(context.summary().finalLeaderName()).isEqualTo("Beta");
        assertThat(context.summary().leadershipDuration()).containsKeys(1, 2);
    }

    @Test
    void returnsEmptyContextForMissingPlayers() {
        MatchContext context = engine.analyze(new Match("Map", "1v1", Duration.ZERO, List.of(), List.of()));

        assertThat(context.timeline()).isEmpty();
        assertThat(context.summary().finalLeaderPid()).isNull();
    }

    private static PlayerState player(int pid, String name, PlayerState.StateSnapshot... timeline) {
        return new PlayerState(pid, name, PlayerState.Race.TERRAN, pid, null, null, null, List.of(timeline));
    }

    private static PlayerState.StateSnapshot snapshot(
            double second,
            int workers,
            double income,
            double army,
            double supply
    ) {
        return new PlayerState.StateSnapshot(
                second,
                new PlayerState.Economy(workers, 0, 0, income, 0),
                new PlayerState.Army(army, 0, 0, 0),
                new PlayerState.Production(supply, 200)
        );
    }
}
