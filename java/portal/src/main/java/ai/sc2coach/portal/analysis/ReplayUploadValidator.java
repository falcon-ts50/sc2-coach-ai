package ai.sc2coach.portal.analysis;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public final class ReplayUploadValidator {

    private static final long MAX_REPLAY_BYTES = 25L * 1024L * 1024L;
    private static final int MAX_FILENAME_LENGTH = 160;

    String validateMetadata(MultipartFile replay) {
        if (replay == null || replay.isEmpty()) {
            throw new IllegalArgumentException("Replay file must not be empty");
        }
        if (replay.getSize() > MAX_REPLAY_BYTES) {
            throw new IllegalArgumentException("Replay file must be 25 MB or smaller");
        }
        String filename = replay.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Replay filename is required");
        }
        String safeName = Path.of(filename).getFileName().toString();
        if (safeName.length() > MAX_FILENAME_LENGTH || containsControlCharacter(safeName)) {
            throw new IllegalArgumentException("Replay filename is not allowed");
        }
        if (!safeName.toLowerCase(Locale.ROOT).endsWith(".sc2replay")) {
            throw new IllegalArgumentException("Expected a .SC2Replay file");
        }
        return safeName;
    }

    void validateReplaySignature(Path replayPath) throws IOException {
        byte[] header = new byte[4];
        int read;
        try (InputStream input = Files.newInputStream(replayPath)) {
            read = input.read(header);
        }
        if (read < 4 || header[0] != 'M' || header[1] != 'P' || header[2] != 'Q'
                || (header[3] != 0x1A && header[3] != 0x1B)) {
            throw new IllegalArgumentException("Expected a StarCraft II replay MPQ archive");
        }
    }

    private static boolean containsControlCharacter(String value) {
        return value.chars().anyMatch(character -> character < 32 || character == 127);
    }
}
