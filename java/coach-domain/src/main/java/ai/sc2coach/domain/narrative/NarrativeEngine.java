package ai.sc2coach.domain.narrative;

import ai.sc2coach.domain.delta.ArgumentDelta;
import ai.sc2coach.domain.episode.Episode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NarrativeEngine {

    public MatchNarrative build(List<Episode> episodes, List<ArgumentDelta> deltas, String finalLeader) {
        var beats = new ArrayList<MatchNarrative.Beat>();
        if (episodes != null) {
            for (Episode episode : episodes) {
                var nearby = nearbyDeltas(episode, deltas);
                beats.add(new MatchNarrative.Beat(
                        episode.from(),
                        kind(episode, nearby),
                        episode.title(),
                        statement(episode, nearby),
                        evidence(episode, nearby)
                ));
            }
        }
        beats.sort(Comparator.comparing(MatchNarrative.Beat::at));
        String summary = beats.isEmpty()
                ? "Недостаточно данных, чтобы построить историю матча."
                : "Матч разбит на " + beats.size() + " значимых эпизодов. В финальном измеренном состоянии лидировал "
                + (finalLeader == null ? "неопределённый игрок" : finalLeader) + ".";
        return new MatchNarrative(summary, beats);
    }

    private List<ArgumentDelta> nearbyDeltas(Episode episode, List<ArgumentDelta> deltas) {
        if (deltas == null) return List.of();
        return deltas.stream()
                .filter(delta -> delta.to().compareTo(episode.from()) >= 0
                        && delta.from().compareTo(episode.to()) <= 0)
                .sorted(Comparator.comparingInt((ArgumentDelta delta) -> weight(delta.significance())).reversed())
                .limit(3)
                .toList();
    }

    private MatchNarrative.Kind kind(Episode episode, List<ArgumentDelta> deltas) {
        boolean collapse = deltas.stream().anyMatch(delta -> delta.component() == ArgumentDelta.Component.ARMY
                && delta.relativeChangePercent() <= -40);
        if (collapse) return MatchNarrative.Kind.COLLAPSE;
        return switch (episode.type()) {
            case OPENING -> MatchNarrative.Kind.OPENING;
            case LEAD_CHANGE -> MatchNarrative.Kind.ADVANTAGE;
            case MAJOR_FIGHT -> MatchNarrative.Kind.FIGHT;
            case RECOVERY -> MatchNarrative.Kind.RECOVERY;
            case TECH_TRANSITION -> MatchNarrative.Kind.TRANSITION;
            default -> MatchNarrative.Kind.CONCLUSION;
        };
    }

    private String statement(Episode episode, List<ArgumentDelta> deltas) {
        var critical = deltas.stream().filter(delta -> delta.significance() == ArgumentDelta.Significance.CRITICAL)
                .findFirst();
        if (critical.isPresent()) {
            var delta = critical.get();
            return episode.title() + ": показатель «" + component(delta.component()) + "» игрока "
                    + delta.playerName() + " изменился на " + percent(delta.relativeChangePercent()) + ".";
        }
        return episode.title() + ". Эпизод требует проверки по таймлайну и доступным доказательствам.";
    }

    private List<String> evidence(Episode episode, List<ArgumentDelta> deltas) {
        var result = new ArrayList<String>();
        result.addAll(episode.evidence());
        deltas.forEach(delta -> result.add(delta.playerName() + ": " + component(delta.component())
                + " " + percent(delta.relativeChangePercent())));
        return result.stream().distinct().limit(5).toList();
    }

    private int weight(ArgumentDelta.Significance significance) {
        return switch (significance) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private String component(ArgumentDelta.Component component) {
        return switch (component) {
            case ECONOMY -> "экономика";
            case ARMY -> "армия";
            case SUPPLY -> "снабжение";
            case OVERALL -> "общее преимущество";
        };
    }

    private String percent(double value) {
        return (value >= 0 ? "+" : "") + Math.round(value * 10.0) / 10.0 + "%";
    }
}
