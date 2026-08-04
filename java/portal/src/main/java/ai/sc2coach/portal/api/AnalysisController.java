package ai.sc2coach.portal.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> analyze(@RequestPart("replay") MultipartFile replay) {
        if (replay.isEmpty()) {
            throw new IllegalArgumentException("Replay file must not be empty");
        }
        String filename = replay.getOriginalFilename();
        if (filename == null || !filename.endsWith(".SC2Replay")) {
            throw new IllegalArgumentException("Expected a .SC2Replay file");
        }
        return Map.of(
                "status", "accepted",
                "filename", filename,
                "storage", "temporary",
                "nextStep", "decoder integration"
        );
    }
}
