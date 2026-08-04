package ai.sc2coach.portal.api;

import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.portal.analysis.AnalysisResponse;
import ai.sc2coach.portal.analysis.AnalysisService;
import ai.sc2coach.portal.analysis.MatchComparison;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
        var score = new MatchComparison.PlayerScore("Alpha", "Terran", 1, 72.0, 70.0, 80.0, 68.0, 60.0);
        var comparison = new MatchComparison.Result("Alpha", 8.0, "medium", List.of(score));
        var summary = new MatchContext.MatchSummary(
                1,
                "Alpha",
                18.0,
                MatchContext.Confidence.MEDIUM,
                Map.of(1, Duration.ofMinutes(2)),
                List.of(new MatchContext.LeadSegment(1, "Alpha", Duration.ZERO, Duration.ofMinutes(2), 18.0))
        );
        var matchContext = new MatchContext(List.of(), summary);
        given(analysisService.analyze(any())).willReturn(new AnalysisResponse(
                "0.1.0", "Test Map", 120.0,
                List.of(new AnalysisResponse.PlayerSummary(1, "Alpha", "Terran", 1, "Win", 3500, 100.0)),
                comparison,
                matchContext
        ));
        MockMultipartFile replay = new MockMultipartFile(
                "replay", "match.SC2Replay", "application/octet-stream", new byte[]{1}
        );

        mockMvc.perform(multipart("/api/v1/analyses").file(replay))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.map").value("Test Map"))
                .andExpect(jsonPath("$.players[0].name").value("Alpha"))
                .andExpect(jsonPath("$.comparison.leader").value("Alpha"))
                .andExpect(jsonPath("$.matchContext.summary.finalLeaderName").value("Alpha"))
                .andExpect(jsonPath("$.matchContext.summary.leadHistory[0].averageGap").value(18.0));
    }
}
