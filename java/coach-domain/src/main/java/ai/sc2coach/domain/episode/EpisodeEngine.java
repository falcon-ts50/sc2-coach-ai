package ai.sc2coach.domain.episode;

import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.decision.Decision;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EpisodeEngine {

    private static final Duration MERGE_WINDOW = Duration.ofSeconds(75);

    public List<Episode> build(List<TurningPoint> turningPoints, List<Decision> decisions) {
        var candidates = new ArrayList<Episode>();
        if (turningPoints != null) {
            turningPoints.forEach(point -> candidates.add(new Episode(
                    point.previousLeaderName() == null ? Episode.Type.MAJOR_FIGHT : Episode.Type.LEAD_CHANGE,
                    point.at().minusSeconds(Math.min(20, point.at().toSeconds())),
                    point.at().plusSeconds(20),
                    point.newLeaderName(),
                    point.previousLeaderName() == null
                            ? "Крупное изменение баланса сил"
                            : "Инициатива перешла к " + safe(point.newLeaderName()),
                    Math.abs(point.scoreSwing()),
                    point.reasons().stream().limit(3)
                            .map(reason -> reason.component() + " " + signed(reason.change()))
                            .toList()
            )));
        }
        if (decisions != null) {
            decisions.forEach(decision -> candidates.add(fromDecision(decision)));
        }
        candidates.sort(Comparator.comparing(Episode::from));
        return merge(candidates);
    }

    private Episode fromDecision(Decision decision) {
        Episode.Type type = switch (decision.type()) {
            case ATTACK -> Episode.Type.MAJOR_FIGHT;
            case REBUILD -> Episode.Type.RECOVERY;
            case EXPAND -> Episode.Type.EXPANSION;
            case TECH_SWITCH -> Episode.Type.TECH_TRANSITION;
            default -> Episode.Type.OTHER;
        };
        String title = switch (type) {
            case MAJOR_FIGHT -> "Крупный боевой эпизод";
            case RECOVERY -> "Восстановление армии";
            case EXPANSION -> "Вероятное расширение";
            case TECH_TRANSITION -> "Вероятный технологический переход";
            default -> "Игровое решение";
        };
        return new Episode(type, decision.startedAt(), decision.endedAt(), null, title,
                decision.confidence().value() * 100,
                decision.evidence().stream().limit(3).map(Object::toString).toList());
    }

    private List<Episode> merge(List<Episode> candidates) {
        if (candidates.isEmpty()) return List.of();
        var result = new ArrayList<Episode>();
        for (Episode candidate : candidates) {
            if (result.isEmpty()) {
                result.add(candidate);
                continue;
            }
            Episode previous = result.getLast();
            boolean close = !candidate.from().minus(previous.to()).isNegative()
                    && candidate.from().minus(previous.to()).compareTo(MERGE_WINDOW) <= 0;
            boolean compatible = previous.type() == candidate.type()
                    || previous.type() == Episode.Type.LEAD_CHANGE
                    || candidate.type() == Episode.Type.LEAD_CHANGE;
            if (close && compatible) {
                var evidence = new ArrayList<>(previous.evidence());
                evidence.addAll(candidate.evidence());
                result.set(result.size() - 1, new Episode(
                        dominant(previous, candidate).type(),
                        previous.from(),
                        candidate.to().compareTo(previous.to()) > 0 ? candidate.to() : previous.to(),
                        candidate.actor() != null ? candidate.actor() : previous.actor(),
                        dominant(previous, candidate).title(),
                        Math.max(previous.importance(), candidate.importance()),
                        evidence.stream().distinct().limit(5).toList()
                ));
            } else {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    private static Episode dominant(Episode left, Episode right) {
        return right.importance() > left.importance() ? right : left;
    }

    private static String safe(String value) { return value == null ? "неизвестному игроку" : value; }
    private static String signed(double value) { return (value >= 0 ? "+" : "") + Math.round(value * 10.0) / 10.0; }
}
