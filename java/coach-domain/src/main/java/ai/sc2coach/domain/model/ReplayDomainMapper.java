package ai.sc2coach.domain.model;

import ai.sc2coach.domain.ReplayAnalysis;

import java.time.Duration;
import java.util.List;

public final class ReplayDomainMapper {

    public Match map(ReplayAnalysis source) {
        var replay = source.replay();
        return new Match(
                replay == null ? null : replay.map(),
                replay == null ? null : replay.type(),
                replay == null || replay.gameSeconds() == null
                        ? Duration.ZERO
                        : Duration.ofMillis(Math.round(replay.gameSeconds() * 1000)),
                replay == null ? List.of() : replay.winner(),
                source.players().stream().map(this::mapPlayer).toList()
        );
    }

    private PlayerState mapPlayer(ReplayAnalysis.Player player) {
        return new PlayerState(
                player.pid() == null ? 0 : player.pid(),
                player.name(),
                PlayerState.Race.from(player.race()),
                player.team(),
                player.result(),
                player.mmr(),
                player.apm(),
                player.stats().stream().map(this::mapSnapshot).toList()
        );
    }

    private PlayerState.StateSnapshot mapSnapshot(ReplayAnalysis.PlayerStat stat) {
        return new PlayerState.StateSnapshot(
                number(stat.time()),
                new PlayerState.Economy(
                        integer(stat.workersActiveCount()),
                        number(stat.mineralsCurrent()),
                        number(stat.vespeneCurrent()),
                        number(stat.mineralsCollectionRate()),
                        number(stat.vespeneCollectionRate())
                ),
                new PlayerState.Army(
                        number(stat.mineralsUsedCurrentArmy()),
                        number(stat.vespeneUsedCurrentArmy()),
                        number(stat.mineralsLostArmy()),
                        number(stat.vespeneLostArmy())
                ),
                new PlayerState.Production(
                        number(stat.foodUsed()),
                        number(stat.foodMade())
                )
        );
    }

    private static double number(Number value) { return value == null ? 0 : value.doubleValue(); }
    private static int integer(Number value) { return value == null ? 0 : value.intValue(); }
}
