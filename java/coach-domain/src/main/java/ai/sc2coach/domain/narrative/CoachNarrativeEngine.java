package ai.sc2coach.domain.narrative;

import java.util.List;

public final class CoachNarrativeEngine {

    public String render(MatchNarrative narrative) {
        if (narrative == null || narrative.beats().isEmpty()) {
            return "В реплее пока недостаточно измеренных эпизодов для связного разбора.";
        }
        List<MatchNarrative.Beat> beats = narrative.beats();
        var text = new StringBuilder();
        text.append("Матч удалось разбить на ").append(beats.size()).append(" значимых эпизодов. ");

        MatchNarrative.Beat first = beats.getFirst();
        text.append("Первый заметный эпизод произошёл на ").append(clock(first.at().toSeconds()))
                .append(": ").append(lower(first.statement())).append(' ');

        beats.stream()
                .filter(beat -> beat.kind() == MatchNarrative.Kind.COLLAPSE
                        || beat.kind() == MatchNarrative.Kind.ADVANTAGE)
                .findFirst()
                .ifPresent(beat -> text.append("Ключевой сдвиг произошёл на ")
                        .append(clock(beat.at().toSeconds())).append(": ")
                        .append(lower(beat.statement())).append(' '));

        MatchNarrative.Beat last = beats.getLast();
        if (last != first) {
            text.append("Последний значимый эпизод начался на ")
                    .append(clock(last.at().toSeconds())).append(": ")
                    .append(lower(last.statement())).append(' ');
        }
        text.append(narrative.summary());
        return text.toString().replaceAll("\\s+", " ").trim();
    }

    private static String lower(String value) {
        if (value == null || value.isBlank()) return "данных недостаточно";
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String clock(long seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
