package ai.sc2coach.portal.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

final class TemporaryWorkspace implements AutoCloseable {

    private final Path root;
    private final Path input;
    private final Path output;

    private TemporaryWorkspace(Path root) throws IOException {
        this.root = root;
        this.input = Files.createDirectory(root.resolve("input"));
        this.output = Files.createDirectory(root.resolve("output"));
    }

    static TemporaryWorkspace create() throws IOException {
        return new TemporaryWorkspace(Files.createTempDirectory("sc2-coach-"));
    }

    Path replayPath(String filename) {
        String safeName = Path.of(filename).getFileName().toString();
        return input.resolve(safeName);
    }

    Path output() {
        return output;
    }

    Path analysisJson() {
        return output.resolve("replay_analysis.json");
    }

    @Override
    public void close() throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
