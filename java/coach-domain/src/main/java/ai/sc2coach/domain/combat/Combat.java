package ai.sc2coach.domain.combat;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
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
        double confidence,
        String id,
        String ordinalLabel
) {
    public Combat {
        participants = participants == null ? List.of() : List.copyOf(participants);
        id = id == null || id.isBlank() ? "combat-" + startedAt.toMillis() : id;
    }

    public Combat(
            Duration startedAt,
            Duration endedAt,
            String initiator,
            String opponent,
            String winner,
            List<Participant> participants,
            String location,
            double confidence
    ) {
        this(startedAt, endedAt, initiator, opponent, winner, participants, location, confidence, null, null);
    }

    public record Participant(
            String player,
            Map<String, Integer> armyBefore,
            Map<String, Integer> additions,
            Map<String, Integer> armyAfter,
            Map<String, Integer> unitsLost,
            Map<String, Integer> workersLost,
            Map<String, Integer> structuresLost,
            Map<String, Integer> staticDefenseLost,
            List<String> upgrades,
            List<String> technologies,
            double armyValueBefore,
            double armyValueAfter,
            ReconciliationStatus reconciliationStatus,
            List<ReconciliationIssue> reconciliationIssues
    ) {
        public Participant {
            armyBefore = immutable(armyBefore);
            additions = immutable(additions);
            armyAfter = immutable(armyAfter);
            unitsLost = immutable(unitsLost);
            workersLost = immutable(workersLost);
            structuresLost = immutable(structuresLost);
            staticDefenseLost = immutable(staticDefenseLost);
            upgrades = upgrades == null ? List.of() : List.copyOf(upgrades);
            technologies = technologies == null ? List.of() : List.copyOf(technologies);
            reconciliationStatus = reconciliationStatus == null ? ReconciliationStatus.EXACT : reconciliationStatus;
            reconciliationIssues = reconciliationIssues == null ? List.of() : List.copyOf(reconciliationIssues);
        }

        public Participant(
                String player,
                Map<String, Integer> armyBefore,
                Map<String, Integer> additions,
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
            this(player, armyBefore, additions, armyAfter, unitsLost, workersLost, structuresLost, staticDefenseLost,
                    upgrades, technologies, armyValueBefore, armyValueAfter, null, List.of());
        }

        public Participant(
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
            this(player, armyBefore, Map.of(), armyAfter, unitsLost, workersLost, structuresLost, staticDefenseLost,
                    upgrades, technologies, armyValueBefore, armyValueAfter);
        }

        public Participant(
                String player,
                Map<String, Integer> armyBefore,
                Map<String, Integer> armyAfter,
                Map<String, Integer> unitsLost,
                double armyValueBefore,
                double armyValueAfter
        ) {
            this(player, armyBefore, Map.of(), armyAfter, unitsLost, Map.of(), Map.of(), Map.of(),
                    List.of(), List.of(), armyValueBefore, armyValueAfter);
        }

        private static Map<String, Integer> immutable(Map<String, Integer> value) {
            return value == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(value));
        }
    }

    public enum ReconciliationStatus {
        EXACT,
        PARTIAL
    }

    public record ReconciliationIssue(
            String unit,
            int startCount,
            int additions,
            int losses,
            int expectedEndCount,
            int actualEndCount,
            int difference,
            String reason
    ) {}
}
