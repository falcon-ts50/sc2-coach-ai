package ai.sc2coach.domain.combat.v2;

public record ResourceValue(double minerals, double vespene) {

    public ResourceValue {
        if (minerals < 0 || vespene < 0) {
            throw new IllegalArgumentException("Resource values must not be negative");
        }
    }

    public static ResourceValue zero() {
        return new ResourceValue(0, 0);
    }

    public double total() {
        return minerals + vespene;
    }

    public ResourceValue plus(ResourceValue other) {
        return new ResourceValue(minerals + other.minerals, vespene + other.vespene);
    }
}
