package ai.sc2coach.domain.coach;

import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.decision.Decision;
import ai.sc2coach.domain.knowledge.Recommendation;
import ai.sc2coach.domain.model.Match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CoachFeedEngine {

    public CoachFeed build(Match match, MatchContext context, List<TurningPoint> turningPoints, List<Decision> decisions) {
        return build(match, context, turningPoints, decisions, List.of());
    }

    public CoachFeed build(
            Match match,
            MatchContext context,
            List<TurningPoint> turningPoints,
            List<Decision> decisions,
            List<Recommendation> recommendations
    ) {
        var cards = new ArrayList<CoachFeed.Card>();
        turningPoints.stream().limit(2).forEach(point -> cards.add(new CoachFeed.Card(
                point.at(), CoachFeed.Kind.POOR,
                point.severity() == TurningPoint.Severity.CRITICAL ? CoachFeed.Impact.GAME_CHANGING : CoachFeed.Impact.HIGH,
                "Перелом в пользу " + safe(point.newLeaderName()),
                "Измеренное преимущество изменилось на " + round(point.scoreSwing())
                        + ". Основной вклад: " + reasons(point.reasons()) + ".",
                point.severity() == TurningPoint.Severity.CRITICAL ? 0.9 : 0.75
        )));
        decisions.stream()
                .sorted(Comparator.comparingDouble((Decision d) -> d.confidence().value()).reversed())
                .limit(3).map(this::card).forEach(cards::add);
        cards.sort(Comparator.comparingInt((CoachFeed.Card card) -> impactWeight(card.impact())).reversed()
                .thenComparing(CoachFeed.Card::at));
        String leader = context == null || context.summary().finalLeaderName() == null
                ? "явный лидер не определён" : context.summary().finalLeaderName();
        List<String> nextActions = recommendations == null ? List.of() : recommendations.stream()
                .map(Recommendation::nextAction)
                .distinct()
                .limit(3)
                .toList();
        if (nextActions.isEmpty()) {
            nextActions = List.of("Сверь ключевые переломы с таймлайном и проверь, можно ли было безопаснее сохранить преимущество.");
        }
        return new CoachFeed("В финальном измеренном состоянии лидировал " + leader + ".",
                cards.stream().limit(5).toList(), nextActions);
    }

    private CoachFeed.Card card(Decision decision) {
        return switch (decision.type()) {
            case REBUILD -> new CoachFeed.Card(decision.startedAt(), CoachFeed.Kind.GOOD, CoachFeed.Impact.MEDIUM,
                    "Восстановление армии", "После потерь армия была восстановлена до рабочего уровня.", decision.confidence().value());
            case ATTACK -> new CoachFeed.Card(decision.startedAt(), CoachFeed.Kind.RISKY, CoachFeed.Impact.HIGH,
                    "Крупный боевой эпизод", "Атака сопровождалась заметным падением стоимости армии и ростом потерь.", decision.confidence().value());
            case EXPAND -> new CoachFeed.Card(decision.startedAt(), CoachFeed.Kind.INFO, CoachFeed.Impact.MEDIUM,
                    "Вероятное расширение", "Экономические показатели похожи на инвестицию в расширение. Это пока гипотеза.", decision.confidence().value());
            case TECH_SWITCH -> new CoachFeed.Card(decision.startedAt(), CoachFeed.Kind.INFO, CoachFeed.Impact.MEDIUM,
                    "Вероятный технологический переход", "Изменилась структура дохода в сторону газа. Это пока гипотеза.", decision.confidence().value());
            default -> new CoachFeed.Card(decision.startedAt(), CoachFeed.Kind.INFO, CoachFeed.Impact.LOW,
                    decision.type().name(), "Обнаружено решение игрока.", decision.confidence().value());
        };
    }

    private String reasons(List<TurningPoint.Reason> reasons) {
        if (reasons == null || reasons.isEmpty()) return "совокупное изменение показателей";
        return reasons.stream().limit(2)
                .map(reason -> componentName(reason.component()) + " " + signed(reason.change()))
                .reduce((left, right) -> left + ", " + right)
                .orElse("совокупное изменение показателей");
    }

    private static String componentName(String component) {
        if (component == null) return "показатель";
        return switch (component.toLowerCase()) {
            case "army" -> "армия";
            case "economy" -> "экономика";
            case "supply" -> "снабжение";
            default -> component.toLowerCase();
        };
    }

    private static int impactWeight(CoachFeed.Impact impact) {
        return switch (impact) {
            case GAME_CHANGING -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private static String safe(String value) { return value == null ? "неизвестного игрока" : value; }
    private static double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private static String signed(double value) { return (value >= 0 ? "+" : "") + round(value); }
}
