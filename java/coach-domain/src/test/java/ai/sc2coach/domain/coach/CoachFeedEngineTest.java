package ai.sc2coach.domain.coach;

import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.model.Match;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CoachFeedEngineTest {

    @Test
    void turnsMeasuredMatchShiftIntoActionableFeed() {
        var summary = new MatchContext.MatchSummary(
                2, "Beta", 30, MatchContext.Confidence.HIGH,
                Map.of(), List.of()
        );
        var point = new TurningPoint(
                Duration.ofMinutes(12), 1, "Alpha", 2, "Beta", 42,
                TurningPoint.Severity.CRITICAL,
                List.of(new TurningPoint.Reason("army", "Beta", 35))
        );

        CoachFeed feed = new CoachFeedEngine().build(
                new Match("Map", "1v1", Duration.ofMinutes(20), List.of("Beta"), List.of()),
                new MatchContext(List.of(), summary),
                List.of(point),
                List.of()
        );

        assertThat(feed.headline()).contains("Beta");
        assertThat(feed.cards()).singleElement().satisfies(card -> {
            assertThat(card.impact()).isEqualTo(CoachFeed.Impact.GAME_CHANGING);
            assertThat(card.explanation()).contains("армия");
        });
        assertThat(feed.nextGameRecommendations()).isNotEmpty();
    }
}
