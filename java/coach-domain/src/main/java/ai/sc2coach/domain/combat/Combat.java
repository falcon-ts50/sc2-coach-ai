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
            double armyValueBefore,
            double armyValueAfter
    ) {
        public Participant {
            armyBefore = armyBefore == null ? Map.of() : Map.copyOf(armyBefore);
            armyAfter = armyAfter == null ? Map.of() : Map.copyOf(armyAfter);
            unitsLost = unitsLost == null ? Map.of() : Map.copyOf(unitsLost);
        }
    }
}
