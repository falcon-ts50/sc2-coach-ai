package ai.sc2coach.domain.combat.v2;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record CombatSnapshot(
        Duration startedAt,
        Duration endedAt,
        String initiator,
        String opponent,
        String location,
        List<Participant> participants,
        CombatOutcome outcome
) {
    public CombatSnapshot {
        if (startedAt == null || endedAt == null || endedAt.compareTo(startedAt) < 0) {
            throw new IllegalArgumentException("Combat window must be valid");
        }
        participants = participants == null ? List.of() : List.copyOf(participants);
        if (participants.size() < 2) throw new IllegalArgumentException("Combat requires at least two participants");
    }

    public record Participant(
            String player,
            ArmySnapshot before,
            ArmySnapshot after,
            LossBreakdown losses
    ) {
        public Participant {
            if (player == null || player.isBlank()) throw new IllegalArgumentException("Player is required");
            if (before == null || after == null || losses == null) {
                throw new IllegalArgumentException("Participant snapshots and losses are required");
            }
            if (!player.equalsIgnoreCase(before.player()) || !player.equalsIgnoreCase(after.player())) {
                throw new IllegalArgumentException("Participant and army snapshot players must match");
            }
        }
    }

    public Map<String, Participant> participantsByPlayer() {
        return participants.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Participant::player,
                participant -> participant
        ));
    }
}
