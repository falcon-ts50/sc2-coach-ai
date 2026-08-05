package ai.sc2coach.domain.combat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sc2DisplayNamesTest {

    @Test
    void mapsInternalUpgradeIdentifiersToCanonicalEnglishNames() {
        assertThat(Sc2DisplayNames.upgrade("TerranInfantryWeaponsLevel1"))
                .contains("Terran Infantry Weapons +1");
        assertThat(Sc2DisplayNames.upgrade("zerglingmovementspeed"))
                .contains("Metabolic Boost");
        assertThat(Sc2DisplayNames.upgrade("overlordspeed"))
                .contains("Pneumatized Carapace");
    }

    @Test
    void excludesCosmeticAndRewardEntries() {
        assertThat(Sc2DisplayNames.upgrade("RewardDanceGhost")).isEmpty();
        assertThat(Sc2DisplayNames.upgrade("SprayTerran")).isEmpty();
    }

    @Test
    void humanizesUnknownGameplayUpgradeInsteadOfLeakingCamelCase() {
        assertThat(Sc2DisplayNames.upgrade("SomeFutureUpgrade"))
                .contains("Some Future Upgrade");
    }
}
