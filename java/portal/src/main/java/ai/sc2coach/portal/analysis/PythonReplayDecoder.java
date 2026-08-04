package ai.sc2coach.portal.analysis;

import ai.sc2coach.portal.config.AnalysisProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public final class PythonReplayDecoder implements ReplayDecoder {

    private final String pythonExecutable;
    private final Path decoderScript;
    private final Duration timeout;

    public PythonReplayDecoder(AnalysisProperties properties) {
        this.pythonExecutable = properties.pythonExecutable();
        this.decoderScript = properties.decoderScript().toAbsolutePath().normalize();
        this.timeout = properties.timeout();
    }

    @Override
    public Path decode(Path replay, Path outputDirectory) {
        if (!Files.isRegularFile(decoderScript)) {
            throw new ReplayDecodingException("Decoder script not found: " + decoderScript);
        }

        List<String> command = List.of(
                pythonExecutable,
                decoderScript.toString(),
                replay.toAbsolutePath().toString(),
                "--out",
                outputDirectory.toAbsolutePath().toString()
        );
        ProcessBuilder builder = new ProcessBuilder(command)
                .redirectErrorStream(true);

        try {
            Process process = builder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ReplayDecodingException("Replay decoder timed out after " + timeout);
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new ReplayDecodingException("Replay decoder failed with exit code "
                        + process.exitValue() + ": " + output.strip());
            }
            Path result = outputDirectory.resolve("replay_analysis.json");
            if (!Files.isRegularFile(result)) {
                throw new ReplayDecodingException("Decoder completed without replay_analysis.json");
            }
            return result;
        } catch (IOException exception) {
            throw new ReplayDecodingException("Could not start replay decoder", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ReplayDecodingException("Replay decoding was interrupted", exception);
        }
    }
}
