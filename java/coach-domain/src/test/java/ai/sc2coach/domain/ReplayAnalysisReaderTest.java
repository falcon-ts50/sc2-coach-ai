package ai.sc2coach.domain;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ReplayAnalysisReaderTest {

    @Test
    void readsPythonDecoderContract() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/replay-analysis-minimal.json")) {
            ReplayAnalysis analysis = new ReplayAnalysisReader().read(input);

            assertThat(analysis.schemaVersion()).isEqualTo("0.1.0");
            assertThat(analysis.replay().map()).isEqualTo("Test Map");
            assertThat(analysis.focusPlayer()).isEqualTo("Alpha");
            assertThat(analysis.players()).singleElement().satisfies(player -> {
                assertThat(player.race()).isEqualTo("Terran");
                assertThat(player.stats()).singleElement()
                        .extracting(ReplayAnalysis.PlayerStat::workersActiveCount)
                        .isEqualTo(18);
            });
            assertThat(analysis.timeline()).singleElement()
                    .extracting(ReplayAnalysis.TimelineEvent::unit)
                    .isEqualTo("Barracks");
        }
    }
}
