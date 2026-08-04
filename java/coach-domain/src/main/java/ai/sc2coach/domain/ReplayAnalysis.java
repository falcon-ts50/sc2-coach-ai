package ai.sc2coach.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReplayAnalysis(
        @JsonProperty("schema_version") String schemaVersion,
        String source,
        Replay replay,
        @JsonProperty("focus_player") String focusPlayer,
        List<Player> players,
        List<TimelineEvent> timeline
) {
    public ReplayAnalysis {
        players = players == null ? List.of() : List.copyOf(players);
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Replay(
            String map,
            String release,
            @JsonProperty("base_build") Long baseBuild,
            String type,
            @JsonProperty("game_seconds") Double gameSeconds,
            List<String> winner
    ) {
        public Replay {
            winner = winner == null ? List.of() : List.copyOf(winner);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Player(
            Integer pid,
            String name,
            String race,
            Integer team,
            String result,
            Integer mmr,
            Double apm,
            List<PlayerStat> stats
    ) {
        public Player {
            stats = stats == null ? List.of() : List.copyOf(stats);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerStat(
            Double time,
            String clock,
            @JsonProperty("minerals_current") Double mineralsCurrent,
            @JsonProperty("vespene_current") Double vespeneCurrent,
            @JsonProperty("minerals_collection_rate") Double mineralsCollectionRate,
            @JsonProperty("vespene_collection_rate") Double vespeneCollectionRate,
            @JsonProperty("workers_active_count") Integer workersActiveCount,
            @JsonProperty("food_used") Double foodUsed,
            @JsonProperty("food_made") Double foodMade,
            @JsonProperty("minerals_used_current_army") Double mineralsUsedCurrentArmy,
            @JsonProperty("vespene_used_current_army") Double vespeneUsedCurrentArmy,
            @JsonProperty("minerals_lost_army") Double mineralsLostArmy,
            @JsonProperty("vespene_lost_army") Double vespeneLostArmy
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TimelineEvent(
            Double time,
            String clock,
            Object player,
            String event,
            String unit,
            String upgrade,
            String victim,
            Map<String, Object> attributes
    ) {}
}
