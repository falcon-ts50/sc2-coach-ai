package ai.sc2coach.domain.combat.v3;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class SpatialTemporalCombatClusterer implements CombatClusterer {

    private final CombatClusteringConfig config;

    public SpatialTemporalCombatClusterer() {
        this(CombatClusteringConfig.defaults());
    }

    public SpatialTemporalCombatClusterer(CombatClusteringConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public List<CombatCluster> cluster(List<CombatEvidence> evidence) {
        if (evidence == null || evidence.isEmpty()) return List.of();

        List<MutableCluster> clusters = new ArrayList<>();
        evidence.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CombatEvidence::time))
                .forEach(item -> {
                    MutableCluster candidate = bestClusterFor(clusters, item);
                    if (candidate == null) {
                        clusters.add(new MutableCluster(item));
                    } else {
                        candidate.add(item);
                    }
                });

        for (int i = 0; i < clusters.size(); i++) {
            clusters.get(i).id = "combat-cluster-" + (i + 1);
        }
        return clusters.stream().map(MutableCluster::toCluster).toList();
    }

    private MutableCluster bestClusterFor(List<MutableCluster> clusters, CombatEvidence evidence) {
        return clusters.stream()
                .filter(cluster -> isTimeCompatible(cluster, evidence))
                .filter(cluster -> isSpatiallyCompatible(cluster, evidence))
                .min(Comparator.comparingDouble(cluster -> distanceScore(cluster, evidence)))
                .orElse(null);
    }

    private boolean isTimeCompatible(MutableCluster cluster, CombatEvidence evidence) {
        Duration gap = evidence.time().minus(cluster.end);
        return !gap.isNegative() && gap.compareTo(config.continuationGap()) <= 0;
    }

    private boolean isSpatiallyCompatible(MutableCluster cluster, CombatEvidence evidence) {
        if (cluster.region == null || evidence.location() == null) return true;
        return cluster.region.center().distanceTo(evidence.location()) <= config.maxSpatialDistance();
    }

    private double distanceScore(MutableCluster cluster, CombatEvidence evidence) {
        Duration gap = evidence.time().minus(cluster.end);
        double timeScore = Math.max(0, gap.toMillis()) / 1000.0;
        if (cluster.region == null || evidence.location() == null) return timeScore + config.maxSpatialDistance();
        return timeScore + cluster.region.center().distanceTo(evidence.location());
    }

    private final class MutableCluster {
        private String id;
        private Duration start;
        private Duration end;
        private CombatRegion region;
        private final List<CombatEvidence> deaths = new ArrayList<>();
        private final List<CombatEvidence> commandsAndAbilities = new ArrayList<>();
        private final Set<String> participants = new LinkedHashSet<>();
        private int evidenceCount;
        private int missingSpatialCount;

        private MutableCluster(CombatEvidence evidence) {
            this.start = evidence.time();
            this.end = evidence.time();
            add(evidence);
        }

        private void add(CombatEvidence evidence) {
            evidenceCount++;
            if (evidence.time().compareTo(start) < 0) start = evidence.time();
            if (evidence.time().compareTo(end) > 0) end = evidence.time();
            if (evidence.location() == null) {
                missingSpatialCount++;
            } else {
                region = region == null ? CombatRegion.around(evidence.location()) : region.include(evidence.location());
            }
            if (evidence.isDeath()) deaths.add(evidence);
            if (evidence.isCommandOrAbility()) commandsAndAbilities.add(evidence);
            participants.addAll(evidence.participantNames());
        }

        private CombatCluster toCluster() {
            double missingRatio = evidenceCount == 0 ? 0.0 : ((double) missingSpatialCount / evidenceCount);
            double confidence = 1.0 - missingRatio * config.missingSpatialPenalty();
            return new CombatCluster(
                    id,
                    start,
                    end,
                    region,
                    List.copyOf(participants),
                    List.copyOf(deaths),
                    List.copyOf(commandsAndAbilities),
                    confidence,
                    missingSpatialCount > 0,
                    missingSpatialCount
            );
        }
    }
}
