package ai.sc2coach.portal.api;

import ai.sc2coach.portal.analysis.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
class ApiExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AnalysisService analysisService;

    @Test
    void returnsProblemDetailForInvalidUpload() throws Exception {
        given(analysisService.analyze(any())).willThrow(new IllegalArgumentException("Expected a .SC2Replay file"));
        MockMultipartFile replay = new MockMultipartFile(
                "replay", "notes.txt", "text/plain", new byte[]{1}
        );

        mockMvc.perform(multipart("/api/v1/analyses").file(replay))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid replay upload"))
                .andExpect(jsonPath("$.detail").value("Expected a .SC2Replay file"));
    }
}
