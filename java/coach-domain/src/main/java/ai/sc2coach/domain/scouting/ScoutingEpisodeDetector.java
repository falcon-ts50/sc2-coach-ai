package ai.sc2coach.domain.scouting;

import ai.sc2coach.domain.ReplayAnalysis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ScoutingEpisodeDetector {

    private static final double MAX_SCOUTING_TIME_SECONDS = 8 * 60;
    private static final double LOOKBACK_SECONDS = 75;
    private static final double RESPONSE_WINDOW_SECONDS = 120;
    private static final double POTENTIAL_VISION_RADIUS = 12.0;

    private static final Set<String> SCOUT_UNITS = Set.of(
            "reaper", "adept", "observer", "overlord", "overseer",
            "changeling", "hallucinatedphoenix", "phoenix", "scv", "probe", "drone"
    );

    public List<ScoutingEpisode> detect(ReplayAnalysis analysis, String focusPlayer) {
        if (analysis == null) return List.of();

        return analysis.timeline().stream()
                .filter(this::isDeath)
                .filter(event -> value(event.time()) <= MAX_SCOUTING_TIME_SECONDS)
                .filter(event -> isScout(event.unit()))
                .filter(event -> focusPlayer == null || focusPlayer.isBlank()
                        || focusPlayer.equalsIgnoreCase(victimOwner(analysis, event)))
                .map(event -> buildEpisode(analysis, event))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ScoutingEpisode::startedAt))
                .toList();
    }

    private ScoutingEpisode buildEpisode(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent death) {
        String player = victimOwner(analysis, death);
        if (player == null) return null;

        double end = value(death.time());
        double start = Math.max(0, end - LOOKBACK_SECONDS);
        String unit = death.unit();

        var scoutPositions = analysis.timeline().stream()
                .filter(event -> value(event.time()) >= start && value(event.time()) <= end)
                .filter(event -> player.equalsIgnoreCase(playerName(analysis, event.player())))
                .filter(event -> unit.equalsIgnoreCase(event.unit()))
                .map(this::position)
                .filter(Objects::nonNull)
                .toList();

        var observations = potentialObservations(analysis, player, start, end, scoutPositions);
        var responses = responseCandidates(analysis, player, end, observations);

        double confidence = 0.45;
        if (!scoutPositions.isEmpty()) confidence += 0.20;
        if (!observations.isEmpty()) confidence += 0.20;
        if (!responses.isEmpty()) confidence += 0.10;

        return new ScoutingEpisode(
                player,
                unit,
                Duration.ofMillis(Math.round(start * 1000)),
                Duration.ofMillis(Math.round(end * 1000)),
                false,
                observations,
                responses,
                confidence
        );
    }

    private List<ScoutingEpisode.ObservedFact> potentialObservations(
            ReplayAnalysis analysis,
            String scoutPlayer,
            double start,
            double end,
            List<ReplayAnalysis.Position> scoutPositions
    ) {
        if (scoutPositions.isEmpty()) return List.of();

        var result = new ArrayList<ScoutingEpisode.ObservedFact>();
        analysis.timeline().stream()
                .filter(event -> value(event.time()) >= start && value(event.time()) <= end)
                .filter(event -> isInformative(event))
                .filter(event -> {
                    String owner = eventOwner(analysis, event);
                    return owner != null && !owner.equalsIgnoreCase(scoutPlayer);
                })
                .forEach(event -> {
                    var eventPosition = position(event);
                    if (eventPosition == null) return;
                    double distance = scoutPositions.stream()
                            .mapToDouble(pos -> distance(pos, eventPosition))
                            .min().orElse(Double.POSITIVE_INFINITY);
                    if (distance > POTENTIAL_VISION_RADIUS) return;

                    result.add(new ScoutingEpisode.ObservedFact(
                            Duration.ofMillis(Math.round(value(event.time()) * 1000)),
                            eventOwner(analysis, event),
                            observationKind(event),
                            observationSubject(event),
                            distance
                    ));
                });
        return result.stream().distinct().limit(12).toList();
    }

    private List<ScoutingEpisode.ResponseCandidate> responseCandidates(
            ReplayAnalysis analysis,
            String player,
            double scoutingEnded,
            List<ScoutingEpisode.ObservedFact> observations
    ) {
        if (observations.isEmpty()) return List.of();

        return analysis.timeline().stream()
                .filter(event -> value(event.time()) > scoutingEnded)
                .filter(event -> value(event.time()) <= scoutingEnded + RESPONSE_WINDOW_SECONDS)
                .filter(event -> player.equalsIgnoreCase(playerName(analysis, event.player())))
                .filter(this::isDecisionLike)
                .map(event -> new ScoutingEpisode.ResponseCandidate(
                        Duration.ofMillis(Math.round(value(event.time()) * 1000)),
                        decisionSubject(event),
                        Math.round(value(event.time()) - scoutingEnded),
                        responseConfidence(event)
                ))
                .distinct()
                .limit(8)
                .toList();
    }

    private boolean isInformative(ReplayAnalysis.TimelineEvent event) {
        String name = lower(event.event());
        return event.upgrade() != null
                || (event.unit() != null && (name.contains("born") || name.contains("finished") || name.contains("init")));
    }

    private boolean isDecisionLike(ReplayAnalysis.TimelineEvent event) {
        String name = lower(event.event());
        return event.upgrade() != null
                || (event.unit() != null && (name.contains("born") || name.contains("finished") || name.contains("init")));
    }

    private String observationKind(ReplayAnalysis.TimelineEvent event) {
        if (event.upgrade() != null) return "UPGRADE";
        return isLikelyStructure(event.unit()) ? "STRUCTURE" : "UNIT";
    }

    private String observationSubject(ReplayAnalysis.TimelineEvent event) {
        return event.upgrade() != null ? event.upgrade() : event.unit();
    }

    private String decisionSubject(ReplayAnalysis.TimelineEvent event) {
        if (event.upgrade() != null) return "Started/completed upgrade: " + event.upgrade();
        return "Produced/built: " + event.unit();
    }

    private double responseConfidence(ReplayAnalysis.TimelineEvent event) {
        return event.upgrade() != null ? 0.65 : 0.50;
    }

    private boolean isLikelyStructure(String unit) {
        String value = lower(unit);
        return value.contains("barracks") || value.contains("factory") || value.contains("starport")
                || value.contains("gateway") || value.contains("warpgate") || value.contains("nexus")
                || value.contains("commandcenter") || value.contains("hatchery") || value.contains("lair")
                || value.contains("hive") || value.contains("pool") || value.contains("den")
                || value.contains("core") || value.contains("bay") || value.contains("spire")
                || value.contains("forge") || value.contains("armory") || value.contains("chamber")
                || value.contains("pylon") || value.contains("supplydepot");
    }

    private boolean isScout(String unit) {
        return unit != null && SCOUT_UNITS.contains(lower(unit));
    }

    private boolean isDeath(ReplayAnalysis.TimelineEvent event) {
        return lower(event.event()).contains("died");
    }

    private ReplayAnalysis.Position position(ReplayAnalysis.TimelineEvent event) {
        return event.position() != null ? event.position() : event.targetPosition();
    }

    private double distance(ReplayAnalysis.Position left, ReplayAnalysis.Position right) {
        double dx = value(left.x()) - value(right.x());
        double dy = value(left.y()) - value(right.y());
        return Math.sqrt(dx * dx + dy * dy);
    }

    private String victimOwner(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        if (event.victim() == null || event.victim().isBlank()) return null;
        return playerName(analysis, event.victim());
    }

    private String eventOwner(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        if (isDeath(event)) return victimOwner(analysis, event);
        return playerName(analysis, event.player());
    }

    private String playerName(ReplayAnalysis analysis, Object value) {
        if (value == null) return null;
        String raw = String.valueOf(value);
        return analysis.players().stream()
                .filter(player -> raw.equalsIgnoreCase(player.name()) || raw.equals(String.valueOf(player.pid())))
                .map(ReplayAnalysis.Player::name)
                .findFirst()
                .orElse(raw);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static double value(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }
}
