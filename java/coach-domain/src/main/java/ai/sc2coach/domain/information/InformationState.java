package ai.sc2coach.domain.information;

import java.util.List;

public record InformationState(String player, List<Entry> entries) {
    public InformationState {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public record Entry(String topic, Knowledge knowledge, List<String> evidence) {
        public Entry {
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }
    }

    public enum Knowledge {
        KNOWN,
        UNKNOWN,
        POTENTIALLY_KNOWN
    }
}
