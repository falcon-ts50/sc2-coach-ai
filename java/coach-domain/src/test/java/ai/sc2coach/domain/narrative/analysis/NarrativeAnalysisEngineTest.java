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
        assertThat(analysis.timeline().snapshots()).allSatisfy(snapshot ->
                assertThat(snapshot.teamPlayers()).contains("Lulu", "dragonDriver")
        );
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

    private MatchContext.Component component(double value) {
        return new MatchContext.Component(value, 0, 0, EVEN);
    }
}
