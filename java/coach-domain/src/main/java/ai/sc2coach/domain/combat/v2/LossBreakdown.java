package ai.sc2coach.domain.combat.v2;

import java.util.Map;

public record LossBreakdown(
        Map<String, Integer> combatUnits,
        Map<String, Integer> workers,
        Map<String, Integer> structures,
        Map<String, Integer> staticDefense,
        Map<String, Integer> supportUnits,
        ResourceValue armyValueLost,
        ResourceValue economicValueLost,
        ResourceValue infrastructureValueLost
) {
    public LossBreakdown {
        combatUnits = copy(combatUnits);
        workers = copy(workers);
        structures = copy(structures);
        staticDefense = copy(staticDefense);
        supportUnits = copy(supportUnits);
        armyValueLost = valueOrZero(armyValueLost);
        economicValueLost = valueOrZero(economicValueLost);
        infrastructureValueLost = valueOrZero(infrastructureValueLost);
    }

    public ResourceValue totalValueLost() {
        return armyValueLost.plus(economicValueLost).plus(infrastructureValueLost);
    }

    public int workerCount() {
        return workers.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static Map<String, Integer> copy(Map<String, Integer> source) {
        return source == null ? Map.of() : Map.copyOf(source);
    }

    private static ResourceValue valueOrZero(ResourceValue value) {
        return value == null ? ResourceValue.zero() : value;
    }
}
