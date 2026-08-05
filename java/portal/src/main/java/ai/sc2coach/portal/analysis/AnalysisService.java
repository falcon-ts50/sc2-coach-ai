package ai.sc2coach.portal.analysis;

import ai.sc2coach.domain.ReplayAnalysis;
import ai.sc2coach.domain.ReplayAnalysisReader;
import ai.sc2coach.domain.coach.CoachFeed;
import ai.sc2coach.domain.coach.CoachFeedEngine;
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
import ai.sc2coach.domain.narrative.MatchNarrative;
import ai.sc2coach.domain.narrative.NarrativeEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public final class AnalysisService {

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
    private final ReplayUploadValidator uploadValidator;

    public AnalysisResponse analyze(MultipartFile replay) {
        String filename = uploadValidator.validateMetadata(replay);
        try (TemporaryWorkspace workspace = TemporaryWorkspace.create()) {
            Path replayPath = workspace.replayPath(filename);
            try (var input = replay.getInputStream()) {
                Files.copy(input, replayPath);
            }
            uploadValidator.validateReplaySignature(replayPath);
            Path analysisPath = replayDecoder.decode(replayPath, workspace.output());
            ReplayAnalysis analysis = reader.read(analysisPath);
            Match match = domainMapper.map(analysis);
            MatchContext matchContext = contextEngine.analyze(match);
            List<TurningPoint> turningPoints = turningPointEngine.detect(matchContext);
            List<Decision> decisions = decisionEngine.detect(match);
            List<Recommendation> recommendations = knowledgeEngine.evaluate(
                    new KnowledgeContext(match, matchContext, turningPoints, decisions)
            );
            CoachFeed feed = coachFeedEngine.build(
                    match, matchContext, turningPoints, decisions, recommendations
            );

            List<Episode> episodes = episodeEngine.build(turningPoints, decisions);
            List<ArgumentDelta> deltas = argumentDeltaEngine.calculate(matchContext);
            MatchNarrative narrative = narrativeEngine.build(
                    episodes, deltas, matchContext.summary().finalLeaderName()
            );
            String narrativeText = coachNarrativeEngine.render(narrative);
            CoachFeed.Card narrativeCard = new CoachFeed.Card(
                    Duration.ZERO,
                    CoachFeed.Kind.INFO,
                    CoachFeed.Impact.HIGH,
                    "Как развивался матч",
                    narrativeText,
                    0.8
            );
            List<CoachFeed.Card> cards = Stream.concat(Stream.of(narrativeCard), feed.cards().stream())
                    .limit(6)
                    .toList();
            CoachFeed coachFeed = new CoachFeed(
                    feed.headline(),
                    cards,
                    feed.nextGameRecommendations()
            );

            return AnalysisResponse.from(analysis, matchContext, turningPoints, coachFeed);
        } catch (IOException exception) {
            throw new ReplayDecodingException("Could not process replay upload", exception);
        }
    }
}
