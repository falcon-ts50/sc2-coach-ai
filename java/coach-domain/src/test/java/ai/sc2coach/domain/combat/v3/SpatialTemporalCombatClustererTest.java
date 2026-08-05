package ai.sc2coach.domain.combat.v3;

import ai.sc2coach.domain.ReplayAnalysis;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpatialTemporalCombatClustererTest {

    private final CombatClusterer clusterer = new SpatialTemporalCombatClusterer(
            new CombatClusteringConfig(Duration.ofSeconds(12), 28.0, 0.20)
    );

    @Test
    void clustersEventsThatAreCloseInTimeAndSpace() {
        List<CombatCluster> clusters = clusterer.cluster(List.of(
                command("a-attack", 100, "Alpha", 40, 40),
                death("bravo-marine", 105, "Bravo", "Alpha", 44, 42)
        ));

        assertThat(clusters).hasSize(1);
        assertThat(clusters.getFirst().participants()).containsExactly("Alpha", "Bravo");
        assertThat(clusters.getFirst().deaths()).hasSize(1);
        assertThat(clusters.getFirst().combatCommandsAndAbilities()).hasSize(1);
        assertThat(clusters.getFirst().region().center().x()).isBetween(40.0, 44.0);
    }

    @Test
    void separatesEventsThatAreCloseInTimeButFarApart() {
        List<CombatCluster> clusters = clusterer.cluster(List.of(
                command("north-attack", 100, "Alpha", 20, 20),
                death("north-death", 103, "Bravo", "Alpha", 22, 21),
                command("south-attack", 104, "Charlie", 190, 190),
                death("south-death", 106, "Delta", "Charlie", 193, 188)
        ));

        assertThat(clusters).hasSize(2);
        assertThat(clusters.get(0).participants()).containsExactly("Alpha", "Bravo");
        assertThat(clusters.get(1).participants()).containsExactly("Charlie", "Delta");
    }

    @Test
    void keepsSmallTemporalGapsInsideAnOngoingFight() {
        List<CombatCluster> clusters = clusterer.cluster(List.of(
                command("opening", 100, "Alpha", 50, 50),
                death("trade-1", 111, "Bravo", "Alpha", 52, 51),
                death("trade-2", 122, "Alpha", "Bravo", 53, 53)
        ));

        assertThat(clusters).hasSize(1);
        assertThat(clusters.getFirst().start()).isEqualTo(Duration.ofSeconds(100));
        assertThat(clusters.getFirst().end()).isEqualTo(Duration.ofSeconds(122));
    }

    @Test
    void splitsLargeTemporalGapsIntoSeparateFights() {
        List<CombatCluster> clusters = clusterer.cluster(List.of(
                command("first", 100, "Alpha", 50, 50),
                death("first-death", 108, "Bravo", "Alpha", 51, 51),
                command("second", 140, "Alpha", 52, 52),
                death("second-death", 145, "Bravo", "Alpha", 53, 52)
        ));

        assertThat(clusters).hasSize(2);
        assertThat(clusters).extracting(CombatCluster::start)
                .containsExactly(Duration.ofSeconds(100), Duration.ofSeconds(140));
    }

    @Test
    void missingCoordinatesLowerConfidenceWithoutThrowing() {
        List<CombatCluster> clusters = clusterer.cluster(List.of(
                command("located", 100, "Alpha", 50, 50),
                CombatEvidence.death("missing-location", Duration.ofSeconds(105),
                        "Bravo", "Alpha", "Marine", null)
        ));

        assertThat(clusters).hasSize(1);
        assertThat(clusters.getFirst().hasMissingSpatialData()).isTrue();
        assertThat(clusters.getFirst().evidenceWithoutSpatialData()).isEqualTo(1);
        assertThat(clusters.getFirst().confidence()).isLessThan(1.0);
        assertThat(clusters.getFirst().participants()).containsExactly("Alpha", "Bravo");
    }

    @Test
    void extractorKeepsKillerAndVictimOwnerSeparate() {
        ReplayAnalysis analysis = analysis(
                players("Alpha", "Bravo"),
                List.of(deathEvent(120, 1, "Marine", "Alpha", Map.of("owner", "Bravo"), 70, 70))
        );

        List<CombatEvidence> evidence = new ReplayCombatEvidenceExtractor().extract(analysis);

        assertThat(evidence).hasSize(1);
        assertThat(evidence.getFirst().killer()).isEqualTo("Alpha");
        assertThat(evidence.getFirst().victimOwner()).isEqualTo("Bravo");
        assertThat(evidence.getFirst().participantNames()).containsExactly("Alpha", "Bravo");
    }

    @Test
    void teamFightKeepsMultipleParticipants() {
        List<CombatCluster> clusters = clusterer.cluster(List.of(
                command("alpha-attack", 100, "Alpha", 80, 80),
                command("charlie-attack", 102, "Charlie", 82, 78),
                death("bravo-loss", 105, "Bravo", "Alpha", 81, 81),
                death("delta-loss", 107, "Delta", "Charlie", 83, 79)
        ));

        assertThat(clusters).hasSize(1);
        assertThat(clusters.getFirst().participants())
                .containsExactly("Alpha", "Charlie", "Bravo", "Delta");
    }

    @Test
    void extractorDoesNotIncludePlayerWhoOnlyBuiltUnits() {
        ReplayAnalysis analysis = analysis(
                players("Alpha", "Bravo", "Macro"),
                List.of(
                        commandEvent(100, 1, "Attack", 35, 35),
                        deathEvent(104, 1, "Marine", "Alpha", Map.of("owner", "Bravo"), 36, 34),
                        birthEvent(105, 3, "Probe", 35, 35)
                )
        );

        List<CombatCluster> clusters = clusterer.cluster(new ReplayCombatEvidenceExtractor().extract(analysis));

        assertThat(clusters).hasSize(1);
        assertThat(clusters.getFirst().participants()).containsExactly("Alpha", "Bravo");
        assertThat(clusters.getFirst().participants()).doesNotContain("Macro");
    }

    @Test
    void parallelTwoVersusTwoFightsDoNotMergeAcrossTheMap() {
        List<CombatCluster> clusters = clusterer.cluster(List.of(
                command("north-alpha", 100, "Alpha", 20, 20),
                command("south-charlie", 101, "Charlie", 180, 180),
                death("north-bravo", 103, "Bravo", "Alpha", 23, 21),
                death("south-delta", 104, "Delta", "Charlie", 182, 179)
        ));

        assertThat(clusters).hasSize(2);
        assertThat(clusters.get(0).participants()).containsExactly("Alpha", "Bravo");
        assertThat(clusters.get(1).participants()).containsExactly("Charlie", "Delta");
    }

    private static CombatEvidence command(String id, int seconds, String player, double x, double y) {
        return CombatEvidence.combatCommand(id, Duration.ofSeconds(seconds), player, "Attack", new MapPoint(x, y));
    }

    private static CombatEvidence death(String id, int seconds, String victimOwner, String killer, double x, double y) {
        return CombatEvidence.death(id, Duration.ofSeconds(seconds), victimOwner, killer, "Marine", new MapPoint(x, y));
    }

    private static List<ReplayAnalysis.Player> players(String... names) {
        return java.util.stream.IntStream.range(0, names.length)
                .mapToObj(index -> new ReplayAnalysis.Player(index + 1, names[index], null,
                        index % 2 == 0 ? 1 : 2, null, null, null, List.of()))
                .toList();
    }

    private static ReplayAnalysis analysis(
            List<ReplayAnalysis.Player> players,
            List<ReplayAnalysis.TimelineEvent> timeline
    ) {
        return new ReplayAnalysis("test", "unit-test", null, null, players, timeline, null);
    }

    private static ReplayAnalysis.TimelineEvent commandEvent(
            double time,
            int player,
            String ability,
            double x,
            double y
    ) {
        return new ReplayAnalysis.TimelineEvent(time, null, player, "CommandEvent", null, null,
                null, null, ability, null, null, new ReplayAnalysis.Position(x, y), Map.of());
    }

    private static ReplayAnalysis.TimelineEvent deathEvent(
            double time,
            int player,
            String unit,
            String killer,
            Map<String, Object> attributes,
            double x,
            double y
    ) {
        return new ReplayAnalysis.TimelineEvent(time, null, player, "UnitDiedEvent", unit, null,
                unit, killer, null, null, new ReplayAnalysis.Position(x, y), null, attributes);
    }

    private static ReplayAnalysis.TimelineEvent birthEvent(
            double time,
            int player,
            String unit,
            double x,
            double y
    ) {
        return new ReplayAnalysis.TimelineEvent(time, null, player, "UnitBornEvent", unit, null,
                null, null, null, null, new ReplayAnalysis.Position(x, y), null, Map.of());
    }
}
