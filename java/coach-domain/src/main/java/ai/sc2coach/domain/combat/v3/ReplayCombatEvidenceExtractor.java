package ai.sc2coach.domain.combat.v3;

import ai.sc2coach.domain.ReplayAnalysis;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ReplayCombatEvidenceExtractor {

    public List<CombatEvidence> extract(ReplayAnalysis analysis) {
        Objects.requireNonNull(analysis, "analysis");
        return analysis.timeline().stream()
                .map(event -> toEvidence(analysis, event))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(CombatEvidence::time))
                .toList();
    }

    private Optional<CombatEvidence> toEvidence(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
        if (isDeath(event)) {
            return Optional.of(CombatEvidence.death(
                    id("death", event),
                    time(event),
                    deathOwnerName(analysis, event),
                    killerName(analysis, event),
                    deathUnitName(analysis, event),
                    location(event)
            ));
        }
        if (isCombatCommand(event)) {
            return Optional.of(CombatEvidence.combatCommand(
                    id("command", event),
                    time(event),
                    playerName(analysis, event.player()),
                    event.ability(),
                    location(event)
            ));
        }
        if (isCombatAbility(event)) {
            return Optional.of(CombatEvidence.combatAbility(
                    id("ability", event),
                    time(event),
                    playerName(analysis, event.player()),
                    event.ability(),
                    location(event)
            ));
        }
        return Optional.empty();
    }

    private boolean isDeath(ReplayAnalysis.TimelineEvent event) {
        return lower(event.event()).contains("died");
    }

    private boolean isCombatCommand(ReplayAnalysis.TimelineEvent event) {
        return lower(event.event()).contains("command") && isCombatAbilityName(event.ability());
    }

    private boolean isCombatAbility(ReplayAnalysis.TimelineEvent event) {
        String name = lower(event.event());
        return (name.contains("ability") || name.contains("spell")) && isCombatAbilityName(event.ability());
    }

    private boolean isCombatAbilityName(String ability) {
        String value = lower(ability);
        return value.contains("attack")
                || value.contains("stim")
                || value.contains("blink")
                || value.contains("yamato")
                || value.contains("snipe")
                || value.contains("storm")
                || value.contains("fungal")
                || value.contains("bile")
                || value.contains("grenade")
                || value.contains("disruptor");
    }

    private Duration time(ReplayAnalysis.TimelineEvent event) {
        return Duration.ofMillis(Math.round(value(event.time()) * 1000));
    }

    private String id(String prefix, ReplayAnalysis.TimelineEvent event) {
        return prefix + ":" + Math.round(value(event.time()) * 1000) + ":" + lower(event.event()) + ":"
                + lower(event.unit()) + ":" + lower(event.ability());
    }

    private MapPoint location(ReplayAnalysis.TimelineEvent event) {
        var position = event.targetPosition() != null ? event.targetPosition() : event.position();
        if (position == null || position.x() == null || position.y() == null) return null;
        return new MapPoint(position.x(), position.y());
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
        return value != null && analysis.players().stream().anyMatch(player -> player.name().equalsIgnoreCase(value));
    }

    private String canonicalPlayerName(ReplayAnalysis analysis, String value) {
        if (value == null) return null;
        return analysis.players().stream()
                .map(ReplayAnalysis.Player::name)
                .filter(name -> name.equalsIgnoreCase(value))
                .findFirst().orElse(value);
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
                .map(ReplayAnalysis.Player::name).findFirst().orElse(raw);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static double value(Number number) {
        return number == null ? 0 : number.doubleValue();
    }
}
