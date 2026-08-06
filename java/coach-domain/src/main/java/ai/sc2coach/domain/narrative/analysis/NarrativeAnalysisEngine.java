package ai.sc2coach.domain.narrative.analysis;

import ai.sc2coach.domain.combat.Combat;
import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.decision.Decision;
import ai.sc2coach.domain.knowledge.Recommendation;
import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.CombatDrilldown;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.DevelopmentDrilldown;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.DevelopmentMetric;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.IntervalDelta;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.IntervalDrilldown;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.IntervalMetrics;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.Kind;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.MacroEvidence;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.MatchFlowInterval;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.MetricDelta;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.PreparationEvidence;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.ProductionEvidence;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.ScoutingEvidence;
import ai.sc2coach.domain.narrative.analysis.MatchFlow.TechEvidence;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Completeness;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Interval;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Marker;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Point;
import ai.sc2coach.domain.narrative.analysis.NarrativeChartModel.Series;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.CombatEvidence;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.CombatParticipantEvidence;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.CombatSideEvidence;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.CountEvidence;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.EvidenceFocus;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.FocusKind;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.MetricComparison;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.ParticipantIdentity;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.ParticipantMetricSeries;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.Relationship;
import ai.sc2coach.domain.narrative.analysis.NarrativeEvidence.UnitEvidenceRow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

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
        NarrativeEvidence evidence = evidence(match, focus, snapshots, phases, events, input.combats());
        MatchFlow matchFlow = matchFlow(match, snapshots, phases, transitions, events, input.combats(), evidence);
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
                evidence,
                matchFlow,
                List.of(
                        "Narrative Analysis использует существующие детерминированные движки и не выводит стратегический результат.",
                        "Данные реплея не доказывают намерение или причинность; причинные связи используют осторожную формулировку порядка и вклада.",
                        "График использует стоимость армии, экономический прокси и лимит из match context; полные базы, очереди производства и расход банка в V1 недоступны."
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
                    List.of("Экономика здесь — прокси по рабочим и доходу из существующего MatchContext, а не полная модель банка и трат.")
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
                List.of("Переход вычислен по context snapshots; пороги и подписи остаются настраиваемой эвристикой.")
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
                    List.of("Подпись открытия выведена по границам timeline из match context, а не полным классификатором build order.")
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
                    ? "Стабилизация следует за ранним спадом и может быть восстановлением после него, но реплей не доказывает намерение."
                    : "Предыдущая фаза предшествует следующей и задаёт контекст, но не доказывает причинность.";
            links.add(new CausalLink(
                    "causal-link-" + (i - 1),
                    kind,
                    previous.id(),
                    current.id(),
                    statement,
                    config.causalLinkConfidence(),
                    List.of(previous.id(), current.id()),
                    List.of("В реплее нет evidence для намерения или скрытой информации.")
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
                List.of("Экономика и лимит — context proxies; replay response не содержит полные ряды баз, очередей производства или трат банка.")
        );
    }

    private MatchFlow matchFlow(Match match, List<MatchStateSnapshot> snapshots, List<MatchPhase> phases,
                                List<StateTransition> transitions, List<NarrativeEvent> events,
                                List<Combat> combats, NarrativeEvidence evidence) {
        if (snapshots.isEmpty()) return MatchFlow.empty();
        Duration start = Duration.ZERO;
        Duration end = matchEnd(match, snapshots, combats);
        if (end.compareTo(start) <= 0) return MatchFlow.empty();

        TreeSet<Duration> boundaries = coarseEpisodeBoundaries(snapshots, evidence.participants(), start, end);

        Map<String, CombatEvidence> combatEvidenceById = evidence.combats().stream()
                .collect(Collectors.toMap(CombatEvidence::id, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<ParticipantIdentity> participants = evidence.participants();
        List<Duration> ordered = new ArrayList<>(boundaries);
        List<MatchFlowInterval> intervals = new ArrayList<>();
        for (int i = 0; i < ordered.size() - 1; i++) {
            Duration from = ordered.get(i);
            Duration to = ordered.get(i + 1);
            if (to.compareTo(from) <= 0) continue;
            List<Combat> overlappingCombats = overlappingCombats(combats, from, to);
            Map<String, IntervalMetrics> startMetrics = intervalMetrics(participants, snapshots, from);
            Map<String, IntervalMetrics> endMetrics = intervalMetrics(participants, snapshots, to);
            IntervalDelta delta = intervalDelta(startMetrics, endMetrics);
            List<String> snapshotIds = snapshotIds(snapshots, from, to, startMetrics, endMetrics);
            List<String> transitionIds = transitionIds(transitions, from, to);
            List<String> intervalEventIds = eventIdsHalfOpen(events, from, to);
            List<String> combatIds = overlappingCombats.stream().map(Combat::id).toList();
            List<String> evidenceIds = evidenceIds(snapshotIds, transitionIds, intervalEventIds, combatIds);
            Kind kind = classifyInterval(intervals, from, overlappingCombats, delta);
            IntervalDrilldown drilldown = intervalDrilldown(from, to, overlappingCombats, combatEvidenceById,
                    startMetrics, endMetrics, delta, snapshotIds);
            Completeness completeness = intervalCompleteness(startMetrics, endMetrics, drilldown);
            String id = "match-flow-" + String.format("%03d", intervals.size());
            intervals.add(new MatchFlowInterval(
                    id,
                    intervals.size(),
                    kind,
                    intervalTitle(kind),
                    from,
                    to,
                    intervalConfidence(kind, completeness, drilldown),
                    completeness,
                    intervalSummary(kind, delta, drilldown),
                    snapshotIds,
                    transitionIds,
                    intervalEventIds,
                    evidenceIds,
                    combatIds,
                    startMetrics,
                    endMetrics,
                    delta,
                    drilldown,
                    intervalLimitations(drilldown)
            ));
        }
        return new MatchFlow(
                "match-flow.v1",
                start,
                end,
                intervals,
                combats.stream().map(Combat::id).toList(),
                List.of(
                        "Ход матча построен как человекочитаемые крупные эпизоды по сглаженным многомерным рядам MatchContext; короткие бои и микрособытия становятся evidence внутри эпизода, а не отдельными карточками.",
                        "Расшифровка развития использует доступные изменения макро-показателей и доступность юнитов в окнах боёв; полные очереди производства, точные времена исследований и полное видение не восстановлены."
                )
        );
    }

    private TreeSet<Duration> coarseEpisodeBoundaries(List<MatchStateSnapshot> snapshots,
                                                       List<ParticipantIdentity> participants,
                                                       Duration start,
                                                       Duration end) {
        TreeSet<Duration> boundaries = new TreeSet<>();
        boundaries.add(start);
        boundaries.add(end);
        if (snapshots.size() < 5 || end.minus(start).compareTo(Duration.ofSeconds(240)) < 0) {
            return boundaries;
        }

        List<FeatureSample> samples = featureSamples(snapshots, participants);
        if (samples.size() < 5) return boundaries;

        int targetEpisodes = targetEpisodeCount(end.minus(start));
        double minEpisodeSeconds = end.minus(start).compareTo(Duration.ofMinutes(15)) >= 0 ? 60 : 45;
        targetEpisodes = Math.min(targetEpisodes, Math.max(1, (int) Math.floor(secondsBetween(start, end) / minEpisodeSeconds)));
        if (targetEpisodes <= 1) return boundaries;

        List<Integer> breakpoints = optimalEpisodeBreakpoints(samples, targetEpisodes, minEpisodeSeconds);
        for (Integer index : breakpoints) {
            if (index == null || index <= 0 || index >= samples.size() - 1) continue;
            addMatchFlowBoundary(boundaries, samples.get(index).at(), start, end);
        }
        return boundaries;
    }

    private int targetEpisodeCount(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds < 600) return 4;
        if (seconds < 1200) return 5;
        return 6;
    }

    private List<FeatureSample> featureSamples(List<MatchStateSnapshot> snapshots, List<ParticipantIdentity> participants) {
        List<String> names = participants.stream().map(ParticipantIdentity::displayName).toList();
        List<double[]> raw = new ArrayList<>();
        for (MatchStateSnapshot snapshot : snapshots) {
            double[] vector = new double[Math.max(1, names.size()) * 3];
            int offset = 0;
            for (String name : names) {
                MatchStateSnapshot.Metrics metrics = snapshot.playerMetrics().get(name);
                vector[offset++] = metrics == null ? 0 : metrics.armyValue();
                vector[offset++] = metrics == null ? 0 : metrics.economyProxy();
                vector[offset++] = metrics == null ? 0 : metrics.supplyUsed();
            }
            raw.add(vector);
        }

        double[][] smoothed = gaussianSmooth(raw, snapshots, 25.0);
        double[][] normalized = normalize(smoothed);
        List<FeatureSample> samples = new ArrayList<>();
        double[] integral = new double[normalized[0].length];
        for (int i = 0; i < normalized.length; i++) {
            double elapsed = i == 0 ? 0 : Math.max(1, secondsBetween(snapshots.get(i - 1).at(), snapshots.get(i).at()));
            double[] derivative = new double[normalized[i].length];
            if (i > 0) {
                for (int d = 0; d < derivative.length; d++) {
                    derivative[d] = (normalized[i][d] - normalized[i - 1][d]) / elapsed * 30.0;
                    integral[d] += normalized[i][d] * elapsed / 60.0;
                }
            }
            double[] vector = new double[normalized[i].length * 3];
            for (int d = 0; d < normalized[i].length; d++) {
                vector[d] = normalized[i][d];
                vector[d + normalized[i].length] = derivative[d];
                vector[d + normalized[i].length * 2] = integral[d];
            }
            samples.add(new FeatureSample(snapshots.get(i).at(), vector));
        }
        return samples;
    }

    private double[][] gaussianSmooth(List<double[]> raw, List<MatchStateSnapshot> snapshots, double sigmaSeconds) {
        int n = raw.size();
        int dimensions = raw.getFirst().length;
        double[][] result = new double[n][dimensions];
        double radius = sigmaSeconds * 2.5;
        for (int i = 0; i < n; i++) {
            double weightSum = 0;
            for (int j = 0; j < n; j++) {
                double distance = Math.abs(secondsBetween(snapshots.get(i).at(), snapshots.get(j).at()));
                if (distance > radius) continue;
                double weight = Math.exp(-(distance * distance) / (2 * sigmaSeconds * sigmaSeconds));
                weightSum += weight;
                for (int d = 0; d < dimensions; d++) result[i][d] += raw.get(j)[d] * weight;
            }
            if (weightSum > 0) {
                for (int d = 0; d < dimensions; d++) result[i][d] /= weightSum;
            }
        }
        return result;
    }

    private double[][] normalize(double[][] values) {
        int n = values.length;
        int dimensions = values[0].length;
        double[] mean = new double[dimensions];
        double[] variance = new double[dimensions];
        for (double[] value : values) {
            for (int d = 0; d < dimensions; d++) mean[d] += value[d];
        }
        for (int d = 0; d < dimensions; d++) mean[d] /= n;
        for (double[] value : values) {
            for (int d = 0; d < dimensions; d++) {
                double centered = value[d] - mean[d];
                variance[d] += centered * centered;
            }
        }
        double[][] normalized = new double[n][dimensions];
        for (int i = 0; i < n; i++) {
            for (int d = 0; d < dimensions; d++) {
                double std = Math.sqrt(variance[d] / Math.max(1, n - 1));
                normalized[i][d] = std <= 0.000001 ? 0 : (values[i][d] - mean[d]) / std;
            }
        }
        return normalized;
    }

    private List<Integer> optimalEpisodeBreakpoints(List<FeatureSample> samples, int targetEpisodes, double minEpisodeSeconds) {
        int n = samples.size();
        double[][] cost = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                cost[i][j] = segmentLinearCost(samples, i, j);
            }
        }

        double[][] dp = new double[targetEpisodes + 1][n];
        int[][] previous = new int[targetEpisodes + 1][n];
        for (int k = 0; k <= targetEpisodes; k++) {
            for (int j = 0; j < n; j++) {
                dp[k][j] = Double.POSITIVE_INFINITY;
                previous[k][j] = -1;
            }
        }
        for (int j = 1; j < n; j++) {
            if (episodeDuration(samples, 0, j) >= minEpisodeSeconds) dp[1][j] = cost[0][j];
        }
        for (int k = 2; k <= targetEpisodes; k++) {
            for (int j = 1; j < n; j++) {
                if (episodeDuration(samples, 0, j) < minEpisodeSeconds * k) continue;
                for (int i = 1; i < j; i++) {
                    if (episodeDuration(samples, i, j) < minEpisodeSeconds || !Double.isFinite(dp[k - 1][i])) continue;
                    double candidate = dp[k - 1][i] + cost[i][j] + k * 0.15;
                    if (candidate < dp[k][j]) {
                        dp[k][j] = candidate;
                        previous[k][j] = i;
                    }
                }
            }
        }

        int selectedK = targetEpisodes;
        while (selectedK > 1 && !Double.isFinite(dp[selectedK][n - 1])) selectedK--;
        if (selectedK <= 1) return List.of();
        List<Integer> breakpoints = new ArrayList<>();
        int cursor = n - 1;
        for (int k = selectedK; k > 1; k--) {
            int prev = previous[k][cursor];
            if (prev <= 0) break;
            breakpoints.add(prev);
            cursor = prev;
        }
        Collections.reverse(breakpoints);
        return breakpoints;
    }

    private double segmentLinearCost(List<FeatureSample> samples, int from, int to) {
        int count = to - from + 1;
        if (count <= 2) return 0;
        int dimensions = samples.getFirst().values().length;
        double firstSecond = samples.get(from).at().toMillis() / 1000.0;
        double sumT = 0;
        double sumTT = 0;
        for (int i = from; i <= to; i++) {
            double t = samples.get(i).at().toMillis() / 1000.0 - firstSecond;
            sumT += t;
            sumTT += t * t;
        }
        double cost = 0;
        for (int d = 0; d < dimensions; d++) {
            double sumY = 0;
            double sumTY = 0;
            for (int i = from; i <= to; i++) {
                double t = samples.get(i).at().toMillis() / 1000.0 - firstSecond;
                double y = samples.get(i).values()[d];
                sumY += y;
                sumTY += t * y;
            }
            double denominator = count * sumTT - sumT * sumT;
            double slope = Math.abs(denominator) <= 0.000001 ? 0 : (count * sumTY - sumT * sumY) / denominator;
            double intercept = (sumY - slope * sumT) / count;
            for (int i = from; i <= to; i++) {
                double t = samples.get(i).at().toMillis() / 1000.0 - firstSecond;
                double error = samples.get(i).values()[d] - (intercept + slope * t);
                cost += error * error;
            }
        }
        return cost / Math.max(1, count);
    }

    private double episodeDuration(List<FeatureSample> samples, int from, int to) {
        return secondsBetween(samples.get(from).at(), samples.get(to).at());
    }

    private double secondsBetween(Duration from, Duration to) {
        return (to.toMillis() - from.toMillis()) / 1000.0;
    }

    private Duration matchEnd(Match match, List<MatchStateSnapshot> snapshots, List<Combat> combats) {
        Duration end = match == null ? Duration.ZERO : match.duration();
        end = max(end, lastAt(snapshots));
        for (Combat combat : combats) {
            end = max(end, combat.endedAt());
        }
        return end;
    }

    private void addMatchFlowBoundary(TreeSet<Duration> boundaries, Duration value, Duration start, Duration end) {
        Duration boundary = clamp(value, start, end);
        if (boundary.minus(start).compareTo(Duration.ofSeconds(1)) <= 0) {
            boundary = start;
        } else if (end.minus(boundary).compareTo(Duration.ofSeconds(1)) <= 0) {
            boundary = end;
        }
        boundaries.add(boundary);
    }

    private Duration max(Duration left, Duration right) {
        if (left == null) return right == null ? Duration.ZERO : right;
        if (right == null) return left;
        return left.compareTo(right) >= 0 ? left : right;
    }

    private Duration clamp(Duration value, Duration start, Duration end) {
        if (value == null) return start;
        if (value.compareTo(start) < 0) return start;
        if (value.compareTo(end) > 0) return end;
        return value;
    }

    private List<Combat> overlappingCombats(List<Combat> combats, Duration from, Duration to) {
        return combats.stream()
                .filter(combat -> overlaps(combat.startedAt(), combat.endedAt(), from, to))
                .sorted(Comparator.comparing(Combat::startedAt).thenComparing(Combat::id))
                .toList();
    }

    private boolean overlaps(Duration leftStart, Duration leftEnd, Duration rightStart, Duration rightEnd) {
        Duration normalizedLeftStart = leftStart == null ? Duration.ZERO : leftStart;
        Duration normalizedLeftEnd = leftEnd == null ? normalizedLeftStart : leftEnd;
        return normalizedLeftStart.compareTo(rightEnd) < 0 && normalizedLeftEnd.compareTo(rightStart) > 0;
    }

    private Map<String, IntervalMetrics> intervalMetrics(List<ParticipantIdentity> participants,
                                                         List<MatchStateSnapshot> snapshots,
                                                         Duration at) {
        MatchStateSnapshot snapshot = snapshotAtOrBefore(snapshots, at).orElseGet(snapshots::getFirst);
        Map<String, IntervalMetrics> result = new LinkedHashMap<>();
        for (ParticipantIdentity participant : participants) {
            MatchStateSnapshot.Metrics metrics = snapshot.playerMetrics().get(participant.displayName());
            if (metrics == null) {
                result.put(participant.id(), IntervalMetrics.unavailable());
                continue;
            }
            result.put(participant.id(), new IntervalMetrics(
                    metrics.armyValue(),
                    metrics.economyProxy(),
                    metrics.supplyUsed(),
                    Completeness.COMPLETE,
                    List.of(snapshot.id())
            ));
        }
        return result;
    }

    private Optional<MatchStateSnapshot> snapshotAtOrBefore(List<MatchStateSnapshot> snapshots, Duration at) {
        return snapshots.stream()
                .filter(snapshot -> snapshot.at().compareTo(at) <= 0)
                .max(Comparator.comparing(MatchStateSnapshot::at));
    }

    private IntervalDelta intervalDelta(Map<String, IntervalMetrics> startMetrics,
                                        Map<String, IntervalMetrics> endMetrics) {
        Map<String, MetricDelta> deltas = new LinkedHashMap<>();
        boolean partial = false;
        for (Map.Entry<String, IntervalMetrics> entry : startMetrics.entrySet()) {
            IntervalMetrics start = entry.getValue();
            IntervalMetrics end = endMetrics.get(entry.getKey());
            if (end == null || start.completeness() == Completeness.UNAVAILABLE || end.completeness() == Completeness.UNAVAILABLE) {
                partial = true;
                continue;
            }
            deltas.put(entry.getKey(), new MetricDelta(
                    end.armyValue() - start.armyValue(),
                    end.economyProxy() - start.economyProxy(),
                    end.supplyUsed() - start.supplyUsed()
            ));
        }
        return new IntervalDelta(deltas, partial ? Completeness.PARTIAL : Completeness.COMPLETE,
                partial ? List.of("Часть метрик участников недоступна для этого интервала.") : List.of());
    }

    private List<String> snapshotIds(List<MatchStateSnapshot> snapshots, Duration from, Duration to,
                                     Map<String, IntervalMetrics> startMetrics,
                                     Map<String, IntervalMetrics> endMetrics) {
        TreeSet<String> ids = new TreeSet<>();
        snapshots.stream()
                .filter(snapshot -> snapshot.at().compareTo(from) >= 0 && snapshot.at().compareTo(to) <= 0)
                .map(MatchStateSnapshot::id)
                .forEach(ids::add);
        startMetrics.values().stream().flatMap(metrics -> metrics.sourceSnapshotIds().stream()).forEach(ids::add);
        endMetrics.values().stream().flatMap(metrics -> metrics.sourceSnapshotIds().stream()).forEach(ids::add);
        return List.copyOf(ids);
    }

    private List<String> transitionIds(List<StateTransition> transitions, Duration from, Duration to) {
        return transitions.stream()
                .filter(transition -> overlaps(transition.from(), transition.to(), from, to))
                .map(StateTransition::id)
                .toList();
    }

    private List<String> eventIdsHalfOpen(List<NarrativeEvent> events, Duration from, Duration to) {
        return events.stream()
                .filter(event -> eventOverlaps(event, from, to))
                .map(NarrativeEvent::id)
                .toList();
    }

    private boolean eventOverlaps(NarrativeEvent event, Duration from, Duration to) {
        Duration at = event.at() == null ? Duration.ZERO : event.at();
        Duration endedAt = event.endedAt() == null ? at : event.endedAt();
        if (endedAt.compareTo(at) <= 0) {
            return at.compareTo(from) >= 0 && at.compareTo(to) < 0;
        }
        return overlaps(at, endedAt, from, to);
    }

    private List<String> evidenceIds(List<String> snapshotIds, List<String> transitionIds,
                                     List<String> eventIds, List<String> combatIds) {
        TreeSet<String> ids = new TreeSet<>();
        ids.addAll(snapshotIds);
        ids.addAll(transitionIds);
        ids.addAll(eventIds);
        ids.addAll(combatIds);
        return List.copyOf(ids);
    }

    private Kind classifyInterval(List<MatchFlowInterval> previousIntervals, Duration from,
                                  List<Combat> overlappingCombats, IntervalDelta delta) {
        if (!overlappingCombats.isEmpty()) return Kind.COMBAT;
        if (from.equals(Duration.ZERO)) return Kind.OPENING_BUILDUP;
        double army = focusDelta(delta, Metric.ARMY);
        double economy = focusDelta(delta, Metric.ECONOMY);
        double supply = focusDelta(delta, Metric.SUPPLY);
        if (army > 1 && previousIntervals.stream().anyMatch(interval -> interval.kind() == Kind.COMBAT)) {
            return Kind.RECOVERY;
        }
        if (economy > 1 || supply > 1) return Kind.ECONOMIC_GROWTH;
        if (army > 1) return Kind.ARMY_BUILDUP;
        if (Math.abs(army) > 1 || Math.abs(economy) > 1 || Math.abs(supply) > 1) {
            return Kind.REGROUPING_OR_LOW_ACTIVITY;
        }
        return Kind.LOW_EVIDENCE;
    }

    private double focusDelta(IntervalDelta delta, Metric metric) {
        return delta.byParticipantId().values().stream()
                .findFirst()
                .map(value -> switch (metric) {
                    case ARMY -> value.armyValueDelta();
                    case ECONOMY -> value.economyProxyDelta();
                    case SUPPLY -> value.supplyUsedDelta();
                    case OVERALL -> 0.0;
                })
                .orElse(0.0);
    }

    private IntervalDrilldown intervalDrilldown(Duration from, Duration to, List<Combat> overlappingCombats,
                                                Map<String, CombatEvidence> combatEvidenceById,
                                                Map<String, IntervalMetrics> startMetrics,
                                                Map<String, IntervalMetrics> endMetrics,
                                                IntervalDelta delta, List<String> snapshotIds) {
        List<String> combatIds = overlappingCombats.stream().map(Combat::id).toList();
        List<CombatEvidence> combatEvidence = combatIds.stream()
                .map(combatEvidenceById::get)
                .filter(Objects::nonNull)
                .toList();
        CombatDrilldown combat = new CombatDrilldown(
                combatIds,
                combatEvidence,
                combatIds.isEmpty() ? List.of("Боёв в этом интервале не обнаружено.") : List.of(),
                combatEvidence.size() == combatIds.size()
                        ? List.of()
                        : List.of("У части пересекающихся боёв нет подробных строк NarrativeEvidence."),
                combatNarrative(overlappingCombats)
        );
        DevelopmentDrilldown development = developmentDrilldown(from, to, overlappingCombats, startMetrics, endMetrics, delta, snapshotIds);
        List<String> limitations = new ArrayList<>();
        limitations.addAll(combat.limitations());
        limitations.addAll(development.limitations());
        return new IntervalDrilldown(combat, development, limitations);
    }

    private DevelopmentDrilldown developmentDrilldown(Duration from, Duration to, List<Combat> overlappingCombats,
                                                      Map<String, IntervalMetrics> startMetrics,
                                                      Map<String, IntervalMetrics> endMetrics,
                                                      IntervalDelta delta, List<String> snapshotIds) {
        MacroEvidence macro = macroEvidence(startMetrics, endMetrics, delta, snapshotIds);
        ProductionEvidence production = productionEvidence(overlappingCombats);
        TechEvidence tech = techEvidence(overlappingCombats);
        PreparationEvidence preparation = preparationEvidence(overlappingCombats, delta);
        ScoutingEvidence scouting = ScoutingEvidence.empty();
        boolean hasDevelopment = !macro.metrics().isEmpty()
                || !production.observations().isEmpty()
                || !tech.observations().isEmpty()
                || !preparation.observations().isEmpty();
        List<String> emptyStates = hasDevelopment
                ? List.of()
                : List.of("Экономических, производственных, технологических или разведывательных событий в этом интервале не обнаружено.");
        List<String> limitations = new ArrayList<>();
        limitations.add("Полные очереди производства, точные времена исследований и полное видение недоступны в текущем replay response.");
        limitations.addAll(macro.limitations());
        limitations.addAll(production.limitations());
        limitations.addAll(tech.limitations());
        limitations.addAll(preparation.limitations());
        return new DevelopmentDrilldown(macro, production, tech, scouting, preparation, emptyStates, limitations);
    }

    private MacroEvidence macroEvidence(Map<String, IntervalMetrics> startMetrics,
                                        Map<String, IntervalMetrics> endMetrics,
                                        IntervalDelta delta, List<String> snapshotIds) {
        Map<String, MetricDelta> byParticipant = delta.byParticipantId();
        if (byParticipant.isEmpty()) return MacroEvidence.empty();
        String focusParticipantId = byParticipant.keySet().iterator().next();
        MetricDelta focus = byParticipant.get(focusParticipantId);
        IntervalMetrics start = startMetrics.getOrDefault(focusParticipantId, IntervalMetrics.unavailable());
        IntervalMetrics end = endMetrics.getOrDefault(focusParticipantId, IntervalMetrics.unavailable());
        List<DevelopmentMetric> metrics = new ArrayList<>();
        addMetric(metrics, "armyValue", start.armyValue(), end.armyValue(), focus.armyValueDelta());
        addMetric(metrics, "economyProxy", start.economyProxy(), end.economyProxy(), focus.economyProxyDelta());
        addMetric(metrics, "supplyUsed", start.supplyUsed(), end.supplyUsed(), focus.supplyUsedDelta());
        if (metrics.isEmpty()) return MacroEvidence.empty();
        return new MacroEvidence(
                "Макро-контекст изменился в этом интервале.",
                metrics,
                delta.completeness(),
                snapshotIds,
                delta.limitations()
        );
    }

    private void addMetric(List<DevelopmentMetric> metrics, String metric, double start, double end, double delta) {
        if (Math.abs(delta) <= 1) return;
        metrics.add(new DevelopmentMetric(metric, start, end, delta, Completeness.COMPLETE));
    }

    private ProductionEvidence productionEvidence(List<Combat> overlappingCombats) {
        List<String> observations = new ArrayList<>();
        for (Combat combat : overlappingCombats) {
            for (Combat.Participant participant : combat.participants()) {
                if (participant.additions().isEmpty()) continue;
                observations.add(participant.player() + ": новые боевые юниты стали доступны в интервале: "
                        + composition(participant.additions()) + ".");
            }
        }
        return new ProductionEvidence(observations, observations.isEmpty()
                ? List.of()
                : List.of("Боевые пополнения сохраняют смысл ADR-012: юниты стали доступны в интервале; локальное участие в конкретной точке боя не утверждается."));
    }

    private TechEvidence techEvidence(List<Combat> overlappingCombats) {
        List<String> observations = new ArrayList<>();
        for (Combat combat : overlappingCombats) {
            for (Combat.Participant participant : combat.participants()) {
                if (!participant.upgrades().isEmpty()) {
                    observations.add(participant.player() + ": апгрейды, видимые в снимке боя: "
                            + String.join(", ", participant.upgrades()) + ".");
                }
                if (!participant.technologies().isEmpty()) {
                    observations.add(participant.player() + ": технологии, видимые в снимке боя: "
                            + String.join(", ", participant.technologies()) + ".");
                }
            }
        }
        return new TechEvidence(observations, observations.isEmpty()
                ? List.of()
                : List.of("Видимые апгрейды/технологии описывают состояние снимка; они не доказывают точный момент исследования внутри интервала."));
    }

    private PreparationEvidence preparationEvidence(List<Combat> overlappingCombats, IntervalDelta delta) {
        if (!overlappingCombats.isEmpty()) return PreparationEvidence.empty();
        double army = focusDelta(delta, Metric.ARMY);
        if (army <= 1) return PreparationEvidence.empty();
        return new PreparationEvidence(
                List.of("Стоимость армии выросла без обнаруженного боя; это похоже на набор армии или подготовку."),
                List.of("Подготовка выведена из видимых изменений метрик, а не из скрытого намерения.")
        );
    }

    private String combatNarrative(List<Combat> combats) {
        if (combats.isEmpty()) return "";
        return combats.stream()
                .map(this::combatNarrative)
                .collect(Collectors.joining(" "));
    }

    private String combatNarrative(Combat combat) {
        String attacker = combat.initiator() == null || combat.initiator().isBlank() ? "Инициатор" : combat.initiator();
        String defender = combat.opponent() == null || combat.opponent().isBlank() ? "соперника" : combat.opponent();
        List<String> parts = new ArrayList<>();
        parts.add(attacker + " атакует " + defender + ".");
        String forces = combat.participants().stream()
                .map(participant -> participant.player() + ": " + compactComposition(participant.armyBefore()))
                .filter(item -> !item.endsWith(": нет"))
                .collect(Collectors.joining("; "));
        if (!forces.isBlank()) parts.add("Состав в начале: " + forces + ".");
        String additions = combat.participants().stream()
                .filter(participant -> !participant.additions().isEmpty())
                .map(participant -> participant.player() + " достроил/получил " + compactComposition(participant.additions()))
                .collect(Collectors.joining("; "));
        if (!additions.isBlank()) parts.add("Во время эпизода: " + additions + ".");
        String losses = combat.participants().stream()
                .filter(participant -> hasValues(participant.unitsLost()) || hasValues(participant.workersLost())
                        || hasValues(participant.structuresLost()) || hasValues(participant.staticDefenseLost()))
                .map(participant -> participant.player() + " потерял " + combatLossSummary(participant))
                .collect(Collectors.joining("; "));
        if (!losses.isBlank()) parts.add("Потери: " + losses + ".");
        return String.join(" ", parts);
    }

    private String combatLossSummary(Combat.Participant participant) {
        List<String> losses = new ArrayList<>();
        if (hasValues(participant.unitsLost())) losses.add(compactComposition(participant.unitsLost()));
        if (hasValues(participant.workersLost())) losses.add("рабочие: " + compactComposition(participant.workersLost()));
        if (hasValues(participant.structuresLost())) losses.add("здания: " + compactComposition(participant.structuresLost()));
        if (hasValues(participant.staticDefenseLost())) losses.add("статичная оборона: " + compactComposition(participant.staticDefenseLost()));
        return losses.isEmpty() ? "нет подтверждённых потерь" : String.join(", ", losses);
    }

    private String composition(Map<String, Integer> value) {
        return value.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "+" + entry.getValue() + " " + entry.getKey())
                .collect(Collectors.joining(", "));
    }

    private String compactComposition(Map<String, Integer> value) {
        if (!hasValues(value)) return "нет";
        return value.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getValue() + " x " + entry.getKey())
                .collect(Collectors.joining(", "));
    }

    private boolean hasValues(Map<String, Integer> value) {
        return value != null && value.values().stream().anyMatch(count -> count != null && count > 0);
    }

    private Completeness intervalCompleteness(Map<String, IntervalMetrics> startMetrics,
                                              Map<String, IntervalMetrics> endMetrics,
                                              IntervalDrilldown drilldown) {
        boolean partial = startMetrics.values().stream().anyMatch(metrics -> metrics.completeness() != Completeness.COMPLETE)
                || endMetrics.values().stream().anyMatch(metrics -> metrics.completeness() != Completeness.COMPLETE)
                || !drilldown.limitations().isEmpty();
        return partial ? Completeness.PARTIAL : Completeness.COMPLETE;
    }

    private double intervalConfidence(Kind kind, Completeness completeness, IntervalDrilldown drilldown) {
        if (kind == Kind.LOW_EVIDENCE) return 0.38;
        double base = kind == Kind.COMBAT ? 0.78 : 0.62;
        if (completeness != Completeness.COMPLETE) base -= 0.08;
        if (!drilldown.development().emptyStates().isEmpty() && !drilldown.combat().emptyStates().isEmpty()) base -= 0.08;
        return Math.max(0.25, Math.min(0.9, base));
    }

    private String intervalTitle(Kind kind) {
        return switch (kind) {
            case OPENING_BUILDUP -> "Открытие и развитие";
            case ECONOMIC_GROWTH -> "Экономическое развитие";
            case TECH_TRANSITION -> "Технологический переход";
            case ARMY_BUILDUP -> "Наращивание армии";
            case MAP_CONTROL_OR_SCOUTING -> "Контроль карты / разведка";
            case PRESSURE_PREPARATION -> "Подготовка давления";
            case COMBAT -> "Боевой интервал";
            case RECOVERY -> "Восстановление";
            case REGROUPING_OR_LOW_ACTIVITY -> "Перегруппировка / низкая активность";
            case LOW_EVIDENCE -> "Низкая доказательность";
        };
    }

    private String intervalSummary(Kind kind, IntervalDelta delta, IntervalDrilldown drilldown) {
        String combatText = drilldown.combat().combatIds().isEmpty()
                ? "боёв нет"
                : "боёв: " + drilldown.combat().combatIds().size();
        String developmentText = drilldown.development().emptyStates().isEmpty()
                ? "данные по развитию есть"
                : "данных по развитию нет";
        if (kind == Kind.LOW_EVIDENCE) {
            return "Интервал сохранён без уверенного стратегического ярлыка: " + combatText + ", " + developmentText + ".";
        }
        return intervalTitle(kind) + ": " + combatText + ", " + developmentText + ".";
    }

    private List<String> intervalLimitations(IntervalDrilldown drilldown) {
        TreeSet<String> limitations = new TreeSet<>(drilldown.limitations());
        return List.copyOf(limitations);
    }

    private NarrativeEvidence evidence(Match match, PlayerState focus, List<MatchStateSnapshot> snapshots,
                                       List<MatchPhase> phases, List<NarrativeEvent> events, List<Combat> combats) {
        List<ParticipantIdentity> participants = participantIdentities(match, focus);
        return new NarrativeEvidence(
                "narrative-evidence.v1",
                participants,
                metricComparisons(participants, snapshots),
                evidenceFocuses(phases, events),
                combatEvidence(combats, participants, focus),
                List.of(
                        "Пополнения сохраняют смысл ADR-012: юниты стали доступны в интервале; локальное участие не утверждается без пространственных evidence."
                )
        );
    }

    private List<ParticipantIdentity> participantIdentities(Match match, PlayerState focus) {
        List<PlayerState> players = match.players().stream()
                .sorted(participantOrder(focus))
                .toList();
        List<ParticipantIdentity> identities = new ArrayList<>();
        int order = 0;
        for (PlayerState player : players) {
            Relationship relationship = relationship(player, focus);
            identities.add(new ParticipantIdentity(
                    participantId(player.name()),
                    player.pid(),
                    player.name(),
                    player.team(),
                    relationship,
                    relationship == Relationship.SELECTED,
                    styleKey(player, relationship),
                    order++
            ));
        }
        return List.copyOf(identities);
    }

    private Comparator<PlayerState> participantOrder(PlayerState focus) {
        return Comparator
                .comparingInt((PlayerState player) -> relationshipOrder(relationship(player, focus)))
                .thenComparing(player -> player.team() == null ? Integer.MAX_VALUE : player.team())
                .thenComparingInt(PlayerState::pid)
                .thenComparing(PlayerState::name);
    }

    private int relationshipOrder(Relationship relationship) {
        return switch (relationship) {
            case SELECTED -> 0;
            case TEAMMATE -> 1;
            case OPPONENT -> 2;
            case UNKNOWN -> 3;
        };
    }

    private Relationship relationship(PlayerState player, PlayerState focus) {
        if (player.pid() == focus.pid()) return Relationship.SELECTED;
        if (player.team() == null || focus.team() == null) return Relationship.UNKNOWN;
        return Objects.equals(player.team(), focus.team()) ? Relationship.TEAMMATE : Relationship.OPPONENT;
    }

    private String participantId(String playerName) {
        return "participant-" + slug(playerName);
    }

    private String styleKey(PlayerState player, Relationship relationship) {
        return relationship.name().toLowerCase() + "-team-" + (player.team() == null ? "unknown" : player.team())
                + "-pid-" + player.pid();
    }

    private List<MetricComparison> metricComparisons(List<ParticipantIdentity> participants, List<MatchStateSnapshot> snapshots) {
        return List.of(
                metricComparison("armyValue", "Стоимость армии", "resources", "MatchContext.playerMetrics.armyValue",
                        Completeness.COMPLETE, participants, snapshots, MatchStateSnapshot.Metrics::armyValue),
                metricComparison("economyProxy", "Экономика", "proxy", "MatchContext.playerMetrics.economyProxy",
                        Completeness.PARTIAL, participants, snapshots, MatchStateSnapshot.Metrics::economyProxy),
                metricComparison("supplyUsed", "Занятый лимит", "supply", "MatchContext.playerMetrics.supplyUsed",
                        Completeness.PARTIAL, participants, snapshots, MatchStateSnapshot.Metrics::supplyUsed)
        );
    }

    private MetricComparison metricComparison(String id, String label, String unit, String source, Completeness baseline,
                                              List<ParticipantIdentity> participants, List<MatchStateSnapshot> snapshots,
                                              ToDoubleFunction<MatchStateSnapshot.Metrics> value) {
        List<ParticipantMetricSeries> series = participants.stream()
                .map(participant -> participantSeries(id, participant, snapshots, value))
                .toList();
        Completeness completeness = series.stream().anyMatch(item -> item.completeness() == Completeness.UNAVAILABLE)
                ? Completeness.PARTIAL
                : baseline;
        return new MetricComparison(id, label, unit, source, completeness, series);
    }

    private ParticipantMetricSeries participantSeries(String metricId, ParticipantIdentity participant,
                                                      List<MatchStateSnapshot> snapshots,
                                                      ToDoubleFunction<MatchStateSnapshot.Metrics> value) {
        List<Point> points = new ArrayList<>();
        for (MatchStateSnapshot snapshot : snapshots) {
            MatchStateSnapshot.Metrics metrics = snapshot.playerMetrics().get(participant.displayName());
            if (metrics == null) continue;
            points.add(new Point(snapshot.at(), value.applyAsDouble(metrics)));
        }
        Completeness completeness = points.isEmpty()
                ? Completeness.UNAVAILABLE
                : points.size() == snapshots.size() ? Completeness.COMPLETE : Completeness.PARTIAL;
        String lineStyle = switch (participant.relationship()) {
            case SELECTED -> "solid";
            case TEAMMATE -> "dashed";
            case OPPONENT -> "dotted";
            case UNKNOWN -> "dashdot";
        };
        int strokeWeight = participant.selected() ? 5 : 3;
        return new ParticipantMetricSeries(metricId + "-" + participant.id(), participant.id(),
                completeness, lineStyle, strokeWeight, points);
    }

    private List<EvidenceFocus> evidenceFocuses(List<MatchPhase> phases, List<NarrativeEvent> events) {
        List<EvidenceFocus> focuses = new ArrayList<>();
        phases.forEach(phase -> focuses.add(new EvidenceFocus("focus-" + phase.id(), FocusKind.PHASE,
                phase.title(), phase.startedAt(), phase.startedAt(), phase.endedAt(), phase.id())));
        events.forEach(event -> focuses.add(new EvidenceFocus("focus-" + event.id(), focusKind(event.kind()),
                event.title(), event.at(), event.at(), event.endedAt(), event.id())));
        return focuses.stream()
                .sorted(Comparator.comparing(EvidenceFocus::at)
                        .thenComparing(focus -> focusKindOrder(focus.kind()))
                        .thenComparing(EvidenceFocus::sourceId))
                .toList();
    }

    private FocusKind focusKind(NarrativeEvent.Kind kind) {
        return switch (kind) {
            case COMBAT -> FocusKind.COMBAT;
            case TURNING_POINT -> FocusKind.TURNING_POINT;
            default -> FocusKind.NARRATIVE_EVENT;
        };
    }

    private int focusKindOrder(FocusKind kind) {
        return switch (kind) {
            case PHASE -> 0;
            case COMBAT -> 1;
            case TURNING_POINT -> 2;
            case NARRATIVE_EVENT -> 3;
        };
    }

    private List<CombatEvidence> combatEvidence(List<Combat> combats, List<ParticipantIdentity> identities, PlayerState focus) {
        Map<String, ParticipantIdentity> byName = identities.stream()
                .collect(Collectors.toMap(ParticipantIdentity::displayName, identity -> identity, (left, right) -> left, LinkedHashMap::new));
        List<CombatEvidence> result = new ArrayList<>();
        int index = 1;
        for (Combat combat : combats) {
            Map<Integer, List<CombatParticipantEvidence>> byTeam = new LinkedHashMap<>();
            for (Combat.Participant participant : combat.participants()) {
                ParticipantIdentity identity = byName.get(participant.player());
                Integer team = identity == null ? null : identity.teamId();
                byTeam.computeIfAbsent(team == null ? Integer.MAX_VALUE : team, ignored -> new ArrayList<>())
                        .add(combatParticipantEvidence(participant, identity));
            }
            List<CombatSideEvidence> sides = byTeam.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(sideOrder(focus.team())))
                    .map(entry -> combatSideEvidence(entry.getKey(), entry.getValue(), focus.team()))
                    .toList();
            Completeness completeness = sides.stream().anyMatch(side -> side.completeness() != Completeness.COMPLETE)
                    ? Completeness.PARTIAL
                    : Completeness.COMPLETE;
            result.add(new CombatEvidence(
                    combat.id(),
                    combat.ordinalLabel() == null ? "Бой " + index : combat.ordinalLabel(),
                    combat.startedAt(),
                    combat.endedAt(),
                    completeness,
                    sides,
                    List.of()
            ));
            index++;
        }
        return List.copyOf(result);
    }

    private Comparator<Integer> sideOrder(Integer focusTeam) {
        return Comparator
                .comparingInt((Integer team) -> Objects.equals(team, focusTeam) ? 0 : team == Integer.MAX_VALUE ? 2 : 1)
                .thenComparingInt(Integer::intValue);
    }

    private CombatParticipantEvidence combatParticipantEvidence(Combat.Participant participant, ParticipantIdentity identity) {
        String participantId = identity == null ? participantId(participant.player()) : identity.id();
        List<UnitEvidenceRow> rows = unitRows(participant);
        Completeness completeness = participant.reconciliationStatus() == Combat.ReconciliationStatus.EXACT
                ? Completeness.COMPLETE
                : Completeness.PARTIAL;
        List<String> issues = participant.reconciliationIssues().stream()
                .map(issue -> issue.unit() + ": " + issue.startCount() + " + " + issue.additions()
                        + " - " + issue.losses() + " = " + issue.expectedEndCount()
                        + ", actual " + issue.actualEndCount())
                .toList();
        return new CombatParticipantEvidence(
                participantId,
                participant.player(),
                completeness,
                rows,
                participant.workersLost(),
                participant.structuresLost(),
                participant.staticDefenseLost(),
                participant.reconciliationStatus().name(),
                issues
        );
    }

    private List<UnitEvidenceRow> unitRows(Combat.Participant participant) {
        Set<String> units = new TreeSet<>();
        units.addAll(participant.armyBefore().keySet());
        units.addAll(participant.additions().keySet());
        units.addAll(participant.unitsLost().keySet());
        units.addAll(participant.armyAfter().keySet());
        List<UnitEvidenceRow> rows = new ArrayList<>();
        for (String unit : units) {
            boolean exact = participant.reconciliationStatus() == Combat.ReconciliationStatus.EXACT
                    && participant.reconciliationIssues().stream().noneMatch(issue -> issue.unit().equals(unit));
            rows.add(new UnitEvidenceRow(
                    unit,
                    count(participant.armyBefore(), unit),
                    count(participant.additions(), unit),
                    count(participant.unitsLost(), unit),
                    count(participant.armyAfter(), unit),
                    CountEvidence.unknown("Killer-unit identity недоступен в текущем combat evidence; неизвестно не равно нулю."),
                    exact ? Completeness.COMPLETE : Completeness.PARTIAL,
                    exact ? "EXACT" : "PARTIAL"
            ));
        }
        return List.copyOf(rows);
    }

    private CombatSideEvidence combatSideEvidence(Integer teamKey, List<CombatParticipantEvidence> participants, Integer focusTeam) {
        Integer teamId = teamKey == Integer.MAX_VALUE ? null : teamKey;
        Relationship relationship = teamId == null ? Relationship.UNKNOWN
                : Objects.equals(teamId, focusTeam) ? Relationship.TEAMMATE : Relationship.OPPONENT;
        List<UnitEvidenceRow> totals = totalRows(participants);
        Map<String, Integer> workers = sumLosses(participants, CombatParticipantEvidence::workerLosses);
        Map<String, Integer> structures = sumLosses(participants, CombatParticipantEvidence::structureLosses);
        Map<String, Integer> defense = sumLosses(participants, CombatParticipantEvidence::staticDefenseLosses);
        Completeness completeness = participants.stream().anyMatch(participant -> participant.completeness() != Completeness.COMPLETE)
                ? Completeness.PARTIAL
                : Completeness.COMPLETE;
        String label = relationship == Relationship.TEAMMATE ? "Команда фокуса"
                : relationship == Relationship.OPPONENT ? "Соперники"
                : "Неизвестная сторона";
        return new CombatSideEvidence(
                teamId == null ? "side-unknown" : "team-" + teamId,
                label,
                teamId,
                relationship,
                completeness,
                totals,
                workers,
                structures,
                defense,
                participants
        );
    }

    private List<UnitEvidenceRow> totalRows(List<CombatParticipantEvidence> participants) {
        Set<String> units = participants.stream()
                .flatMap(participant -> participant.rows().stream().map(UnitEvidenceRow::unit))
                .collect(Collectors.toCollection(TreeSet::new));
        List<UnitEvidenceRow> rows = new ArrayList<>();
        for (String unit : units) {
            int start = 0;
            int additions = 0;
            int losses = 0;
            int end = 0;
            boolean partial = false;
            for (CombatParticipantEvidence participant : participants) {
                for (UnitEvidenceRow row : participant.rows()) {
                    if (!row.unit().equals(unit)) continue;
                    start += row.startCount();
                    additions += row.additions();
                    losses += row.losses();
                    end += row.endCount();
                    partial |= row.completeness() != Completeness.COMPLETE;
                }
            }
            rows.add(new UnitEvidenceRow(unit, start, additions, losses, end,
                    CountEvidence.unknown("Командные убийства недоступны без attribution по killer-unit."),
                    partial ? Completeness.PARTIAL : Completeness.COMPLETE,
                    partial ? "PARTIAL" : "EXACT"));
        }
        return List.copyOf(rows);
    }

    private Map<String, Integer> sumLosses(List<CombatParticipantEvidence> participants,
                                           java.util.function.Function<CombatParticipantEvidence, Map<String, Integer>> mapper) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (CombatParticipantEvidence participant : participants) {
            mapper.apply(participant).forEach((unit, count) -> result.merge(unit, count, Integer::sum));
        }
        return result;
    }

    private int count(Map<String, Integer> value, String key) {
        return value.getOrDefault(key, 0);
    }

    private String slug(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");
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
                + teamText + ". Narrative Analysis показывает фазы матча и контекст переходов; стратегический результат не вычисляется.";
        List<String> points = new ArrayList<>();
        phases.stream().limit(4).forEach(phase -> points.add(phase.title() + " " + clock(phase.startedAt()) + "–" + clock(phase.endedAt())));
        points.add("Стратегический результат: не оценивался");
        return new NarrativeSummary(
                verdict,
                "NOT_EVALUATED",
                points,
                List.of("Сценарная цепочка использует осторожные связи вроде предшествования или восстановления; она не утверждает обязательных победителей или эффективность разменов.")
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

    private record FeatureSample(Duration at, double[] values) {}

    private record Candidate(MatchStateSnapshot before, MatchStateSnapshot after, Metric metric, double delta) {}
}
