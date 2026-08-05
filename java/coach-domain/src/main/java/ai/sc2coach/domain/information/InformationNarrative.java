package ai.sc2coach.domain.information;

import java.util.stream.Collectors;

public record InformationNarrative(InformationEpisode episode, String text) {

    public static InformationNarrative from(InformationEpisode episode) {
        String observed = episode.potentiallyObserved().stream()
                .map(InformationObservation::subject)
                .distinct()
                .limit(4)
                .collect(Collectors.joining(", "));
        String reaction = episode.reactionCandidates().stream()
                .findFirst()
                .map(candidate -> " Через " + candidate.delaySeconds()
                        + " секунд " + candidate.player() + " начал: " + candidate.action()
                        + ". Это решение согласуется с возможной реакцией на разведданные.")
                .orElse("");

        String text = "На " + clock(episode.start()) + " " + episode.scoutUnit() + " "
                + episode.scoutPlayer() + " вошёл в область игрока " + episode.targetPlayer() + ".";
        if (!observed.isBlank()) {
            text += " Во время разведки он потенциально мог увидеть: " + observed + ".";
        }
        if (!episode.missingInformation().isEmpty()) {
            text += " Часть информации осталась неизвестной: "
                    + episode.missingInformation().stream()
                    .map(InformationGap::topic)
                    .distinct()
                    .limit(3)
                    .collect(Collectors.joining(", "))
                    + ".";
        }
        return new InformationNarrative(episode, text + reaction);
    }

    private static String clock(java.time.Duration duration) {
        long seconds = duration.toSeconds();
        return "%d:%02d".formatted(seconds / 60, seconds % 60);
    }
}
