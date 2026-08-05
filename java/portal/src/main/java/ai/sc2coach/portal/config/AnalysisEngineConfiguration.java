package ai.sc2coach.portal.config;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AnalysisEngineConfiguration {

    @Bean ReplayAnalysisReader replayAnalysisReader() { return new ReplayAnalysisReader(); }
    @Bean ReplayDomainMapper replayDomainMapper() { return new ReplayDomainMapper(); }
    @Bean MatchContextEngine matchContextEngine() { return new MatchContextEngine(); }
    @Bean TurningPointEngine turningPointEngine() { return new TurningPointEngine(); }
    @Bean DecisionEngine decisionEngine() { return DecisionEngine.defaults(); }
    @Bean KnowledgeEngine knowledgeEngine() { return KnowledgeEngine.defaults(); }
    @Bean CoachFeedEngine coachFeedEngine() { return new CoachFeedEngine(); }
    @Bean EpisodeEngine episodeEngine() { return new EpisodeEngine(); }
    @Bean ArgumentDeltaEngine argumentDeltaEngine() { return new ArgumentDeltaEngine(); }
    @Bean NarrativeEngine narrativeEngine() { return new NarrativeEngine(); }
    @Bean CoachNarrativeEngine coachNarrativeEngine() { return new CoachNarrativeEngine(); }
    @Bean CombatEngine combatEngine() { return new CombatEngine(); }
}
