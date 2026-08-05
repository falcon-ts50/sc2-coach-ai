package ai.sc2coach.domain.combat.v3;

import java.util.List;

public interface CombatClusterer {
    List<CombatCluster> cluster(List<CombatEvidence> evidence);
}
