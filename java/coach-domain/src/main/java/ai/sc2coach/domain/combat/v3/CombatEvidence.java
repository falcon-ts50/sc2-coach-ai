package ai.sc2coach.domain.combat.v3;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record CombatEvidence(
        String id,
        CombatEvidenceType type,
        Duration time,
        String actor,
        String victimOwner,
        String killer,
        String unit,
        String ability,
        MapPoint location
) {
    public CombatEvidence {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(time, "time");
    }

    public static CombatEvidence death(
            String id,
            Duration time,
            String victimOwner,
            String killer,
            String unit,
            MapPoint location
    ) {
        return new CombatEvidence(id, CombatEvidenceType.UNIT_DEATH, time, killer, victimOwner, killer,
                unit, null, location);
    }

    public static CombatEvidence combatCommand(
            String id,
            Duration time,
            String actor,
            String ability,
            MapPoint location
    ) {
        return new CombatEvidence(id, CombatEvidenceType.COMBAT_COMMAND, time, actor, null, null,
                null, ability, location);
    }

    public static CombatEvidence combatAbility(
            String id,
            Duration time,
            String actor,
            String ability,
            MapPoint location
    ) {
        return new CombatEvidence(id, CombatEvidenceType.COMBAT_ABILITY, time, actor, null, null,
                null, ability, location);
    }

    public boolean hasSpatialData() {
        return location != null;
    }

    public boolean isDeath() {
        return type == CombatEvidenceType.UNIT_DEATH;
    }

    public boolean isCommandOrAbility() {
        return type == CombatEvidenceType.COMBAT_COMMAND || type == CombatEvidenceType.COMBAT_ABILITY;
    }

    public List<String> participantNames() {
        Set<String> names = new LinkedHashSet<>();
        add(names, actor);
        add(names, killer);
        add(names, victimOwner);
        return List.copyOf(names);
    }

    private static void add(Set<String> names, String value) {
        if (value != null && !value.isBlank()) names.add(value);
    }
}
