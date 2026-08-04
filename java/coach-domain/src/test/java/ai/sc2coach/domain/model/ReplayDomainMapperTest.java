package ai.sc2coach.domain.model;

import ai.sc2coach.domain.ReplayAnalysisReader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReplayDomainMapperTest {

    @Test
    void mapsDecoderContractToDecisionReadyDomain() throws Exception {
        try (var input = getClass().getResourceAsStream("/replay-analysis-minimal.json")) {
            var source = new ReplayAnalysisReader().read(input);
            Match match = new ReplayDomainMapper().map(source);

            assertThat(match.map()).isEqualTo("Test Map");
            assertThat(match.duration().toMillis()).isEqualTo(721_500);
            assertThat(match.players()).singleElement().satisfies(player -> {
                assertThat(player.race()).isEqualTo(PlayerState.Race.TERRAN);
                assertThat(player.latest().economy().workers()).isEqualTo(18);
                assertThat(player.latest().economy().incomeRate()).isEqualTo(900);
                assertThat(player.latest().production().supplyAvailable()).isEqualTo(8);
            });
        }
    }
}
