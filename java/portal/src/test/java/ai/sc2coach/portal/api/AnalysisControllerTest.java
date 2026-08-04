package ai.sc2coach.portal.api;

import ai.sc2coach.portal.analysis.AnalysisResponse;
import ai.sc2coach.portal.analysis.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AnalysisService analysisService;

    @Test
    void returnsDecodedReplaySummary() throws Exception {
        given(analysisService.analyze(any())).willReturn(new AnalysisResponse(
                "0.1.0", "Test Map", 120.0,
                List.of(new AnalysisResponse.PlayerSummary(1, "Alpha", "Terran", 1, "Win", 3500, 100.0))
        ));
        MockMultipartFile replay = new MockMultipartFile(
                "replay", "match.SC2Replay", "application/octet-stream", new byte[]{1}
        );

        mockMvc.perform(multipart("/api/v1/analyses").file(replay))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.map").value("Test Map"))
                .andExpect(jsonPath("$.players[0].name").value("Alpha"));
    }
}
