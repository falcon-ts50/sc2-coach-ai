package ai.sc2coach.domain.decision;

import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionEngineTest {

    @Test
    void detectsArmyLossAndSubsequentRebuild() {
        var player = player(List.of(
                snapshot(300, 40, 800, 200, 1000, 500, 0, 0),
                snapshot(360, 41, 850, 220, 500, 250, 700, 300),
                snapshot(480, 45, 1000, 300, 700, 350, 700, 300)
        ));

        var decisions = DecisionEngine.defaults().detect(match(player));

        assertThat(decisions).extracting(Decision::type)
                .contains(Decision.Type.ATTACK, Decision.Type.REBUILD);
        assertThat(decisions.stream().filter(d -> d.type() == Decision.Type.ATTACK).findFirst().orElseThrow().evidence())
                .hasSize(2);
    }

    @Test
    void marksEconomicInterpretationsAsHeuristics() {
        var player = player(List.of(
                snapshot(300, 30, 500, 100, 300, 100, 0, 0),
                snapshot(360, 35, 700, 300, 320, 120, 0, 0)
        ));

        var decisions = DecisionEngine.defaults().detect(match(player));

        assertThat(decisions).anySatisfy(decision -> {
            assertThat(decision.type()).isIn(Decision.Type.EXPAND, Decision.Type.TECH_SWITCH);
            assertThat(decision.confidence().basis()).isEqualTo(Decision.Confidence.Basis.HEURISTIC);
            assertThat(decision.attributes()).containsEntry("hypothesis", true);
        });
    }

    private static Match match(PlayerState player) {
        return new Match("Test Map", "2v2", Duration.ofMinutes(10), List.of(), List.of(player));
    }

    private static PlayerState player(List<PlayerState.StateSnapshot> timeline) {
        return new PlayerState(1, "Alpha", PlayerState.Race.TERRAN, 1, "Win", 3000, 100.0, timeline);
    }

    private static PlayerState.StateSnapshot snapshot(
            double second,
            int workers,
            double mineralRate,
            double gasRate,
            double armyMinerals,
            double armyGas,
            double mineralsLost,
            double gasLost
    ) {
        return new PlayerState.StateSnapshot(
                second,
                new PlayerState.Economy(workers, 0, 0, mineralRate, gasRate),
                new PlayerState.Army(armyMinerals, armyGas, mineralsLost, gasLost),
                new PlayerState.Production(100, 120)
        );
    }
}
