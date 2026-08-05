package ai.sc2coach.domain.combat;

import ai.sc2coach.domain.ReplayAnalysis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CombatEngine {

    private static final double WINDOW_BEFORE_SECONDS = 5;
    private static final double WINDOW_AFTER_SECONDS = 45;
    private static final double MIN_ATTACK_GAP_SECONDS = 30;

    public List<Combat> detect(ReplayAnalysis analysis, String focusPlayer) {
        var attacks = analysis.timeline().stream()
                .filter(this::isAttackCommand)
                .sorted(Comparator.comparingDouble(event -> value(event.time())))
                .filter(new SpacedAttackPredicate())
                .toList();

        return attacks.stream()
                .map(event -> buildCombat(analysis, event))
                .filter(combat -> combat.participants().size() >= 2)
                .filter(this::hasObservedCombatLoss)
                .filter(combat -> focusPlayer == null || focusPlayer.isBlank()
                        || combat.participants().stream().anyMatch(p -> p.player().equalsIgnoreCase(focusPlayer)))
                .limit(8)
                .toList();
    }

    private Combat buildCombat(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent attack) {
        double attackTime = value(attack.time());
        double start = Math.max(0, attackTime - WINDOW_BEFORE_SECONDS);
        double end = attackTime + WINDOW_AFTER_SECONDS;
        String initiator = playerName(analysis, attack.player());

        var deaths = analysis.timeline().stream()
                .filter(this::isDeath)
                .filter(event -> value(event.time()) >= start && value(event.time()) <= end)
                .filter(event -> isCombatUnit(deathUnitName(analysis, event)))
                .toList();

        var participantNames = new ArrayList<String>();
        if (initiator != null) participantNames.add(initiator);
        deaths.stream().map(event -> playerName(analysis, event.player()))
                .filter(Objects::nonNull).distinct().forEach(participantNames::add);

        var distinctParticipants = participantNames.stream().distinct().limit(4).toList();
        var participants = distinctParticipants.stream()
                .map(player -> participant(analysis, player, start, end, deaths))
                .toList();

        String opponent = distinctParticipants.stream()
                .filter(player -> !player.equalsIgnoreCase(initiator))
                .findFirst().orElse(null);
        String winner = participants.stream()
                .min(Comparator.comparingDouble(this::observedLossScore))
                .map(Combat.Participant::player).orElse(null);

        return new Combat(
                Duration.ofMillis(Math.round(start * 1000)),
                Duration.ofMillis(Math.round(end * 1000)),
                initiator,
                opponent,
                winner,
                participants,
                location(attack),
                deaths.isEmpty() ? 0.45 : 0.82
        );
    }

    private Combat.Participant participant(
            ReplayAnalysis analysis,
            String player,
            double start,
            double end,
            List<ReplayAnalysis.TimelineEvent> deaths
    ) {
        var before = armyAt(analysis, player, start);
        var after = armyAt(analysis, player, end);
        var lost = deaths.stream()
                .filter(event -> player.equalsIgnoreCase(playerName(analysis, event.player())))
                .map(event -> deathUnitName(analysis, event))
                .filter(this::isCombatUnit)
                .collect(Collectors.groupingBy(name -> name, LinkedHashMap::new, Collectors.summingInt(ignored -> 1)));

        return new Combat.Participant(
                player,
                before,
                after,
                lost,
                armyValueAt(analysis, player, start),
                armyValueAt(analysis, player, end)
        );
    }

    private Map<String, Integer> armyAt(ReplayAnalysis analysis, String player, double at) {
        var counts = new LinkedHashMap<String, Integer>();
        analysis.timeline().stream()
                .filter(event -> value(event.time()) <= at)
                .filter(event -> player.equalsIgnoreCase(playerName(analysis, event.player())))
                .forEach(event -> {
                    String unit = lifecycleUnitName(analysis, event);
                    if (!isCombatUnit(unit)) return;
                    if (isBirth(event)) counts.merge(unit, 1, Integer::sum);
                    if (isDeath(event)) counts.computeIfPresent(unit, (ignored, count) -> Math.max(0, count - 1));
                });
        counts.values().removeIf(count -> count <= 0);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(12)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> left, LinkedHashMap::new));
    }

    private double armyValueAt(ReplayAnalysis analysis, String player, double at) {
        return analysis.players().stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(player))
                .flatMap(candidate -> candidate.stats().stream())
                .filter(stat -> value(stat.time()) <= at)
                .max(Comparator.comparingDouble(stat -> value(stat.time())))
                .map(stat -> value(stat.mineralsUsedCurrentArmy()) + value(stat.vespeneUsedCurrentArmy()))
                .orElse(0.0);
    }

    private boolean isAttackCommand(ReplayAnalysis.TimelineEvent event) {
        String eventName = lower(event.event());
        String ability = lower(event.ability());
        return eventName.contains("command") && ability.contains("attack");
    }

    private boolean isBirth(ReplayAnalysis.TimelineEvent event) {
        String name = lower(event.event());
        return name.contains("born") || name.contains("finished") || name.contains("init");
    }

    private boolean isDeath(ReplayAnalysis.TimelineEvent event) {
        return lower(event.event()).contains("died");
    }

    private String lifecycleUnitName(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        if (isDeath(event)) return deathUnitName(analysis, event);
        return event.unit();
    }

    private String deathUnitName(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        if (event.unit() != null && !event.unit().isBlank()) return event.unit();
        if (event.victim() == null || event.victim().isBlank()) return null;
        return isKnownPlayerName(analysis, event.victim()) ? null : event.victim();
    }

    private boolean isKnownPlayerName(ReplayAnalysis analysis, String value) {
        return analysis.players().stream().anyMatch(player -> player.name().equalsIgnoreCase(value));
    }

    private boolean isCombatUnit(String unit) {
        if (unit == null || unit.isBlank()) return false;
        String value = lower(unit);
        return !value.startsWith("beacon")
                && !value.contains("mineralfield")
                && !value.contains("vespeneg")
                && !value.contains("destructible")
                && !value.contains("watchtower")
                && !value.contains("xelnaga")
                && !value.contains("larva")
                && !value.contains("egg")
                && !value.contains("mule")
                && !value.contains("scv")
                && !value.contains("probe")
                && !value.contains("drone")
                && !value.contains("commandcenter")
                && !value.contains("orbitalcommand")
                && !value.contains("planetaryfortress")
                && !value.contains("nexus")
                && !value.contains("hatchery")
                && !value.contains("lair")
                && !value.contains("hive")
                && !value.contains("barracks")
                && !value.contains("factory")
                && !value.contains("starport")
                && !value.contains("gateway")
                && !value.contains("warpgate")
                && !value.contains("pylon")
                && !value.contains("supplydepot")
                && !value.contains("refinery")
                && !value.contains("assimilator")
                && !value.contains("extractor")
                && !value.contains("engineeringbay")
                && !value.contains("armory")
                && !value.contains("missileturret")
                && !value.contains("photoncannon")
                && !value.contains("spinecrawler")
                && !value.contains("sporecrawler");
    }

    private boolean hasObservedCombatLoss(Combat combat) {
        return combat.participants().stream().anyMatch(participant -> !participant.unitsLost().isEmpty());
    }

    private double observedLossScore(Combat.Participant participant) {
        double resourceLoss = Math.max(0, participant.armyValueBefore() - participant.armyValueAfter());
        int observedDeaths = participant.unitsLost().values().stream().mapToInt(Integer::intValue).sum();
        return resourceLoss + observedDeaths * 25.0;
    }

    private String playerName(ReplayAnalysis analysis, Object player) {
        if (player == null) return null;
        if (player instanceof Number number) {
            return analysis.players().stream()
                    .filter(candidate -> candidate.pid() != null && candidate.pid() == number.intValue())
                    .map(ReplayAnalysis.Player::name).findFirst().orElse(String.valueOf(player));
        }
        String raw = String.valueOf(player);
        return analysis.players().stream()
                .filter(candidate -> raw.equalsIgnoreCase(candidate.name()) || raw.equals(String.valueOf(candidate.pid())))
                .map(ReplayAnalysis.Player::name).findFirst().orElse(raw);
    }

    private String location(ReplayAnalysis.TimelineEvent event) {
        var position = event.targetPosition() != null ? event.targetPosition() : event.position();
        return position == null ? null : String.format(Locale.ROOT, "%.1f, %.1f", position.x(), position.y());
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static double value(Number number) {
        return number == null ? 0 : number.doubleValue();
    }

    private static final class SpacedAttackPredicate implements java.util.function.Predicate<ReplayAnalysis.TimelineEvent> {
        private double previous = -Double.MAX_VALUE;

        @Override
        public boolean test(ReplayAnalysis.TimelineEvent event) {
            double current = value(event.time());
            if (current - previous < MIN_ATTACK_GAP_SECONDS) return false;
            previous = current;
            return true;
        }
    }
}
