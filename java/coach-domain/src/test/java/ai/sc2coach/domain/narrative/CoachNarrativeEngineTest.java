package ai.sc2coach.domain.narrative;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoachNarrativeEngineTest {

    @Test
    void rendersChronologicalExplanation() {
        var narrative = new MatchNarrative(
                "В финале лидировал Beta.",
                List.of(
                        new MatchNarrative.Beat(Duration.ofMinutes(9), MatchNarrative.Kind.FIGHT,
                                "Первый бой", "Первый бой не изменил игру окончательно.", List.of()),
                        new MatchNarrative.Beat(Duration.ofMinutes(12), MatchNarrative.Kind.COLLAPSE,
                                "Повторная атака", "Армия Alpha уменьшилась на -55.0%.", List.of())
                )
        );

        String text = new CoachNarrativeEngine().render(narrative);

        assertThat(text).contains("9:00", "12:00", "-55.0%", "В финале лидировал Beta");
    }
}
