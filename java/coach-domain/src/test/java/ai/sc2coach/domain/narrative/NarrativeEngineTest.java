package ai.sc2coach.domain.narrative;

import ai.sc2coach.domain.delta.ArgumentDelta;
import ai.sc2coach.domain.episode.Episode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NarrativeEngineTest {

    @Test
    void describesCriticalArmyLossInsideFightEpisode() {
        var episode = new Episode(Episode.Type.MAJOR_FIGHT, Duration.ofMinutes(10), Duration.ofMinutes(11),
                "Alpha", "Бой за центр", 90, List.of("смена лидера"));
        var delta = new ArgumentDelta(Duration.ofMinutes(10), Duration.ofMinutes(11), 1, "Alpha",
                ArgumentDelta.Component.ARMY, 100, 45, -55, -55,
                ArgumentDelta.Significance.CRITICAL);

        var narrative = new NarrativeEngine().build(List.of(episode), List.of(delta), "Beta");

        assertThat(narrative.beats()).hasSize(1);
        assertThat(narrative.beats().getFirst().kind()).isEqualTo(MatchNarrative.Kind.COLLAPSE);
        assertThat(narrative.beats().getFirst().statement()).contains("армия", "-55.0%");
    }
}
