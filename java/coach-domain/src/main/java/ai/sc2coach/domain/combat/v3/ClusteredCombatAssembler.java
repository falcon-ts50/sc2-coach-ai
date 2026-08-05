package ai.sc2coach.domain.combat.v3;

import ai.sc2coach.domain.combat.Combat;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClusteredCombatAssembler implements CombatAssembler {

    @Override
    public List<Combat> assemble(List<CombatCluster> clusters) {
        if (clusters == null || clusters.isEmpty()) return List.of();
        return clusters.stream().map(this::assemble).toList();
    }

    private Combat assemble(CombatCluster cluster) {
        List<Combat.Participant> participants = cluster.participants().stream()
                .map(player -> new Combat.Participant(
                        player,
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        0.0,
                        0.0
                ))
                .toList();

        String initiator = cluster.combatCommandsAndAbilities().stream()
                .map(CombatEvidence::actor)
                .filter(player -> player != null && !player.isBlank())
                .findFirst()
                .orElse(cluster.participants().isEmpty() ? null : cluster.participants().getFirst());

        return new Combat(
                cluster.start(),
                cluster.end(),
                initiator,
                null,
                null,
                participants,
                location(cluster),
                cluster.confidence()
        );
    }

    private String location(CombatCluster cluster) {
        MapPoint center = cluster.center();
        return center == null ? null : String.format(Locale.ROOT, "%.1f, %.1f", center.x(), center.y());
    }
}
