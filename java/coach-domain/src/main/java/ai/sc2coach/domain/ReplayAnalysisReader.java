package ai.sc2coach.domain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ReplayAnalysisReader {

    private final ObjectMapper objectMapper;

    public ReplayAnalysisReader() {
        this(new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    public ReplayAnalysisReader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public ReplayAnalysis read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream input = Files.newInputStream(path)) {
            return read(input);
        }
    }

    public ReplayAnalysis read(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        ReplayAnalysis analysis = objectMapper.readValue(input, ReplayAnalysis.class);
        if (analysis.schemaVersion() == null || analysis.schemaVersion().isBlank()) {
            throw new IOException("replay_analysis.json has no schema_version");
        }
        return analysis;
    }
}
