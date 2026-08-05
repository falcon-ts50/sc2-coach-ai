package ai.sc2coach.domain.information;

import java.util.List;

public record InformationConfidence(double value, List<String> factors) {
    public InformationConfidence {
        value = Math.max(0.0, Math.min(1.0, value));
        factors = factors == null ? List.of() : List.copyOf(factors);
    }

    public static InformationConfidence of(double value, String... factors) {
        return new InformationConfidence(value, factors == null ? List.of() : List.of(factors));
    }
}
