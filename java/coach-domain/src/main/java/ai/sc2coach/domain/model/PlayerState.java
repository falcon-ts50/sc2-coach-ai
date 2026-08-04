package ai.sc2coach.domain.model;

import java.util.List;
import java.util.Objects;

public record PlayerState(
        int pid,
        String name,
        Race race,
        Integer team,
        String result,
        Integer mmr,
        Double apm,
        List<StateSnapshot> timeline
) {
    public PlayerState {
        name = Objects.requireNonNullElse(name, "Unknown player");
        race = race == null ? Race.UNKNOWN : race;
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
    }

    public StateSnapshot latest() {
        return timeline.isEmpty() ? StateSnapshot.empty() : timeline.getLast();
    }

    public enum Race {
        TERRAN, ZERG, PROTOSS, RANDOM, UNKNOWN;

        public static Race from(String value) {
            if (value == null) return UNKNOWN;
            try {
                return valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return UNKNOWN;
            }
        }
    }

    public record StateSnapshot(
            double second,
            Economy economy,
            Army army,
            Production production
    ) {
        public StateSnapshot {
            economy = economy == null ? Economy.empty() : economy;
            army = army == null ? Army.empty() : army;
            production = production == null ? Production.empty() : production;
        }

        public static StateSnapshot empty() {
            return new StateSnapshot(0, Economy.empty(), Army.empty(), Production.empty());
        }
    }

    public record Economy(
            int workers,
            double minerals,
            double gas,
            double mineralRate,
            double gasRate
    ) {
        public static Economy empty() { return new Economy(0, 0, 0, 0, 0); }
        public double incomeRate() { return mineralRate + gasRate; }
    }

    public record Army(
            double mineralValue,
            double gasValue,
            double mineralsLost,
            double gasLost
    ) {
        public static Army empty() { return new Army(0, 0, 0, 0); }
        public double value() { return mineralValue + gasValue; }
        public double losses() { return mineralsLost + gasLost; }
    }

    public record Production(
            double supplyUsed,
            double supplyCap
    ) {
        public static Production empty() { return new Production(0, 0); }
        public double supplyAvailable() { return Math.max(0, supplyCap - supplyUsed); }
        public boolean supplyBlocked() { return supplyCap > 0 && supplyUsed >= supplyCap; }
    }
}
