package ai.sc2coach.portal.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServiceTest {

    @Test
    void decodesReadsAndDeletesTemporaryWorkspace() throws Exception {
        AtomicReference<Path> replayPath = new AtomicReference<>();
        AtomicReference<Path> outputPath = new AtomicReference<>();

        ReplayDecoder decoder = (replay, output) -> {
            replayPath.set(replay);
            outputPath.set(output);
            try {
                Path result = output.resolve("replay_analysis.json");
                Files.writeString(result, """
                        {
                          "schema_version": "0.1.0",
                          "replay": {"map": "Cleanup Test", "game_seconds": 120.0, "winner": []},
                          "players": [{"pid": 1, "name": "Alpha", "race": "Terran", "team": 1, "stats": []}],
                          "timeline": []
                        }
                        """);
                return result;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        };

        AnalysisService service = new AnalysisService(decoder, new ReplayUploadValidator());
        MockMultipartFile upload = new MockMultipartFile(
                "replay", "../unsafe.SC2Replay", "application/octet-stream", new byte[]{'M', 'P', 'Q', 0x1A}
        );

        AnalysisResponse response = service.analyze(upload);

        assertThat(response.map()).isEqualTo("Cleanup Test");
        assertThat(response.players()).singleElement()
                .extracting(AnalysisResponse.PlayerSummary::name)
                .isEqualTo("Alpha");
        assertThat(replayPath.get().getFileName().toString()).isEqualTo("unsafe.SC2Replay");
        assertThat(replayPath.get()).doesNotExist();
        assertThat(outputPath.get()).doesNotExist();
    }
}
