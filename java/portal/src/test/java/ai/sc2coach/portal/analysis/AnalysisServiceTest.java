package ai.sc2coach.portal.analysis;

import ai.sc2coach.domain.ReplayAnalysisReader;
import ai.sc2coach.domain.coach.CoachFeedEngine;
import ai.sc2coach.domain.combat.CombatEngine;
import ai.sc2coach.domain.context.MatchContextEngine;
import ai.sc2coach.domain.context.TurningPointEngine;
import ai.sc2coach.domain.decision.DecisionEngine;
import ai.sc2coach.domain.delta.ArgumentDeltaEngine;
import ai.sc2coach.domain.episode.EpisodeEngine;
import ai.sc2coach.domain.knowledge.KnowledgeEngine;
import ai.sc2coach.domain.model.ReplayDomainMapper;
import ai.sc2coach.domain.narrative.CoachNarrativeEngine;
import ai.sc2coach.domain.narrative.NarrativeEngine;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServiceTest {

    @Test
    void decodesReadsAddsDiagnosticsAndDeletesTemporaryWorkspace() throws Exception {
        AtomicReference<Path> replayPath = new AtomicReference<>();
        AtomicReference<Path> outputPath = new AtomicReference<>();

        ReplayDecoder decoder = (replay, output) -> {
            replayPath.set(replay);
            outputPath.set(output);
            try {
                Path result = output.resolve("replay_analysis.json");
                Files.writeString(result, """
                        {
                          "schema_version": "0.1.0",
                          "replay": {"map": "Cleanup Test", "game_seconds": 120.0, "winner": []},
                          "players": [{"pid": 1, "name": "Alpha", "race": "Terran", "team": 1, "stats": []}],
                          "timeline": []
                        }
                        """);
                return result;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        };

        AnalysisService service = service(decoder);
        MockMultipartFile upload = new MockMultipartFile(
                "replay", "../unsafe.SC2Replay", "application/octet-stream", new byte[]{'M', 'P', 'Q', 0x1A}
        );

        AnalysisResponse response = service.analyze(upload);

        assertThat(response.map()).isEqualTo("Cleanup Test");
        assertThat(response.focusPlayer()).isEqualTo("Alpha");
        assertThat(response.players()).singleElement()
                .extracting(AnalysisResponse.PlayerSummary::name)
                .isEqualTo("Alpha");
        assertThat(response.diagnostics()).isNotNull();
        assertThat(response.diagnostics().analysisId()).isNotBlank();
        assertThat(response.diagnostics().replaySizeBytes()).isEqualTo(4);
        assertThat(response.diagnostics().totalTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(replayPath.get().getFileName().toString()).isEqualTo("unsafe.SC2Replay");
        assertThat(replayPath.get()).doesNotExist();
        assertThat(outputPath.get()).doesNotExist();
    }

    private AnalysisService service(ReplayDecoder decoder) {
        return new AnalysisService(
                decoder,
                new ReplayAnalysisReader(),
                new ReplayDomainMapper(),
                new MatchContextEngine(),
                new TurningPointEngine(),
                DecisionEngine.defaults(),
                KnowledgeEngine.defaults(),
                new CoachFeedEngine(),
                new EpisodeEngine(),
                new ArgumentDeltaEngine(),
                new NarrativeEngine(),
                new CoachNarrativeEngine(),
                new CombatEngine(),
                new ReplayUploadValidator()
        );
    }
}
