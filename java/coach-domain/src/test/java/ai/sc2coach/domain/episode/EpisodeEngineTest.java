package ai.sc2coach.domain.episode;

import ai.sc2coach.domain.context.TurningPoint;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EpisodeEngineTest {

    @Test
    void groupsNearbyLeadChangesIntoOneEpisode() {
        var first = new TurningPoint(Duration.ofMinutes(9), 1, "Alpha", 2, "Beta", 32,
                TurningPoint.Severity.MAJOR,
                List.of(new TurningPoint.Reason("army", "Beta", 24)));
        var second = new TurningPoint(Duration.ofMinutes(10), 2, "Beta", 1, "Alpha", 28,
                TurningPoint.Severity.MAJOR,
                List.of(new TurningPoint.Reason("economy", "Alpha", 12)));

        List<Episode> episodes = new EpisodeEngine().build(List.of(first, second), List.of());

        assertThat(episodes).hasSize(1);
        assertThat(episodes.getFirst().from()).isEqualTo(Duration.ofMinutes(8).plusSeconds(40));
        assertThat(episodes.getFirst().to()).isEqualTo(Duration.ofMinutes(10).plusSeconds(20));
        assertThat(episodes.getFirst().evidence()).hasSize(2);
    }
}
