package ai.sc2coach.domain.information;

public record InformationPoint(double x, double y) {

    public double distanceTo(InformationPoint other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
