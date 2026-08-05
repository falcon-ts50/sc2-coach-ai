package ai.sc2coach.domain.combat.v3;

public record MapPoint(double x, double y) {

    public double distanceTo(MapPoint other) {
        double dx = x - other.x;
        double dy = y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
