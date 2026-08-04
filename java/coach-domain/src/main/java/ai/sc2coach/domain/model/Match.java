package ai.sc2coach.domain.model;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record Match(
        String map,
        String gameType,
        Duration duration,
        List<String> winners,
        List<PlayerState> players
) {
    public Match {
        map = Objects.requireNonNullElse(map, "Unknown map");
        gameType = Objects.requireNonNullElse(gameType, "Unknown");
        duration = duration == null ? Duration.ZERO : duration;
        winners = winners == null ? List.of() : List.copyOf(winners);
        players = players == null ? List.of() : List.copyOf(players);
    }

    public PlayerState player(int pid) {
        return players.stream()
                .filter(player -> player.pid() == pid)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player pid: " + pid));
    }
}
