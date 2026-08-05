package ai.sc2coach.domain.narrative.analysis;

import ai.sc2coach.domain.combat.Combat;
import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.decision.Decision;
import ai.sc2coach.domain.knowledge.Recommendation;
import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Completeness;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Interval;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Marker;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Point;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Series;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class NarrativeAnalysisEngine {

    private final NarrativeAnalysisConfig config;

    public NarrativeAnalysisEngine() {
        this(NarrativeAnalysisConfig.defaults());
    }

    public NarrativeAnalysisEngine(NarrativeAnalysisConfig config) {
        this.config = config == null ? NarrativeAnalysisConfig.defaults() : config;
    }

    public NarrativeAnalysis analyze(NarrativeAnalysisInput input) {
        if (input == null || input.match() == null || input.matchContext() == null
                || input.matchContext().timeline().size() < 2) {
            return NarrativeAnalysis.empty(input == null ? null : input.focusPlayer());
        }

        Match match = input.match();
        PlayerState focus = resolveFocus(match, input.focusPlayer()).orElse(null);
        if (focus == null) return NarrativeAnalysis.empty(input.focusPlayer());

        List<String> team = focusTeam(match, focus);
        List<NarrativeEvent> events = normalizeEvents(input, focus);
        List<MatchStateSnapshot> snapshots = snapshots(input.matchContext(), focus, team);
        List<StateTransition> transitions = transitions(snapshots, events);
        List<MatchPhase> phases = phases(snapshots, transitions, events);
        List<CausalLink> links = causalLinks(phases, transitions);
        NarrativeTimeline timeline = new NarrativeTimeline(events, snapshots, transitions, phases, links);
        NarrativeChartModel chart = chart(snapshots, phases, events);
        NarrativeSummary summary = summary(match, focus, team, phases, transitions, links);

        return new NarrativeAnalysis(
                "narrative-analysis.v1",
                focus.name(),
                focus.pid(),
                team,
                officialResult(focus),
                "PRELIMINARY",
                "NOT_EVALUATED",
                timeline,
                summary,
                chart,
                List.of(
                        "Narrative Analysis consumes existing deterministic engines and does not infer a strategic result.",
                        "Replay data does not prove intent or causality; causal links use cautious precedence/contribution language.",
                        "The chart uses match-context army value, economy proxy and supply series; full bases/production queues are not complete in V1."
                )
        );
    }

    private Optional<PlayerState> resolveFocus(Match match, String focusPlayer) {
        if (focusPlayer != null && !focusPlayer.isBlank()) {
            return match.players().stream()
                    .filter(player -> player.name().equalsIgnoreCase(focusPlayer))
                    .findFirst();
        }
        return match.players().stream().findFirst();
    }

    private List<String> focusTeam(Match match, PlayerState focus) {
        if (focus.team() == null) return List.of(focus.name());
        return match.players().stream()
                .filter(player -> Objects.equals(player.team(), focus.team()))
                .map(PlayerState::name)
                .sorted()
                .toList();
    }

    private List<NarrativeEvent> normalizeEvents(NarrativeAnalysisInput input, PlayerState focus) {
        List<NarrativeEvent> events = new ArrayList<>();
        int index = 1;
        for (Combat combat : input.combats()) {
            events.add(new NarrativeEvent(
                    combat.id(),
                    NarrativeEvent.Kind.COMBAT,
                    combat.startedAt(),
                    combat.endedAt(),
                    focus.name(),
                    focus.pid(),
                    combat.ordinalLabel() == null ? "Бой " + index : combat.ordinalLabel(),
                    "CombatEngine",
                    combat.confidence(),
                    List.of(combat.id()),
                    Map.of(
                            "initiator", value(combat.initiator()),
                            "opponent", value(combat.opponent()),
                            "location", value(combat.location())
                    )
            ));
            index++;
        }
        index = 1;
        for (TurningPoint point : input.turningPoints()) {
            events.add(new NarrativeEvent(
                    "turning-point-" + index,
                    NarrativeEvent.Kind.TURNING_POINT,
                    point.at(),
                    point.at(),
                    point.newLeaderName(),
                    point.newLeaderPid(),
                    "Перелом: " + value(point.newLeaderName()),
                    "TurningPointEngine",
                    severityConfidence(point.severity()),
                    List.of("turning-point-" + index),
                    Map.of("scoreSwing", point.scoreSwing(), "severity", point.severity().name())
            ));
            index++;
        }
        for (Decision decision : input.decisions()) {
            if (decision.playerPid() != focus.pid()) continue;
            events.add(new NarrativeEvent(
                    decision.id(),
                    NarrativeEvent.Kind.DECISION,
                    decision.startedAt(),
                    decision.endedAt(),
                    focus.name(),
                    focus.pid(),
                    "Решение: " + decision.type().name(),
                    "DecisionEngine",
                    decision.confidence().value(),
                    List.of(decision.id()),
                    Map.of("type", decision.type().name())
            ));
        }
        for (Recommendation recommendation : input.recommendations()) {
            events.add(new NarrativeEvent(
                    recommendation.id(),
                    NarrativeEvent.Kind.RECOMMENDATION,
                    Duration.ZERO,
                    Duration.ZERO,
                    focus.name(),
                    focus.pid(),
                    recommendation.title(),
                    "KnowledgeEngine",
                    recommendation.confidence(),
                    List.of(recommendation.id()),
                    Map.of("category", recommendation.category().name(), "priority", recommendation.priority().name())
            ));
        }
        events.sort(Comparator.comparing(NarrativeEvent::at).thenComparing(NarrativeEvent::id));
        return List.copyOf(events);
    }

    private List<MatchStateSnapshot> snapshots(MatchContext context, PlayerState focus, List<String> team) {
        List<MatchStateSnapshot> snapshots = new ArrayList<>();
        int index = 0;
        for (MatchContext.ContextFrame frame : context.timeline()) {
            MatchContext.PlayerContext player = frame.players().stream()
                    .filter(candidate -> candidate.pid() == focus.pid())
                    .findFirst()
                    .orElse(null);
            if (player == null) continue;
            Map<String, MatchStateSnapshot.Metrics> all = new LinkedHashMap<>();
            for (MatchContext.PlayerContext candidate : frame.players()) {
                all.put(candidate.name(), metrics(candidate));
            }
            snapshots.add(new MatchStateSnapshot(
                    snapshotId(index, frame.at()),
                    frame.at(),
                    focus.name(),
                    focus.pid(),
                    team,
                    metrics(player),
                    all,
                    0.86,
                    List.of("Economy is a worker/income proxy from the existing MatchContext, not a full bank-spend model.")
            ));
            index++;
        }
        return List.copyOf(snapshots);
    }

    private MatchStateSnapshot.Metrics metrics(MatchContext.PlayerContext player) {
        return new MatchStateSnapshot.Metrics(
                player.army().absoluteValue(),
                player.economy().absoluteValue(),
                player.supply().absoluteValue(),
                0,
                player.overallScore()
        );
    }

    private List<StateTransition> transitions(List<MatchStateSnapshot> snapshots, List<NarrativeEvent> events) {
        if (snapshots.size() < 2) return List.of();

        List<StateTransition> result = new ArrayList<>();
        addTransition(result, bestDrop(snapshots, Duration.ZERO, config.earlyPhaseMax(), Metric.ARMY),
                StateTransition.Kind.EARLY_DECLINE, "Ранний спад боевой силы");
        addTransition(result, firstRecoveryAfter(snapshots, result, config.midPhaseMax(), Metric.ARMY),
                StateTransition.Kind.DEFENSIVE_ADAPTATION, "Адаптация и восстановление армии");
        Duration midgameFrom = result.stream().map(StateTransition::to).max(Duration::compareTo).orElse(config.earlyPhaseMax());
        addTransition(result, bestRise(snapshots, midgameFrom, config.midPhaseMax(), Metric.OVERALL),
                StateTransition.Kind.MIDGAME_IMPROVEMENT, "Средняя стадия: улучшение общего состояния");
        addTransition(result, bestDrop(snapshots, config.midPhaseMax(), lastAt(snapshots), Metric.ARMY),
                StateTransition.Kind.LATE_DETERIORATION, "Позднее ухудшение позиции");

        return result.stream()
                .filter(transition -> Math.abs(transition.metricDelta().values().stream()
                        .mapToDouble(Double::doubleValue).sum()) > 0)
                .sorted(Comparator.comparing(StateTransition::from).thenComparing(StateTransition::id))
                .toList();
    }

    private void addTransition(List<StateTransition> result, Candidate candidate, StateTransition.Kind kind, String title) {
        if (candidate == null || candidate.before == null || candidate.after == null) return;
        double confidence = switch (kind) {
            case EARLY_DECLINE -> Math.abs(candidate.delta) >= config.armySwingThreshold() ? 0.78 : 0.62;
            case DEFENSIVE_ADAPTATION -> Math.abs(candidate.delta) >= config.armySwingThreshold() ? 0.76 : 0.61;
            case MIDGAME_IMPROVEMENT -> Math.abs(candidate.delta) >= config.overallSwingThreshold() ? 0.73 : 0.6;
            case LATE_DETERIORATION -> Math.abs(candidate.delta) >= config.overallSwingThreshold() ? 0.75 : 0.6;
            default -> 0.55;
        };
        String id = "transition-" + result.size();
        result.add(new StateTransition(
                id,
                kind,
                candidate.before.at(),
                candidate.after.at(),
                candidate.before.id(),
                candidate.after.id(),
                Map.of(candidate.metric.id, candidate.delta),
                title + ": " + rounded(candidate.before.metrics(), candidate.metric)
                        + " → " + rounded(candidate.after.metrics(), candidate.metric),
                confidence,
                List.of(candidate.before.id(), candidate.after.id()),
                List.of("This transition is deterministic from context snapshots, but threshold labels are configurable heuristics.")
        ));
    }

    private Candidate bestDrop(List<MatchStateSnapshot> snapshots, Duration from, Duration to, Metric metric) {
        return bestDelta(snapshots, from, to, metric, true);
    }

    private Candidate bestRise(List<MatchStateSnapshot> snapshots, Duration from, Duration to, Metric metric) {
        return bestDelta(snapshots, from, to, metric, false);
    }

    private Candidate bestRiseAfter(List<MatchStateSnapshot> snapshots, List<StateTransition> previous, Duration to, Metric metric) {
        Duration from = previous.stream().map(StateTransition::to).max(Duration::compareTo).orElse(Duration.ZERO);
        return bestRise(snapshots, from, to, metric);
    }

    private Candidate firstRecoveryAfter(List<MatchStateSnapshot> snapshots, List<StateTransition> previous, Duration to, Metric metric) {
        Duration from = previous.stream().map(StateTransition::to).max(Duration::compareTo).orElse(Duration.ZERO);
        MatchStateSnapshot base = snapshots.stream()
                .filter(snapshot -> snapshot.at().compareTo(from) >= 0)
                .findFirst()
                .orElse(null);
        if (base == null) return bestRiseAfter(snapshots, previous, to, metric);
        double threshold = metric == Metric.ARMY ? config.armySwingThreshold() / 2 : config.overallSwingThreshold();
        Candidate fallback = null;
        for (MatchStateSnapshot after : snapshots) {
            if (after.at().compareTo(base.at()) <= 0 || after.at().compareTo(to) > 0) continue;
            double delta = value(after.metrics(), metric) - value(base.metrics(), metric);
            if (delta <= 0) continue;
            Candidate candidate = new Candidate(base, after, metric, delta);
            if (delta >= threshold) return candidate;
            if (fallback == null || delta > fallback.delta) fallback = candidate;
        }
        return fallback;
    }

    private Candidate bestDelta(List<MatchStateSnapshot> snapshots, Duration from, Duration to, Metric metric, boolean negative) {
        Candidate best = null;
        for (int i = 0; i < snapshots.size(); i++) {
            MatchStateSnapshot before = snapshots.get(i);
            if (before.at().compareTo(from) < 0 || before.at().compareTo(to) > 0) continue;
            for (int j = i + 1; j < snapshots.size(); j++) {
                MatchStateSnapshot after = snapshots.get(j);
                if (after.at().compareTo(to) > 0) break;
                double delta = value(after.metrics(), metric) - value(before.metrics(), metric);
                if ((negative && delta >= 0) || (!negative && delta <= 0)) continue;
                if (best == null || Math.abs(delta) > Math.abs(best.delta)) {
                    best = new Candidate(before, after, metric, delta);
                }
            }
        }
        return best;
    }

    private List<MatchPhase> phases(List<MatchStateSnapshot> snapshots, List<StateTransition> transitions, List<NarrativeEvent> events) {
        if (snapshots.isEmpty()) return List.of();
        Duration start = snapshots.getFirst().at();
        Duration end = snapshots.getLast().at();
        List<StateTransition> ordered = transitions.stream()
                .sorted(Comparator.comparing(StateTransition::from))
                .toList();
        if (ordered.isEmpty()) {
            return List.of(new MatchPhase("phase-0", MatchPhase.Kind.UNKNOWN, "Сценарий без устойчивых фаз",
                    start, end, "Недостаточно выраженных переходов, чтобы выделить фазы.", snapshots.getFirst().id(),
                    snapshots.getLast().id(), List.of(), eventIds(events, start, end), 0.45, List.of()));
        }

        List<MatchPhase> phases = new ArrayList<>();
        int index = 0;
        StateTransition first = ordered.getFirst();
        if (first.from().compareTo(start) > 0) {
            phases.add(new MatchPhase(
                    "phase-" + index,
                    MatchPhase.Kind.OPENING,
                    phaseTitle(MatchPhase.Kind.OPENING),
                    start,
                    first.from(),
                    "Открытие до первого выраженного перехода: график ещё не показывает устойчивого боевого перелома.",
                    snapshots.getFirst().id(),
                    first.beforeSnapshotId(),
                    List.of(),
                    eventIds(events, start, first.from()),
                    config.phaseConfidence(),
                    List.of("Opening label is derived from match-context timeline boundaries, not from a complete build-order classifier.")
            ));
            index++;
        }
        for (StateTransition transition : ordered) {
            MatchPhase.Kind kind = phaseKind(transition.kind());
            phases.add(new MatchPhase(
                    "phase-" + index,
                    kind,
                    phaseTitle(kind),
                    transition.from(),
                    transition.to(),
                    phaseSummary(kind, transition),
                    transition.beforeSnapshotId(),
                    transition.afterSnapshotId(),
                    List.of(transition.id()),
                    eventIds(events, transition.from(), transition.to()),
                    Math.max(config.phaseConfidence(), transition.confidence()),
                    transition.limitations()
            ));
            index++;
        }
        return List.copyOf(phases);
    }

    private MatchPhase.Kind phaseKind(StateTransition.Kind kind) {
        return switch (kind) {
            case EARLY_DECLINE -> MatchPhase.Kind.PRESSURE;
            case DEFENSIVE_ADAPTATION -> MatchPhase.Kind.STABILIZATION;
            case MIDGAME_IMPROVEMENT -> MatchPhase.Kind.MIDGAME;
            case LATE_DETERIORATION -> MatchPhase.Kind.DETERIORATION;
            default -> MatchPhase.Kind.UNKNOWN;
        };
    }

    private String phaseTitle(MatchPhase.Kind kind) {
        return switch (kind) {
            case PRESSURE -> "Раннее давление";
            case STABILIZATION -> "Стабилизация";
            case MIDGAME -> "Средняя стадия";
            case DETERIORATION -> "Позднее ухудшение";
            case OPENING -> "Открытие";
            case CLOSING -> "Завершение";
            default -> "Неустойчивая фаза";
        };
    }

    private String phaseSummary(MatchPhase.Kind kind, StateTransition transition) {
        return switch (kind) {
            case PRESSURE -> "Контекст показывает раннее проседание одного из ключевых показателей. Это не оценка результата, а граница фазы.";
            case STABILIZATION -> "После раннего давления видна попытка восстановить состояние, что согласуется с адаптацией к ходу матча.";
            case MIDGAME -> "В средней стадии показатели улучшаются относительно предыдущего отрезка.";
            case DETERIORATION -> "Позднее состояние ухудшается относительно более сильной средней стадии.";
            default -> transition.interpretation();
        };
    }

    private List<String> eventIds(List<NarrativeEvent> events, Duration from, Duration to) {
        return events.stream()
                .filter(event -> event.at().compareTo(from) >= 0 && event.at().compareTo(to) <= 0)
                .map(NarrativeEvent::id)
                .toList();
    }

    private List<CausalLink> causalLinks(List<MatchPhase> phases, List<StateTransition> transitions) {
        List<CausalLink> links = new ArrayList<>();
        for (int i = 1; i < phases.size(); i++) {
            MatchPhase previous = phases.get(i - 1);
            MatchPhase current = phases.get(i);
            CausalLink.Kind kind = current.kind() == MatchPhase.Kind.STABILIZATION
                    ? CausalLink.Kind.RECOVERED_FROM
                    : CausalLink.Kind.PRECEDED;
            String statement = current.kind() == MatchPhase.Kind.STABILIZATION
                    ? "Стабилизация следует за ранним спадом и может быть восстановлением после него, но replay не доказывает намерение."
                    : "Предыдущая фаза предшествует следующей и задаёт контекст, но не доказывает причинность.";
            links.add(new CausalLink(
                    "causal-link-" + (i - 1),
                    kind,
                    previous.id(),
                    current.id(),
                    statement,
                    config.causalLinkConfidence(),
                    List.of(previous.id(), current.id()),
                    List.of("No intent or hidden-information evidence is available in the replay.")
            ));
        }
        return List.copyOf(links);
    }

    private NarrativeChartModel chart(List<MatchStateSnapshot> snapshots, List<MatchPhase> phases, List<NarrativeEvent> events) {
        if (snapshots.isEmpty()) return NarrativeChartModel.empty();
        List<Point> army = snapshots.stream().map(snapshot -> new Point(snapshot.at(), snapshot.metrics().armyValue())).toList();
        List<Point> economy = snapshots.stream().map(snapshot -> new Point(snapshot.at(), snapshot.metrics().economyProxy())).toList();
        List<Point> supply = snapshots.stream().map(snapshot -> new Point(snapshot.at(), snapshot.metrics().supplyUsed())).toList();
        List<Marker> markers = events.stream()
                .filter(event -> event.kind() == NarrativeEvent.Kind.COMBAT || event.kind() == NarrativeEvent.Kind.TURNING_POINT)
                .map(event -> new Marker("marker-" + event.id(), event.title(), markerKind(event.kind()), event.at(), event.id()))
                .toList();
        List<Interval> intervals = phases.stream()
                .map(phase -> new Interval("interval-" + phase.id(), phase.id(), phase.title(), phase.startedAt(), phase.endedAt()))
                .toList();
        return new NarrativeChartModel(
                snapshots.getFirst().at(),
                snapshots.getLast().at(),
                List.of(
                        new Series("armyValue", "Стоимость армии", "resources", "MatchContext.army.absoluteValue", Completeness.COMPLETE, army),
                        new Series("economyProxy", "Экономика", "proxy", "MatchContext.economy.absoluteValue", Completeness.PARTIAL, economy),
                        new Series("supplyUsed", "Занятый лимит", "supply", "MatchContext.supply.absoluteValue", Completeness.PARTIAL, supply)
                ),
                markers,
                intervals,
                List.of("Economy and supply are context proxies; the replay response does not expose full base, queue or bank-spend series.")
        );
    }

    private NarrativeChartModel.Kind markerKind(NarrativeEvent.Kind kind) {
        return kind == NarrativeEvent.Kind.COMBAT ? NarrativeChartModel.Kind.COMBAT : NarrativeChartModel.Kind.TURNING_POINT;
    }

    private NarrativeSummary summary(Match match, PlayerState focus, List<String> team, List<MatchPhase> phases,
                                     List<StateTransition> transitions, List<CausalLink> links) {
        String teamText = team.stream().filter(name -> !name.equals(focus.name())).findFirst()
                .map(name -> ", союзник: " + name)
                .orElse("");
        String verdict = "Официальный результат реплея для " + focus.name() + ": " + officialResult(focus)
                + teamText + ". Narrative Analysis показывает фазы матча и контекст переходов; strategic result не вычисляется.";
        List<String> points = new ArrayList<>();
        phases.stream().limit(4).forEach(phase -> points.add(phase.title() + " " + clock(phase.startedAt()) + "–" + clock(phase.endedAt())));
        points.add("Strategic result: NOT_EVALUATED");
        return new NarrativeSummary(
                verdict,
                "NOT_EVALUATED",
                points,
                List.of("Causal chain uses cautious links such as preceded/recovered-from; it does not assert mandatory winners or trade efficiency.")
        );
    }

    private String officialResult(PlayerState focus) {
        return focus.result() == null || focus.result().isBlank() ? "Unknown" : focus.result();
    }

    private double severityConfidence(TurningPoint.Severity severity) {
        if (severity == null) return 0.62;
        return switch (severity) {
            case CRITICAL -> 0.82;
            case MAJOR -> 0.74;
            case NOTABLE -> 0.66;
        };
    }

    private Duration lastAt(List<MatchStateSnapshot> snapshots) {
        return snapshots.isEmpty() ? Duration.ZERO : snapshots.getLast().at();
    }

    private String snapshotId(int index, Duration at) {
        return "snapshot-" + index + "-" + at.toMillis();
    }

    private double value(MatchStateSnapshot.Metrics metrics, Metric metric) {
        return switch (metric) {
            case ARMY -> metrics.armyValue();
            case ECONOMY -> metrics.economyProxy();
            case SUPPLY -> metrics.supplyUsed();
            case OVERALL -> metrics.overallScore();
        };
    }

    private double rounded(MatchStateSnapshot.Metrics metrics, Metric metric) {
        return Math.round(value(metrics, metric) * 10.0) / 10.0;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String clock(Duration value) {
        long seconds = Math.round(value.toMillis() / 1000.0);
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    private enum Metric {
        ARMY("armyValue"),
        ECONOMY("economyProxy"),
        SUPPLY("supplyUsed"),
        OVERALL("overallScore");

        private final String id;

        Metric(String id) {
            this.id = id;
        }
    }

    private record Candidate(MatchStateSnapshot before, MatchStateSnapshot after, Metric metric, double delta) {}
}
