package ai.sc2coach.domain.decision;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionModelTest {

    @Test
    void keepsEvidenceAndConfidenceExplicit() {
        var evidence = new Evidence.Metric(
                "army.value.before_attack",
                Duration.ofMinutes(12),
                "armyValue",
                8_400,
                "resources",
                Map.of("opponentArmyValue", 10_200)
        );
        var decision = new Decision(
                "attack-1",
                1,
                Decision.Type.ATTACK,
                Duration.ofMinutes(12),
                null,
                new Decision.Confidence(0.8, Decision.Confidence.Basis.DETERMINISTIC_RULE),
                List.of(evidence),
                Map.of()
        );

        assertThat(decision.evidence()).containsExactly(evidence);
        assertThat(decision.confidence().value()).isEqualTo(0.8);
    }

    @Test
    void rejectsInvalidConfidence() {
        assertThatThrownBy(() -> new Decision.Confidence(1.1, Decision.Confidence.Basis.HEURISTIC))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
