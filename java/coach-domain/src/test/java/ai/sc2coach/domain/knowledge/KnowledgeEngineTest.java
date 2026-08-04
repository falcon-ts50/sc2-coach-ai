package ai.sc2coach.domain.knowledge;

import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.model.Match;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeEngineTest {

    @Test
    void createsActionableRecommendationFromCriticalTurningPoint() {
        var point = new TurningPoint(
                Duration.ofMinutes(12), 1, "Alpha", 2, "Beta", 42,
                TurningPoint.Severity.CRITICAL,
                List.of(new TurningPoint.Reason("army", "Beta", 35))
        );
        var context = new KnowledgeContext(
                new Match("Map", "1v1", Duration.ofMinutes(20), List.of("Beta"), List.of()),
                new MatchContext(List.of(), MatchContext.MatchSummary.empty()),
                List.of(point),
                List.of()
        );

        List<Recommendation> recommendations = KnowledgeEngine.defaults().evaluate(context);

        assertThat(recommendations).singleElement().satisfies(recommendation -> {
            assertThat(recommendation.priority()).isEqualTo(Recommendation.Priority.CRITICAL);
            assertThat(recommendation.nextAction()).contains("Открой этот момент");
            assertThat(recommendation.confidence()).isEqualTo(0.9);
        });
    }
}
