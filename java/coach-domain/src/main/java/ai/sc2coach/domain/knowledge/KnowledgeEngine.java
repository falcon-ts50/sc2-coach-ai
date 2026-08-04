package ai.sc2coach.domain.knowledge;

import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.decision.Decision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KnowledgeEngine {

    private final List<KnowledgeRule> rules;

    public KnowledgeEngine(List<KnowledgeRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static KnowledgeEngine defaults() {
        return new KnowledgeEngine(List.of(
                new RepeatAttackRule(),
                new TurningPointReviewRule()
        ));
    }

    public List<Recommendation> evaluate(KnowledgeContext context) {
        var result = new ArrayList<Recommendation>();
        rules.forEach(rule -> result.addAll(rule.evaluate(context)));
        result.sort(Comparator
                .comparingInt((Recommendation r) -> priorityWeight(r.priority())).reversed()
                .thenComparingDouble(Recommendation::confidence).reversed());
        return List.copyOf(result);
    }

    private static int priorityWeight(Recommendation.Priority priority) {
        return switch (priority) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private static final class RepeatAttackRule implements KnowledgeRule {
        @Override
        public List<Recommendation> evaluate(KnowledgeContext context) {
            List<Decision> attacks = context.decisions().stream()
                    .filter(decision -> decision.type() == Decision.Type.ATTACK)
                    .toList();
            if (attacks.isEmpty()) return List.of();
            Decision attack = attacks.getFirst();
            return List.of(new Recommendation(
                    "army.verify-before-repeat-attack",
                    Recommendation.Category.ARMY,
                    Recommendation.Priority.HIGH,
                    attack.confidence().value(),
                    "Проверяй готовность армии перед повторной атакой",
                    "Обнаружен крупный боевой эпизод с заметным падением стоимости армии и ростом потерь.",
                    "Перед следующим боем сравни восстановленную стоимость армии с уровнем до предыдущего размена и проверь текущее преимущество.",
                    attack.evidence()
            ));
        }
    }

    private static final class TurningPointReviewRule implements KnowledgeRule {
        @Override
        public List<Recommendation> evaluate(KnowledgeContext context) {
            if (context.turningPoints().isEmpty()) return List.of();
            TurningPoint point = context.turningPoints().getFirst();
            Recommendation.Priority priority = point.severity() == TurningPoint.Severity.CRITICAL
                    ? Recommendation.Priority.CRITICAL
                    : Recommendation.Priority.HIGH;
            return List.of(new Recommendation(
                    "general.review-main-turning-point",
                    Recommendation.Category.GENERAL,
                    priority,
                    point.severity() == TurningPoint.Severity.CRITICAL ? 0.9 : 0.75,
                    "Разбери главный перелом матча",
                    "В момент " + format(point.at().toSeconds()) + " измеренное преимущество изменилось на " + round(point.scoreSwing()) + ".",
                    "Открой этот момент в реплее и проверь позицию армии, готовность союзников и возможность отступить до размена.",
                    List.of()
            ));
        }
    }

    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private static String format(long seconds) { return (seconds / 60) + ":" + String.format("%02d", seconds % 60); }
}
