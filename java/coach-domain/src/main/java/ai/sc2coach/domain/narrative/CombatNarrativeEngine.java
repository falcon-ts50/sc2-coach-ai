package ai.sc2coach.domain.narrative;

import ai.sc2coach.domain.combat.Combat;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CombatNarrativeEngine {

    public String render(String focusPlayer, List<Combat> combats, String fallbackNarrative) {
        if (combats == null || combats.isEmpty()) return fallbackNarrative;

        var ordered = combats.stream()
                .sorted(Comparator.comparing(Combat::startedAt))
                .toList();
        var opening = ordered.getFirst();
        var decisive = ordered.stream()
                .max(Comparator.comparingDouble(this::focusArmyLossShare))
                .orElse(opening);
        var last = ordered.getLast();

        return String.join("\n\n",
                introduction(focusPlayer, opening),
                combatParagraph(focusPlayer, decisive, "Самым тяжёлым эпизодом"),
                conclusion(focusPlayer, last, ordered.size(), fallbackNarrative)
        );
    }

    private String introduction(String focusPlayer, Combat combat) {
        String subject = focusPlayer == null ? "Выбранный игрок" : focusPlayer;
        String action = subject.equalsIgnoreCase(combat.initiator())
                ? "начал атаку на " + safe(combat.opponent(), "соперника")
                : safe(combat.initiator(), "Соперник") + " атаковал " + subject;
        return "Первый надёжно восстановленный бой начался на " + clock(combat.startedAt().toSeconds())
                + ": " + action + ". " + resultSentence(subject, combat);
    }

    private String combatParagraph(String focusPlayer, Combat combat, String prefix) {
        var participant = participant(combat, focusPlayer);
        if (participant == null) return prefix + " стал бой на " + clock(combat.startedAt().toSeconds()) + ".";

        String before = composition(participant.armyBefore());
        String after = composition(participant.armyAfter());
        String losses = composition(participant.unitsLost());
        double change = participant.armyValueBefore() <= 0 ? 0
                : (participant.armyValueAfter() - participant.armyValueBefore()) / participant.armyValueBefore() * 100;

        return prefix + " стал бой на " + clock(combat.startedAt().toSeconds()) + ". До него армия "
                + participant.player() + " включала " + before + "; после боя осталось " + after
                + ". За эпизод потеряно: " + losses + ". Стоимость армии изменилась с "
                + Math.round(participant.armyValueBefore()) + " до " + Math.round(participant.armyValueAfter())
                + " ресурсов (" + signed(change) + "%). " + resultSentence(participant.player(), combat);
    }

    private String conclusion(String focusPlayer, Combat combat, int count, String fallback) {
        String subject = focusPlayer == null ? "выбранного игрока" : focusPlayer;
        String finalResult = combat.winner() == null
                ? "результат последнего боя нельзя определить надёжно"
                : combat.winner().equalsIgnoreCase(focusPlayer)
                    ? "последний бой остался за " + subject
                    : "последний бой выиграл " + combat.winner();
        return "Всего выделено " + count + " боевых эпизодов; " + finalResult + ". "
                + (fallback == null ? "" : fallback);
    }

    private String resultSentence(String player, Combat combat) {
        if (combat.winner() == null) return "Победитель эпизода не определён из доступных событий.";
        return combat.winner().equalsIgnoreCase(player)
                ? player + " закончил размен с меньшими потерями."
                : "Размен выиграл " + combat.winner() + ".";
    }

    private Combat.Participant participant(Combat combat, String focusPlayer) {
        return combat.participants().stream()
                .filter(candidate -> focusPlayer != null && candidate.player().equalsIgnoreCase(focusPlayer))
                .findFirst()
                .orElseGet(() -> combat.participants().stream().findFirst().orElse(null));
    }

    private double focusArmyLossShare(Combat combat) {
        return combat.participants().stream()
                .mapToDouble(participant -> participant.armyValueBefore() <= 0 ? 0
                        : Math.max(0, participant.armyValueBefore() - participant.armyValueAfter()) / participant.armyValueBefore())
                .max().orElse(0);
    }

    private String composition(Map<String, Integer> composition) {
        if (composition == null || composition.isEmpty()) return "состав не восстановлен";
        return composition.entrySet().stream()
                .limit(8)
                .map(entry -> entry.getValue() + " × " + entry.getKey())
                .collect(Collectors.joining(", "));
    }

    private String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private String signed(double value) { return (value > 0 ? "+" : "") + Math.round(value); }
    private String clock(long seconds) { return (seconds / 60) + ":" + String.format("%02d", seconds % 60); }
}
