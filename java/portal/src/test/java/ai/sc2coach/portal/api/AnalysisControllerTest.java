package ai.sc2coach.portal.api;

import ai.sc2coach.domain.coach.CoachFeed;
import ai.sc2coach.domain.combat.Combat;
import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AnalysisService analysisService;

    @Test
    void returnsDecodedReplaySummary() throws Exception {
        var score = new MatchComparison.PlayerScore("Alpha", "Terran", 1, 72.0, 70.0, 80.0, 68.0, 60.0);
        var comparison = new MatchComparison.Result("Alpha", 8.0, "medium", List.of(score));
        var summary = new MatchContext.MatchSummary(
                1, "Alpha", 18.0, MatchContext.Confidence.MEDIUM,
                Map.of(1, Duration.ofMinutes(2)),
                List.of(new MatchContext.LeadSegment(1, "Alpha", Duration.ZERO, Duration.ofMinutes(2), 18.0))
        );
        var point = new TurningPoint(
                Duration.ofSeconds(90), 2, "Beta", 1, "Alpha", 32,
                TurningPoint.Severity.MAJOR,
                List.of(new TurningPoint.Reason("army", "Alpha", 24))
        );
        var feed = new CoachFeed(
                "В финале лидировал Alpha.",
                List.of(new CoachFeed.Card(Duration.ofSeconds(90), CoachFeed.Kind.POOR,
                        CoachFeed.Impact.HIGH, "Перелом", "Преимущество изменилось.", 0.8)),
                List.of("Сохраняй армию после неудачного боя.")
        );
        var diagnostics = new AnalysisResponse.Diagnostics(
                "analysis-123", "0.7.0", "abcdef1", Instant.parse("2026-08-05T11:00:00Z"),
                183_431, 750, 120, 870
        );
        var combat = new Combat(
                Duration.ofSeconds(95), Duration.ofSeconds(145), "Alpha", "Beta", null,
                List.of(new Combat.Participant(
                        "Alpha",
                        Map.of("Marine", 2),
                        Map.of("Marine", 3),
                        Map.of("Marine", 4),
                        Map.of("Marine", 1),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        100,
                        200,
                        Combat.ReconciliationStatus.EXACT,
                        List.of()
                )),
                "42.0, 42.0", 0.86, "combat-01-095-145-alpha", "Бой 1"
        );
        given(analysisService.analyze(any(), nullable(String.class))).willReturn(new AnalysisResponse(
                "0.2.0", "Test Map", 120.0, "Alpha",
                List.of(new AnalysisResponse.PlayerSummary(1, "Alpha", "Terran", 1, "Win", 3500, 100.0)),
                comparison, new MatchContext(List.of(), summary), List.of(point), List.of(combat), feed,
                "# Transcript\n\n- `01:30` **Alpha** — command: Attack",
                diagnostics
        ));
        MockMultipartFile replay = new MockMultipartFile(
                "replay", "match.SC2Replay", "application/octet-stream", new byte[]{1}
        );

        mockMvc.perform(multipart("/api/v1/analyses").file(replay).param("focusPlayer", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchContext.summary.finalLeaderName").value("Alpha"))
                .andExpect(jsonPath("$.turningPoints[0].newLeaderName").value("Alpha"))
                .andExpect(jsonPath("$.combats[0].id").value("combat-01-095-145-alpha"))
                .andExpect(jsonPath("$.combats[0].ordinalLabel").value("Бой 1"))
                .andExpect(jsonPath("$.combats[0].participants[0].additions.Marine").value(3))
                .andExpect(jsonPath("$.combats[0].participants[0].reconciliationStatus").value("EXACT"))
                .andExpect(jsonPath("$.coachFeed.cards[0].title").value("Перелом"))
                .andExpect(jsonPath("$.transcriptMarkdown").value(org.hamcrest.Matchers.containsString("Attack")))
                .andExpect(jsonPath("$.diagnostics.analysisId").value("analysis-123"))
                .andExpect(jsonPath("$.diagnostics.totalTimeMs").value(870));
    }
}
