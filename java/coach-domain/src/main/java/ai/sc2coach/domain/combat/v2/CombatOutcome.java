package ai.sc2coach.domain.combat.v2;

public record CombatOutcome(
        String armyTradeWinner,
        String economicWinner,
        String strategicWinner,
        String explanation,
        double confidence
) {
    public CombatOutcome {
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }
        explanation = explanation == null ? "" : explanation;
    }
}
