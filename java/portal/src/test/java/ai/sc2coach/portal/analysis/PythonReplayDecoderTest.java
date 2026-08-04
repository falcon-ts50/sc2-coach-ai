package ai.sc2coach.portal.analysis;

import ai.sc2coach.portal.config.AnalysisProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PythonReplayDecoderTest {

    @TempDir
    Path tempDir;

    @Test
    void invokesConfiguredDecoderAndRequiresJsonOutput() throws Exception {
        Path script = tempDir.resolve("decoder.sh");
        Files.writeString(script, """
                #!/bin/sh
                mkdir -p "$3"
                cat > "$3/replay_analysis.json" <<'JSON'
                {"schema_version":"0.1.0","replay":{"map":"Process Test","game_seconds":1,"winner":[]},"players":[],"timeline":[]}
                JSON
                """);
        Path replay = Files.write(tempDir.resolve("match.SC2Replay"), new byte[]{1});
        Path output = Files.createDirectory(tempDir.resolve("out"));

        PythonReplayDecoder decoder = new PythonReplayDecoder(
                new AnalysisProperties("/bin/sh", script, Duration.ofSeconds(5))
        );

        Path result = decoder.decode(replay, output);

        assertThat(result).isRegularFile();
        assertThat(Files.readString(result)).contains("Process Test");
    }
}
