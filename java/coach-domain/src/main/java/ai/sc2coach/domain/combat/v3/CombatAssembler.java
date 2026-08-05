package ai.sc2coach.domain.combat.v3;

import ai.sc2coach.domain.combat.Combat;

import java.util.List;

public interface CombatAssembler {
    List<Combat> assemble(List<CombatCluster> clusters);
}
