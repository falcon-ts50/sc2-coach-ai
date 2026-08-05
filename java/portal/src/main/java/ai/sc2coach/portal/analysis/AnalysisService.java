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
import ai.sc2coach.domain.knowledge.KnowledgeContext;
import ai.sc2coach.domain.knowledge.KnowledgeEngine;
import ai.sc2coach.domain.knowledge.Recommendation;
import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.ReplayDomainMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public final class AnalysisService {

    private final ReplayDecoder replayDecoder;
    private final ReplayAnalysisReader reader = new ReplayAnalysisReader();
    private final ReplayDomainMapper domainMapper = new ReplayDomainMapper();
    private final MatchContextEngine contextEngine = new MatchContextEngine();
    private final TurningPointEngine turningPointEngine = new TurningPointEngine();
    private final DecisionEngine decisionEngine = DecisionEngine.defaults();
    private final KnowledgeEngine knowledgeEngine = KnowledgeEngine.defaults();
    private final CoachFeedEngine coachFeedEngine = new CoachFeedEngine();
    private final ReplayUploadValidator uploadValidator;

    public AnalysisService(ReplayDecoder replayDecoder, ReplayUploadValidator uploadValidator) {
        this.replayDecoder = replayDecoder;
        this.uploadValidator = uploadValidator;
    }

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
            CoachFeed coachFeed = coachFeedEngine.build(
                    match, matchContext, turningPoints, decisions, recommendations
            );
            return AnalysisResponse.from(analysis, matchContext, turningPoints, coachFeed);
        } catch (IOException exception) {
            throw new ReplayDecodingException("Could not process replay upload", exception);
        }
    }
}
