package ai.sc2coach.domain.combat.v3;

public record CombatRegion(double minX, double minY, double maxX, double maxY) {

    public CombatRegion {
        if (minX > maxX) throw new IllegalArgumentException("minX must be <= maxX");
        if (minY > maxY) throw new IllegalArgumentException("minY must be <= maxY");
    }

    public static CombatRegion around(MapPoint point) {
        return new CombatRegion(point.x(), point.y(), point.x(), point.y());
    }

    public CombatRegion include(MapPoint point) {
        return new CombatRegion(
                Math.min(minX, point.x()),
                Math.min(minY, point.y()),
                Math.max(maxX, point.x()),
                Math.max(maxY, point.y())
        );
    }

    public MapPoint center() {
        return new MapPoint((minX + maxX) / 2.0, (minY + maxY) / 2.0);
    }
}
