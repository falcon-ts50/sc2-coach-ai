package ai.sc2coach.portal.api;

import ai.sc2coach.portal.analysis.AnalysisResponse;
import ai.sc2coach.portal.analysis.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AnalysisResponse analyze(
            @RequestPart("replay") MultipartFile replay,
            @RequestParam(value = "focusPlayer", required = false) String focusPlayer
    ) {
        return analysisService.analyze(replay, focusPlayer);
    }
}
