package ai.sc2coach.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;

@ConfigurationProperties("sc2coach.analysis")
public record AnalysisProperties(
        String pythonExecutable,
        Path decoderScript,
        Duration timeout
) {
    public AnalysisProperties {
        pythonExecutable = pythonExecutable == null || pythonExecutable.isBlank() ? "python3" : pythonExecutable;
        decoderScript = decoderScript == null ? Path.of("../analyze.py") : decoderScript;
        timeout = timeout == null ? Duration.ofMinutes(2) : timeout;
    }
}
