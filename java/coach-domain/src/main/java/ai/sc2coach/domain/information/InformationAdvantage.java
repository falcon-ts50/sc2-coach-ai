package ai.sc2coach.domain.information;

import java.util.List;

public record InformationAdvantage(List<InformationState> states) {
    public InformationAdvantage {
        states = states == null ? List.of() : List.copyOf(states);
    }
}
