package ai.sc2coach.domain.combat.v2;

import java.util.List;
import java.util.Map;

public record ArmySnapshot(
        String player,
        Map<String, Integer> composition,
        ResourceValue value,
        double supply,
        Map<String, Integer> upgrades,
        List<String> technologies
) {
    public ArmySnapshot {
        if (player == null || player.isBlank()) throw new IllegalArgumentException("Player is required");
        composition = composition == null ? Map.of() : Map.copyOf(composition);
        value = value == null ? ResourceValue.zero() : value;
        if (supply < 0) throw new IllegalArgumentException("Supply must not be negative");
        upgrades = upgrades == null ? Map.of() : Map.copyOf(upgrades);
        technologies = technologies == null ? List.of() : List.copyOf(technologies);
    }
}
