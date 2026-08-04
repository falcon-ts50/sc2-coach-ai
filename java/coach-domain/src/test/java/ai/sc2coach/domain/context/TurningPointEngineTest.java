package ai.sc2coach.domain.context;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static ai.sc2coach.domain.context.MatchContext.*;
import static org.assertj.core.api.Assertions.assertThat;

class TurningPointEngineTest {

    @Test
    void detectsLeaderChangeAndExplainsLargestComponents() {
        var before = frame(60, 1, player(1, "Alpha", 20, 25, 10), player(2, "Beta", -20, -25, -10));
        var after = frame(120, 2, player(1, "Alpha", -18, -35, -8), player(2, "Beta", 18, 35, 8));
        var context = new MatchContext(List.of(before, after), MatchSummary.empty());

        var points = new TurningPointEngine().detect(context);

        assertThat(points).singleElement().satisfies(point -> {
            assertThat(point.previousLeaderName()).isEqualTo("Alpha");
            assertThat(point.newLeaderName()).isEqualTo("Beta");
            assertThat(point.at()).isEqualTo(Duration.ofSeconds(120));
            assertThat(point.reasons()).extracting(TurningPoint.Reason::component)
                    .contains("army", "economy");
        });
    }

    @Test
    void ignoresSmallStableChanges() {
        var before = frame(60, 1, player(1, "Alpha", 5, 6, 2), player(2, "Beta", -5, -6, -2));
        var after = frame(120, 1, player(1, "Alpha", 6, 7, 3), player(2, "Beta", -6, -7, -3));
        assertThat(new TurningPointEngine().detect(new MatchContext(List.of(before, after), MatchSummary.empty()))).isEmpty();
    }

    private ContextFrame frame(double second, int leader, PlayerContext... players) {
        var list = List.of(players);
        var lead = list.stream().filter(p -> p.pid() == leader).findFirst().orElseThrow();
        return new ContextFrame(Duration.ofSeconds((long) second), list, leader, lead.overallScore(), 20);
    }

    private PlayerContext player(int pid, String name, double economy, double army, double supply) {
        double score = economy * .4 + army * .45 + supply * .15;
        return new PlayerContext(pid, name, component(economy), component(army), component(supply), score, Rank.EVEN);
    }

    private Component component(double relative) {
        return new Component(0, 0, relative, Rank.EVEN);
    }
}
