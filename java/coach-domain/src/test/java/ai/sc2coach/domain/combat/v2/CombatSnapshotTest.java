package ai.sc2coach.domain.combat.v2;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CombatSnapshotTest {

    @Test
    void separatesArmyStateFromCollateralLossesAndUpgrades() {
        ArmySnapshot before = new ArmySnapshot(
                "dragonDriver",
                Map.of("Marine", 14, "SiegeTank", 1),
                new ResourceValue(1050, 250),
                30,
                Map.of("TerranInfantryWeapons", 2, "TerranInfantryArmor", 1),
                List.of("Stimpack", "CombatShield")
        );
        ArmySnapshot after = new ArmySnapshot(
                "dragonDriver",
                Map.of("Marine", 3),
                new ResourceValue(150, 0),
                6,
                Map.of("TerranInfantryWeapons", 2, "TerranInfantryArmor", 1),
                List.of("Stimpack", "CombatShield")
        );
        LossBreakdown losses = new LossBreakdown(
                Map.of("Marine", 11, "SiegeTank", 1),
                Map.of("SCV", 9),
                Map.of("SupplyDepot", 1),
                Map.of(),
                Map.of(),
                new ResourceValue(900, 250),
                new ResourceValue(450, 0),
                new ResourceValue(100, 0)
        );

        CombatSnapshot.Participant participant = new CombatSnapshot.Participant(
                "dragonDriver", before, after, losses
        );

        assertThat(participant.before().composition()).doesNotContainKeys("SCV", "SupplyDepot");
        assertThat(participant.before().upgrades()).containsEntry("TerranInfantryWeapons", 2);
        assertThat(participant.losses().workerCount()).isEqualTo(9);
        assertThat(participant.losses().structures()).containsEntry("SupplyDepot", 1);
        assertThat(participant.losses().totalValueLost().total()).isEqualTo(1700);
    }

    @Test
    void rejectsParticipantWhoseSnapshotsBelongToAnotherPlayer() {
        ArmySnapshot snapshot = new ArmySnapshot(
                "Guardian", Map.of(), ResourceValue.zero(), 0, Map.of(), List.of()
        );
        LossBreakdown losses = new LossBreakdown(
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), null, null, null
        );

        assertThatThrownBy(() -> new CombatSnapshot.Participant(
                "dragonDriver", snapshot, snapshot, losses
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keepsArmyEconomicAndStrategicWinnersIndependent() {
        CombatOutcome outcome = new CombatOutcome(
                "dragonDriver",
                "Guardian",
                "Guardian",
                "dragonDriver won the army trade, but Guardian destroyed workers and infrastructure.",
                0.82
        );

        assertThat(outcome.armyTradeWinner()).isNotEqualTo(outcome.strategicWinner());
        assertThat(outcome.economicWinner()).isEqualTo(outcome.strategicWinner());
    }
}
