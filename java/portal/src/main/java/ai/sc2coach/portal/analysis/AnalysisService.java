package ai.sc2coach.portal.analysis;

import ai.sc2coach.domain.ReplayAnalysis;
import ai.sc2coach.domain.ReplayAnalysisReader;
import ai.sc2coach.domain.coach.CoachFeed;
import ai.sc2coach.domain.coach.CoachFeedEngine;
import ai.sc2coach.domain.combat.Combat;
import ai.sc2coach.domain.combat.CombatEngine;
import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.MatchContextEngine;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.context.TurningPointEngine;
import ai.sc2coach.domain.decision.Decision;
import ai.sc2coach.domain.decision.DecisionEngine;
import ai.sc2coach.domain.delta.ArgumentDelta;
import ai.sc2coach.domain.delta.ArgumentDeltaEngine;
import ai.sc2coach.domain.episode.Episode;
import ai.sc2coach.domain.episode.EpisodeEngine;
import ai.sc2coach.domain.knowledge.KnowledgeContext;
import ai.sc2coach.domain.knowledge.KnowledgeEngine;
import ai.sc2coach.domain.knowledge.Recommendation;
import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.ReplayDomainMapper;
import ai.sc2coach.domain.narrative.CoachNarrativeEngine;
import ai.sc2coach.domain.narrative.CombatNarrativeEngine;
import ai.sc2coach.domain.narrative.MatchNarrative;
import ai.sc2coach.domain.narrative.NarrativeEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public final class AnalysisService {

    private static final String APPLICATION_VERSION = System.getenv().getOrDefault("APP_VERSION", "0.8.0-SNAPSHOT");
    private static final String GIT_COMMIT = System.getenv().getOrDefault("GIT_COMMIT", "unknown");

    private final ReplayDecoder replayDecoder;
    private final ReplayAnalysisReader reader;
    private final ReplayDomainMapper domainMapper;
    private final MatchContextEngine contextEngine;
    private final TurningPointEngine turningPointEngine;
    private final DecisionEngine decisionEngine;
    private final KnowledgeEngine knowledgeEngine;
    private final CoachFeedEngine coachFeedEngine;
    private final EpisodeEngine episodeEngine;
    private final ArgumentDeltaEngine argumentDeltaEngine;
    private final NarrativeEngine narrativeEngine;
    private final CoachNarrativeEngine coachNarrativeEngine;
    private final CombatNarrativeEngine combatNarrativeEngine;
    private final CombatEngine combatEngine;
    private final ReplayUploadValidator uploadValidator;

    public AnalysisResponse analyze(MultipartFile replay) {
        return analyze(replay, null);
    }

    public AnalysisResponse analyze(MultipartFile replay, String requestedFocusPlayer) {
        String analysisId = UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        String filename = uploadValidator.validateMetadata(replay);
        long replaySize = replay.getSize();
        log.info("analysis_started id={} replaySizeBytes={} focusPlayer={}", analysisId, replaySize, requestedFocusPlayer);

        try (TemporaryWorkspace workspace = TemporaryWorkspace.create()) {
            Path replayPath = workspace.replayPath(filename);
            try (var input = replay.getInputStream()) {
                Files.copy(input, replayPath);
            }
            uploadValidator.validateReplaySignature(replayPath);

            long decodeStartedAt = System.nanoTime();
            Path analysisPath = replayDecoder.decode(replayPath, workspace.output());
            long decodeTimeMs = elapsedMillis(decodeStartedAt);

            long analysisStartedAt = System.nanoTime();
            ReplayAnalysis analysis = reader.read(analysisPath);
            String focusPlayer = resolveFocusPlayer(analysis, requestedFocusPlayer);
            Match match = domainMapper.map(analysis);
            MatchContext matchContext = contextEngine.analyze(match);
            List<TurningPoint> turningPoints = turningPointEngine.detect(matchContext);
            List<Decision> decisions = decisionEngine.detect(match);
            List<Recommendation> recommendations = knowledgeEngine.evaluate(
                    new KnowledgeContext(match, matchContext, turningPoints, decisions)
            );
            CoachFeed feed = coachFeedEngine.build(match, matchContext, turningPoints, decisions, recommendations);
            List<Combat> combats = combatEngine.detect(analysis, focusPlayer);

            List<Episode> episodes = episodeEngine.build(turningPoints, decisions);
            List<ArgumentDelta> deltas = argumentDeltaEngine.calculate(matchContext);
            MatchNarrative narrative = narrativeEngine.build(episodes, deltas, matchContext.summary().finalLeaderName());
            String fallbackNarrative = coachNarrativeEngine.render(narrative);
            String narrativeText = combatNarrativeEngine.render(focusPlayer, combats, fallbackNarrative);
            CoachFeed.Card narrativeCard = new CoachFeed.Card(
                    Duration.ZERO, CoachFeed.Kind.INFO, CoachFeed.Impact.HIGH,
                    "Как развивался матч", narrativeText, combats.isEmpty() ? 0.65 : 0.84
            );
            List<CoachFeed.Card> cards = Stream.concat(Stream.of(narrativeCard), feed.cards().stream())
                    .limit(6)
                    .toList();
            CoachFeed coachFeed = new CoachFeed(
                    "Разбор матча для " + focusPlayer + ". " + feed.headline(),
                    cards,
                    feed.nextGameRecommendations()
            );

            long analysisTimeMs = elapsedMillis(analysisStartedAt);
            long totalTimeMs = elapsedMillis(startedAt);
            var diagnostics = new AnalysisResponse.Diagnostics(
                    analysisId, APPLICATION_VERSION, GIT_COMMIT, Instant.now(), replaySize,
                    decodeTimeMs, analysisTimeMs, totalTimeMs
            );
            log.info("analysis_completed id={} focusPlayer={} combats={} totalTimeMs={}",
                    analysisId, focusPlayer, combats.size(), totalTimeMs);

            return AnalysisResponse.from(
                    analysis, focusPlayer, matchContext, turningPoints, combats, coachFeed, diagnostics
            );
        } catch (IOException exception) {
            log.error("analysis_failed id={} reason=io", analysisId, exception);
            throw new ReplayDecodingException("Could not process replay upload. Analysis ID: " + analysisId, exception);
        } catch (RuntimeException exception) {
            log.error("analysis_failed id={} reason={}", analysisId, exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }

    private static String resolveFocusPlayer(ReplayAnalysis analysis, String requested) {
        if (requested != null && !requested.isBlank()) {
            return analysis.players().stream()
                    .map(ReplayAnalysis.Player::name)
                    .filter(name -> name.equalsIgnoreCase(requested))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown focus player: " + requested));
        }
        if (analysis.focusPlayer() != null && !analysis.focusPlayer().isBlank()) return analysis.focusPlayer();
        return analysis.players().stream().findFirst().map(ReplayAnalysis.Player::name).orElse(null);
    }

    private static long elapsedMillis(long startedAtNanos) {
        return Math.max(0, (System.nanoTime() - startedAtNanos) / 1_000_000);
    }
}
