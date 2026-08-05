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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class CombatEngine {

    private static final double WINDOW_BEFORE_SECONDS = 5;
    private static final double WINDOW_AFTER_SECONDS = 45;
    private static final double MIN_ATTACK_GAP_SECONDS = 30;

    public List<Combat> detect(ReplayAnalysis analysis, String focusPlayer) {
        return analysis.timeline().stream()
                .filter(this::isAttackCommand)
                .sorted(Comparator.comparingDouble(event -> value(event.time())))
                .filter(new SpacedAttackPredicate())
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
                .toList();

        var participantNames = new ArrayList<String>();
        addDistinct(participantNames, initiator);
        deaths.forEach(event -> {
            addDistinct(participantNames, deathOwnerName(analysis, event));
            addDistinct(participantNames, killerName(analysis, event));
        });

        var distinctParticipants = participantNames.stream().limit(8).toList();
        var participants = distinctParticipants.stream()
                .map(player -> participant(analysis, player, start, end, deaths))
                .toList();

        Integer initiatorTeam = teamOf(analysis, initiator);
        var opposingPlayers = distinctParticipants.stream()
                .filter(player -> isOpposingTeam(analysis, initiatorTeam, player))
                .toList();

        String opponent = opposingPlayers.isEmpty() ? null : String.join(" и ", opposingPlayers);
        String winner = teamWinner(analysis, participants);

        long combatDeaths = deaths.stream()
                .map(event -> deathUnitName(analysis, event))
                .filter(this::isCombatUnit)
                .count();

        return new Combat(
                Duration.ofMillis(Math.round(start * 1000)),
                Duration.ofMillis(Math.round(end * 1000)),
                initiator,
                opponent,
                winner,
                participants,
                location(attack),
                combatDeaths == 0 ? 0.45 : 0.86
        );
    }

    private Combat.Participant participant(
            ReplayAnalysis analysis,
            String player,
            double start,
            double end,
            List<ReplayAnalysis.TimelineEvent> deaths
    ) {
        var playerDeaths = deaths.stream()
                .filter(event -> player.equalsIgnoreCase(deathOwnerName(analysis, event)))
                .map(event -> deathUnitName(analysis, event))
                .filter(Objects::nonNull)
                .toList();

        return new Combat.Participant(
                player,
                armyAt(analysis, player, start),
                armyAt(analysis, player, end),
                grouped(playerDeaths, this::isCombatUnit),
                grouped(playerDeaths, this::isWorker),
                grouped(playerDeaths, unit -> isStructure(unit) && !isStaticDefense(unit)),
                grouped(playerDeaths, this::isStaticDefense),
                completedUpgrades(analysis, player, start, this::isLevelUpgrade),
                completedUpgrades(analysis, player, start, upgrade -> !isLevelUpgrade(upgrade)),
                armyValueAt(analysis, player, start),
                armyValueAt(analysis, player, end)
        );
    }

    private String teamWinner(ReplayAnalysis analysis, List<Combat.Participant> participants) {
        var lossesByTeam = new LinkedHashMap<Integer, Double>();
        var playersByTeam = new LinkedHashMap<Integer, List<String>>();

        for (var participant : participants) {
            Integer team = teamOf(analysis, participant.player());
            if (team == null) continue;
            lossesByTeam.merge(team, observedLossScore(participant), Double::sum);
            playersByTeam.computeIfAbsent(team, ignored -> new ArrayList<>()).add(participant.player());
        }

        if (lossesByTeam.size() < 2) return null;
        var ordered = lossesByTeam.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();
        if (Math.abs(ordered.get(0).getValue() - ordered.get(1).getValue()) < 25.0) return null;
        return String.join(" и ", playersByTeam.getOrDefault(ordered.get(0).getKey(), List.of()));
    }

    private Map<String, Integer> grouped(List<String> units, Predicate<String> category) {
        return units.stream().filter(category)
                .collect(Collectors.groupingBy(Sc2DisplayNames::unit, LinkedHashMap::new,
                        Collectors.summingInt(ignored -> 1)));
    }

    private List<String> completedUpgrades(
            ReplayAnalysis analysis,
            String player,
            double at,
            Predicate<String> category
    ) {
        return analysis.timeline().stream()
                .filter(event -> value(event.time()) <= at)
                .filter(event -> player.equalsIgnoreCase(playerName(analysis, event.player())))
                .map(ReplayAnalysis.TimelineEvent::upgrade)
                .filter(Objects::nonNull)
                .filter(upgrade -> !upgrade.isBlank())
                .filter(category)
                .map(Sc2DisplayNames::upgrade)
                .flatMap(java.util.Optional::stream)
                .distinct()
                .sorted()
                .toList();
    }

    private Map<String, Integer> armyAt(ReplayAnalysis analysis, String player, double at) {
        var counts = new LinkedHashMap<String, Integer>();
        analysis.timeline().stream()
                .filter(event -> value(event.time()) <= at)
                .filter(event -> player.equalsIgnoreCase(ownerForLifecycleEvent(analysis, event)))
                .forEach(event -> {
                    String unit = lifecycleUnitName(analysis, event);
                    if (!isCombatUnit(unit)) return;
                    String displayName = Sc2DisplayNames.unit(unit);
                    if (isBirth(event)) counts.merge(displayName, 1, Integer::sum);
                    if (isDeath(event)) counts.computeIfPresent(displayName,
                            (ignored, count) -> Math.max(0, count - 1));
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
        return lower(event.event()).contains("command") && lower(event.ability()).contains("attack");
    }

    private boolean isBirth(ReplayAnalysis.TimelineEvent event) {
        String name = lower(event.event());
        return name.contains("born") || name.contains("finished") || name.contains("init");
    }

    private boolean isDeath(ReplayAnalysis.TimelineEvent event) {
        return lower(event.event()).contains("died");
    }

    private String lifecycleUnitName(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        return isDeath(event) ? deathUnitName(analysis, event) : event.unit();
    }

    private String ownerForLifecycleEvent(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        return isDeath(event) ? deathOwnerName(analysis, event) : playerName(analysis, event.player());
    }

    private String deathUnitName(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        if (event.unit() != null && !event.unit().isBlank()) return event.unit();
        if (event.victim() == null || event.victim().isBlank()) return null;
        return isKnownPlayerName(analysis, event.victim()) ? null : event.victim();
    }

    private String deathOwnerName(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        if (event.victim() != null && isKnownPlayerName(analysis, event.victim())) {
            return canonicalPlayerName(analysis, event.victim());
        }
        Object owner = event.attributes() == null ? null : event.attributes().get("owner");
        if (owner != null) return canonicalPlayerName(analysis, String.valueOf(owner));
        return null;
    }

    private String killerName(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        if (event.killer() != null && isKnownPlayerName(analysis, event.killer())) {
            return canonicalPlayerName(analysis, event.killer());
        }
        return playerName(analysis, event.player());
    }

    private boolean isKnownPlayerName(ReplayAnalysis analysis, String value) {
        return analysis.players().stream().anyMatch(player -> player.name().equalsIgnoreCase(value));
    }

    private String canonicalPlayerName(ReplayAnalysis analysis, String value) {
        return analysis.players().stream()
                .map(ReplayAnalysis.Player::name)
                .filter(name -> name.equalsIgnoreCase(value))
                .findFirst().orElse(value);
    }

    private Integer teamOf(ReplayAnalysis analysis, String player) {
        if (player == null) return null;
        return analysis.players().stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(player))
                .map(ReplayAnalysis.Player::team)
                .findFirst().orElse(null);
    }

    private boolean isOpposingTeam(ReplayAnalysis analysis, Integer initiatorTeam, String player) {
        Integer team = teamOf(analysis, player);
        return team != null && initiatorTeam != null && !team.equals(initiatorTeam);
    }

    private boolean isCombatUnit(String unit) {
        return unit != null && !unit.isBlank() && !isWorker(unit) && !isStructure(unit) && !isNoise(unit);
    }

    private boolean isWorker(String unit) {
        String value = lower(unit);
        return value.equals("scv") || value.equals("probe") || value.equals("drone") || value.equals("mule");
    }

    private boolean isStaticDefense(String unit) {
        String value = lower(unit);
        return value.contains("missileturret") || value.contains("photoncannon")
                || value.contains("spinecrawler") || value.contains("sporecrawler")
                || value.contains("bunker") || value.contains("planetaryfortress");
    }

    private boolean isStructure(String unit) {
        String value = lower(unit);
        return isStaticDefense(unit)
                || value.contains("commandcenter") || value.contains("orbitalcommand")
                || value.contains("nexus") || value.contains("hatchery") || value.contains("lair") || value.contains("hive")
                || value.contains("barracks") || value.contains("factory") || value.contains("starport")
                || value.contains("gateway") || value.contains("warpgate") || value.contains("pylon")
                || value.contains("supplydepot") || value.contains("refinery") || value.contains("assimilator")
                || value.contains("extractor") || value.contains("engineeringbay") || value.contains("armory")
                || value.contains("forge") || value.contains("spire") || value.contains("den")
                || value.contains("core") || value.contains("archive") || value.contains("bay")
                || value.contains("pool") || value.contains("chamber") || value.contains("nest")
                || value.contains("warren");
    }

    private boolean isNoise(String unit) {
        String value = lower(unit);
        return value.startsWith("beacon") || value.contains("mineralfield") || value.contains("vespeneg")
                || value.contains("destructible") || value.contains("watchtower") || value.contains("xelnaga")
                || value.contains("larva") || value.contains("egg") || value.contains("creep")
                || value.contains("broodlingescort");
    }

    private boolean isLevelUpgrade(String upgrade) {
        String value = lower(upgrade);
        return value.contains("weapon") || value.contains("armor") || value.contains("armour")
                || value.contains("shield") || value.matches(".*level[123].*");
    }

    private boolean hasObservedCombatLoss(Combat combat) {
        return combat.participants().stream().anyMatch(participant -> !participant.unitsLost().isEmpty());
    }

    private double observedLossScore(Combat.Participant participant) {
        double armyLoss = Math.max(0, participant.armyValueBefore() - participant.armyValueAfter());
        return armyLoss
                + count(participant.unitsLost()) * 25.0
                + count(participant.workersLost()) * 50.0
                + (count(participant.structuresLost()) + count(participant.staticDefenseLost())) * 200.0;
    }

    private int count(Map<String, Integer> values) {
        return values.values().stream().mapToInt(Integer::intValue).sum();
    }

    private String playerName(ReplayAnalysis analysis, Object player) {
        if (player == null) return null;
        if (player instanceof Number number) {
            return analysis.players().stream()
                    .filter(candidate -> candidate.pid() != null && candidate.pid() == number.intValue())
                    .map(ReplayAnalysis.Player::name).findFirst().orElse(null);
        }
        String raw = String.valueOf(player);
        return analysis.players().stream()
                .filter(candidate -> raw.equalsIgnoreCase(candidate.name()) || raw.equals(String.valueOf(candidate.pid())))
                .map(ReplayAnalysis.Player::name).findFirst().orElse(null);
    }

    private void addDistinct(List<String> values, String value) {
        if (value != null && values.stream().noneMatch(existing -> existing.equalsIgnoreCase(value))) values.add(value);
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

    private static final class SpacedAttackPredicate implements Predicate<ReplayAnalysis.TimelineEvent> {
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
