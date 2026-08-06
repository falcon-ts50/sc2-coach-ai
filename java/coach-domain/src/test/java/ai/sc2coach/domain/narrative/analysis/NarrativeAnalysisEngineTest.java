package ai.sc2coach.domain.narrative.analysis;

import ai.sc2coach.domain.combat.Combat;
import ai.sc2coach.domain.context.MatchContext;
import ai.sc2coach.domain.context.TurningPoint;
import ai.sc2coach.domain.model.Match;
import ai.sc2coach.domain.model.PlayerState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static ai.sc2coach.domain.context.MatchContext.Rank.AHEAD;
import static ai.sc2coach.domain.context.MatchContext.Rank.BEHIND;
import static ai.sc2coach.domain.context.MatchContext.Rank.EVEN;
import static org.assertj.core.api.Assertions.assertThat;

class NarrativeAnalysisEngineTest {

    private final NarrativeAnalysisEngine engine = new NarrativeAnalysisEngine();

    @Test
    void producesDeterministicTimelineSummaryAndChart() {
        NarrativeAnalysis first = engine.analyze(input());
        NarrativeAnalysis second = engine.analyze(input());

        assertThat(first).isEqualTo(second);
        assertThat(first.timeline().phases()).extracting(MatchPhase::title)
                .contains("Раннее давление", "Стабилизация", "Средняя стадия", "Позднее ухудшение");
        assertThat(first.chart().series()).extracting(NarrativeChartModel.Series::id)
                .containsExactly("armyValue", "economyProxy", "supplyUsed");
        assertThat(first.chart().phaseIntervals()).hasSameSizeAs(first.timeline().phases());
    }

    @Test
    void keepsStrategicResultNotEvaluatedDespiteOfficialReplayWin() {
        NarrativeAnalysis analysis = engine.analyze(input());

        assertThat(analysis.officialReplayResult()).isEqualTo("Win");
        assertThat(analysis.strategicResultStatus()).isEqualTo("NOT_EVALUATED");
        assertThat(analysis.summary().verdict()).contains("strategic result не вычисляется");
        assertThat(analysis.summary().limitations()).anyMatch(item -> item.contains("does not assert"));
    }

    @Test
    void preservesTeamPerspectiveForDragonDriverAndLulu() {
        NarrativeAnalysis analysis = engine.analyze(input());

        assertThat(analysis.focusPlayer()).isEqualTo("dragonDriver");
        assertThat(analysis.focusTeamPlayers()).containsExactly("Lulu", "dragonDriver");
        assertThat(analysis.evidence().participants()).extracting(NarrativeEvidence.ParticipantIdentity::displayName)
                .containsExactly("dragonDriver", "Lulu", "Frontdoor", "Guardian");
        assertThat(analysis.evidence().participants().getFirst().relationship())
                .isEqualTo(NarrativeEvidence.Relationship.SELECTED);
        assertThat(analysis.evidence().participants().getFirst().styleKey())
                .contains("pid-3");
        assertThat(analysis.timeline().snapshots()).allSatisfy(snapshot ->
                assertThat(snapshot.teamPlayers()).contains("Lulu", "dragonDriver")
        );
    }

    @Test
    void emitsAllParticipantMetricComparisonsOnSharedDomain() {
        NarrativeAnalysis analysis = engine.analyze(input());

        assertThat(analysis.evidence().metricComparisons()).extracting(NarrativeEvidence.MetricComparison::id)
                .containsExactly("armyValue", "economyProxy", "supplyUsed");
        NarrativeEvidence.MetricComparison army = analysis.evidence().metricComparisons().getFirst();

        assertThat(army.series()).hasSize(4);
        assertThat(army.series().getFirst().lineStyle()).isEqualTo("solid");
        assertThat(army.series().getFirst().strokeWeight()).isGreaterThan(army.series().get(1).strokeWeight());
        assertThat(army.series().getFirst().points()).hasSameSizeAs(analysis.timeline().snapshots());
        assertThat(army.series()).anySatisfy(series ->
                assertThat(series.completeness()).isEqualTo(NarrativeChartModel.Completeness.UNAVAILABLE)
        );
    }

    @Test
    void exposesCombatEvidenceWithoutInventingKillCredit() {
        NarrativeAnalysis analysis = engine.analyze(inputWithCombatParticipants());

        NarrativeEvidence.CombatEvidence combat = analysis.evidence().combats().getFirst();
        assertThat(combat.sides()).extracting(NarrativeEvidence.CombatSideEvidence::label)
                .containsExactly("Команда фокуса", "Соперники");
        NarrativeEvidence.CombatParticipantEvidence lulu = combat.sides().getFirst().participants().stream()
                .filter(participant -> participant.player().equals("Lulu"))
                .findFirst()
                .orElseThrow();
        NarrativeEvidence.UnitEvidenceRow row = lulu.rows().stream()
                .filter(unit -> unit.unit().equals("Zergling"))
                .findFirst()
                .orElseThrow();

        assertThat(row.startCount()).isEqualTo(2);
        assertThat(row.additions()).isEqualTo(16);
        assertThat(row.losses()).isEqualTo(3);
        assertThat(row.endCount()).isEqualTo(15);
        assertThat(row.creditedKills().value()).isNull();
        assertThat(row.creditedKills().completeness()).isEqualTo(NarrativeChartModel.Completeness.UNAVAILABLE);
    }

    @Test
    void ordersEqualTimestampFocusesDeterministically() {
        NarrativeAnalysis analysis = engine.analyze(inputWithCombatParticipants());

        assertThat(analysis.evidence().focuses())
                .extracting(focus -> focus.kind() + ":" + focus.sourceId())
                .containsSubsequence("COMBAT:combat-2", "TURNING_POINT:turning-point-1");
    }

    @Test
    void causalLinksUseCautiousSemanticsOnly() {
        NarrativeAnalysis analysis = engine.analyze(input());

        assertThat(analysis.timeline().causalLinks()).isNotEmpty();
        assertThat(analysis.timeline().causalLinks()).allSatisfy(link -> {
            assertThat(link.kind()).isIn(CausalLink.Kind.PRECEDED, CausalLink.Kind.RECOVERED_FROM);
            assertThat(link.statement()).contains("не доказывает");
            assertThat(link.statement()).doesNotContain("победил потому что");
        });
    }

    @Test
    void emitsContinuousMatchFlowWithoutTemporalGaps() {
        NarrativeAnalysis analysis = engine.analyze(input());

        MatchFlow matchFlow = analysis.matchFlow();

        assertThat(matchFlow.matchStartedAt()).isEqualTo(Duration.ZERO);
        assertThat(matchFlow.matchEndedAt()).isEqualTo(Duration.ofSeconds(1260));
        assertThat(matchFlow.intervals()).isNotEmpty();
        assertThat(matchFlow.intervals().getFirst().startedAt()).isEqualTo(matchFlow.matchStartedAt());
        assertThat(matchFlow.intervals().getLast().endedAt()).isEqualTo(matchFlow.matchEndedAt());
        for (int i = 0; i < matchFlow.intervals().size(); i++) {
            MatchFlow.MatchFlowInterval interval = matchFlow.intervals().get(i);
            assertThat(interval.ordinal()).isEqualTo(i);
            assertThat(interval.startedAt()).isLessThan(interval.endedAt());
            if (i > 0) {
                assertThat(interval.startedAt()).isEqualTo(matchFlow.intervals().get(i - 1).endedAt());
            }
        }
    }

    @Test
    void mapsCombatAndDevelopmentEvidenceToTheSameInterval() {
        NarrativeAnalysis analysis = engine.analyze(inputWithCombatParticipants());

        MatchFlow.MatchFlowInterval combatInterval = analysis.matchFlow().intervals().stream()
                .filter(interval -> interval.combatIds().contains("combat-2"))
                .findFirst()
                .orElseThrow();

        assertThat(combatInterval.kind()).isEqualTo(MatchFlow.Kind.COMBAT);
        assertThat(combatInterval.drilldown().combat().combatIds()).containsExactly("combat-2");
        assertThat(combatInterval.drilldown().combat().emptyStates()).isEmpty();
        assertThat(combatInterval.drilldown().development().production().observations())
                .anyMatch(item -> item.contains("Lulu") && item.contains("Zergling"));
        assertThat(combatInterval.drilldown().development().emptyStates()).isEmpty();
    }

    @Test
    void serializesSeparateEmptyStatesForNoCombatAndNoDevelopmentEvidence() {
        NarrativeAnalysis analysis = engine.analyze(lowEvidenceInput());

        assertThat(analysis.matchFlow().intervals()).hasSize(1);
        MatchFlow.MatchFlowInterval emptyInterval = analysis.matchFlow().intervals().stream()
                .filter(interval -> !interval.drilldown().combat().emptyStates().isEmpty())
                .filter(interval -> !interval.drilldown().development().emptyStates().isEmpty())
                .findFirst()
                .orElseThrow();

        assertThat(emptyInterval.drilldown().combat().emptyStates())
                .contains("Боёв в этом интервале не обнаружено.");
        assertThat(emptyInterval.drilldown().development().emptyStates())
                .contains("Экономических, производственных, технологических или разведывательных событий в этом интервале не обнаружено.");
    }

    private NarrativeAnalysisInput input() {
        Match match = new Match("Test Map", "2v2", Duration.ofMinutes(20), List.of("dragonDriver", "Lulu"),
                List.of(player(1, "Frontdoor", 1, "Loss"), player(2, "Guardian", 1, "Loss"),
                        player(3, "dragonDriver", 2, "Win"), player(4, "Lulu", 2, "Win")));
        MatchContext context = new MatchContext(List.of(
                frame(0, 500, 400, 14, 20),
                frame(240, 120, 500, 18, -35),
                frame(420, 820, 850, 39, 5),
                frame(840, 1600, 1350, 72, 45),
                frame(1080, 1400, 1200, 66, 15),
                frame(1260, 500, 400, 14, -74)
        ), MatchContext.MatchSummary.empty());
        List<Combat> combats = List.of(
                new Combat(Duration.ofSeconds(370), Duration.ofSeconds(423), "Frontdoor", "Lulu", null,
                        List.of(), "110,92", 0.82, "combat-2", "Бой 2")
        );
        List<TurningPoint> turningPoints = List.of(
                new TurningPoint(Duration.ofSeconds(840), 1, "Frontdoor", 3, "dragonDriver", 42,
                        TurningPoint.Severity.MAJOR, List.of())
        );
        return new NarrativeAnalysisInput(match, "dragonDriver", context, turningPoints, List.of(), combats, null, List.of());
    }

    private NarrativeAnalysisInput inputWithCombatParticipants() {
        Match match = new Match("Test Map", "2v2", Duration.ofMinutes(20), List.of("dragonDriver", "Lulu"),
                List.of(player(1, "Frontdoor", 1, "Loss"), player(2, "Guardian", 1, "Loss"),
                        player(3, "dragonDriver", 2, "Win"), player(4, "Lulu", 2, "Win")));
        Combat.Participant lulu = new Combat.Participant(
                "Lulu",
                Map.of("Zergling", 2),
                Map.of("Zergling", 16),
                Map.of("Zergling", 15),
                Map.of("Zergling", 3),
                Map.of("Drone", 1),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                250,
                850,
                Combat.ReconciliationStatus.EXACT,
                List.of()
        );
        Combat.Participant frontdoor = new Combat.Participant(
                "Frontdoor",
                Map.of("Marine", 5),
                Map.of(),
                Map.of("Marine", 4),
                Map.of("Marine", 1),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                250,
                200,
                Combat.ReconciliationStatus.EXACT,
                List.of()
        );
        List<Combat> combats = List.of(new Combat(Duration.ofSeconds(370), Duration.ofSeconds(423),
                "Frontdoor", "Lulu", null, List.of(lulu, frontdoor), "110,92", 0.82, "combat-2", "Бой 2"));
        List<TurningPoint> turningPoints = List.of(
                new TurningPoint(Duration.ofSeconds(370), 1, "Frontdoor", 3, "dragonDriver", 42,
                        TurningPoint.Severity.MAJOR, List.of())
        );
        return new NarrativeAnalysisInput(match, "dragonDriver", context(), turningPoints, List.of(), combats, null, List.of());
    }

    private NarrativeAnalysisInput lowEvidenceInput() {
        Match match = new Match("Test Map", "1v1", Duration.ofMinutes(10), List.of("dragonDriver"),
                List.of(player(1, "Frontdoor", 1, "Loss"), player(3, "dragonDriver", 2, "Win")));
        MatchContext context = new MatchContext(List.of(
                quietFrame(0),
                quietFrame(300),
                quietFrame(600)
        ), MatchContext.MatchSummary.empty());
        return new NarrativeAnalysisInput(match, "dragonDriver", context, List.of(), List.of(), List.of(), null, List.of());
    }

    private MatchContext context() {
        return new MatchContext(List.of(
                frame(0, 500, 400, 14, 20),
                frame(240, 120, 500, 18, -35),
                frame(420, 820, 850, 39, 5),
                frame(840, 1600, 1350, 72, 45),
                frame(1080, 1400, 1200, 66, 15),
                frame(1260, 500, 400, 14, -74)
        ), MatchContext.MatchSummary.empty());
    }

    private PlayerState player(int pid, String name, int team, String result) {
        return new PlayerState(pid, name, PlayerState.Race.TERRAN, team, result, null, null, List.of());
    }

    private MatchContext.ContextFrame frame(int seconds, double army, double economy, double supply, double score) {
        var dragon = new MatchContext.PlayerContext(3, "dragonDriver", component(economy), component(army), component(supply),
                score, score >= 0 ? AHEAD : BEHIND);
        var lulu = new MatchContext.PlayerContext(4, "Lulu", component(economy * 0.8), component(army * 0.7),
                component(Math.max(0, supply - 5)), score * 0.7, EVEN);
        var frontdoor = new MatchContext.PlayerContext(1, "Frontdoor", component(1000), component(900), component(48),
                30, AHEAD);
        return new MatchContext.ContextFrame(Duration.ofSeconds(seconds), List.of(frontdoor, dragon, lulu),
                score >= 30 ? 3 : 1, score, Math.abs(score) / 2);
    }

    private MatchContext.ContextFrame quietFrame(int seconds) {
        var dragon = new MatchContext.PlayerContext(3, "dragonDriver", component(100), component(100), component(20),
                0, EVEN);
        var frontdoor = new MatchContext.PlayerContext(1, "Frontdoor", component(100), component(100), component(20),
                0, EVEN);
        return new MatchContext.ContextFrame(Duration.ofSeconds(seconds), List.of(frontdoor, dragon), null, 0, 0);
    }

    private MatchContext.Component component(double value) {
        return new MatchContext.Component(value, 0, 0, EVEN);
    }
}
