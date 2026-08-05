package ai.sc2coach.domain.combat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record Combat(
        Duration startedAt,
        Duration endedAt,
        String initiator,
        String opponent,
        String winner,
        List<Participant> participants,
        String location,
        double confidence
) {
    public Combat {
        participants = participants == null ? List.of() : List.copyOf(participants);
    }

    public record Participant(
            String player,
            Map<String, Integer> armyBefore,
            Map<String, Integer> armyAfter,
            Map<String, Integer> unitsLost,
            Map<String, Integer> workersLost,
            Map<String, Integer> structuresLost,
            Map<String, Integer> staticDefenseLost,
            List<String> upgrades,
            List<String> technologies,
            double armyValueBefore,
            double armyValueAfter
    ) {
        public Participant {
            armyBefore = immutable(armyBefore);
            armyAfter = immutable(armyAfter);
            unitsLost = immutable(unitsLost);
            workersLost = immutable(workersLost);
            structuresLost = immutable(structuresLost);
            staticDefenseLost = immutable(staticDefenseLost);
            upgrades = upgrades == null ? List.of() : List.copyOf(upgrades);
            technologies = technologies == null ? List.of() : List.copyOf(technologies);
        }

        public Participant(
                String player,
                Map<String, Integer> armyBefore,
                Map<String, Integer> armyAfter,
                Map<String, Integer> unitsLost,
                double armyValueBefore,
                double armyValueAfter
        ) {
            this(player, armyBefore, armyAfter, unitsLost, Map.of(), Map.of(), Map.of(),
                    List.of(), List.of(), armyValueBefore, armyValueAfter);
        }

        private static Map<String, Integer> immutable(Map<String, Integer> value) {
            return value == null ? Map.of() : Map.copyOf(value);
        }
    }
}
