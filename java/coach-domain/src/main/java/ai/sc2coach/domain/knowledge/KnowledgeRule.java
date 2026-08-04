package ai.sc2coach.domain.knowledge;

import java.util.List;

public interface KnowledgeRule {
    List<Recommendation> evaluate(KnowledgeContext context);
}
