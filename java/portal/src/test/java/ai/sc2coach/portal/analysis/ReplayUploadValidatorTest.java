package ai.sc2coach.portal.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplayUploadValidatorTest {

    private final ReplayUploadValidator validator = new ReplayUploadValidator();

    @Test
    void sanitizesReplayFilename() {
        MockMultipartFile replay = new MockMultipartFile(
                "replay", "../match.SC2Replay", "application/octet-stream", new byte[]{'M', 'P', 'Q', 0x1A}
        );

        assertThat(validator.validateMetadata(replay)).isEqualTo("match.SC2Replay");
    }

    @Test
    void rejectsNonReplayExtension() {
        MockMultipartFile replay = new MockMultipartFile(
                "replay", "match.txt", "text/plain", new byte[]{'M', 'P', 'Q', 0x1A}
        );

        assertThatThrownBy(() -> validator.validateMetadata(replay))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a .SC2Replay file");
    }

    @Test
    void rejectsReplayNamedFileWithoutMpqSignature() throws Exception {
        var replay = Files.createTempFile("fake-replay", ".SC2Replay");
        Files.write(replay, "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> validator.validateReplaySignature(replay))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected a StarCraft II replay MPQ archive");
    }
}
