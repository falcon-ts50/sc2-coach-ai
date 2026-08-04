package ai.sc2coach.portal.analysis;

import ai.sc2coach.domain.ReplayAnalysis;
import ai.sc2coach.domain.ReplayAnalysisReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public final class AnalysisService {

    private final ReplayDecoder replayDecoder;
    private final ReplayAnalysisReader reader = new ReplayAnalysisReader();

    public AnalysisService(ReplayDecoder replayDecoder) {
        this.replayDecoder = replayDecoder;
    }

    public AnalysisResponse analyze(MultipartFile replay) {
        String filename = validate(replay);
        try (TemporaryWorkspace workspace = TemporaryWorkspace.create()) {
            Path replayPath = workspace.replayPath(filename);
            try (var input = replay.getInputStream()) {
                Files.copy(input, replayPath);
            }
            Path analysisPath = replayDecoder.decode(replayPath, workspace.output());
            ReplayAnalysis analysis = reader.read(analysisPath);
            return AnalysisResponse.from(analysis);
        } catch (IOException exception) {
            throw new ReplayDecodingException("Could not process replay upload", exception);
        }
    }

    private static String validate(MultipartFile replay) {
        if (replay == null || replay.isEmpty()) {
            throw new IllegalArgumentException("Replay file must not be empty");
        }
        String filename = replay.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".sc2replay")) {
            throw new IllegalArgumentException("Expected a .SC2Replay file");
        }
        return Path.of(filename).getFileName().toString();
    }
}
