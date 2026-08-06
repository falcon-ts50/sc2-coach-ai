package ai.sc2coach.domain.combat;

import ai.sc2coach.domain.ReplayAnalysis;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CombatEngineHistoryTest {

    @Test
    void exposesAdditionsWhenLossesExceedTheStartSnapshotButReconcile() {
        ReplayAnalysis analysis = analysis(
                players("Frontdoor", "Lulu"),
                List.of(
                        birth(80, 2, "Marine"),
                        birth(81, 2, "Marine"),
                        command(100, 1, "Attack"),
                        births(105, 2, "Marine", 16),
                        death(110, "Marine", "Lulu", "Frontdoor"),
                        death(111, "Marine", "Lulu", "Frontdoor"),
                        death(112, "Marine", "Lulu", "Frontdoor")
                )
        );

        Combat.Participant lulu = participant(new CombatEngine().detect(analysis, "Lulu").getFirst(), "Lulu");

        assertThat(lulu.armyBefore()).containsEntry("Marine", 2);
        assertThat(lulu.additions()).containsEntry("Marine", 16);
        assertThat(lulu.unitsLost()).containsEntry("Marine", 3);
        assertThat(lulu.armyAfter()).containsEntry("Marine", 15);
        assertThat(lulu.reconciliationStatus()).isEqualTo(Combat.ReconciliationStatus.EXACT);
        assertThat(lulu.reconciliationIssues()).isEmpty();
    }

    @Test
    void marksIncompleteLifecycleEvidenceWhenTheTransitionDoesNotReconcile() {
        ReplayAnalysis analysis = analysis(
                players("Alpha", "Bravo"),
                List.of(
                        birth(80, 2, "Marine"),
                        command(100, 1, "Attack"),
                        death(110, "Marine", "Bravo", "Alpha"),
                        death(111, "Marine", "Bravo", "Alpha")
                )
        );

        Combat.Participant bravo = participant(new CombatEngine().detect(analysis, "Bravo").getFirst(), "Bravo");

        assertThat(bravo.armyBefore()).containsEntry("Marine", 1);
        assertThat(bravo.additions()).isEmpty();
        assertThat(bravo.unitsLost()).containsEntry("Marine", 2);
        assertThat(bravo.armyAfter()).isEmpty();
        assertThat(bravo.reconciliationStatus()).isEqualTo(Combat.ReconciliationStatus.PARTIAL);
        assertThat(bravo.reconciliationIssues()).singleElement().satisfies(issue -> {
            assertThat(issue.unit()).isEqualTo("Marine");
            assertThat(issue.startCount()).isEqualTo(1);
            assertThat(issue.additions()).isZero();
            assertThat(issue.losses()).isEqualTo(2);
            assertThat(issue.expectedEndCount()).isEqualTo(-1);
            assertThat(issue.actualEndCount()).isZero();
        });
    }

    @Test
    void keepsTeamGameParticipantsAndVictimOwnerLossesSeparate() {
        ReplayAnalysis analysis = analysis(
                List.of(
                        player(1, "Alpha", 1),
                        player(2, "Bravo", 1),
                        player(3, "Charlie", 2),
                        player(4, "Delta", 2)
                ),
                List.of(
                        birth(80, 3, "Marine"),
                        birth(80, 4, "Marauder"),
                        command(100, 1, "Attack"),
                        command(101, 2, "Attack"),
                        death(110, "Marine", "Charlie", "Alpha"),
                        death(111, "Marauder", "Delta", "Bravo")
                )
        );

        Combat combat = new CombatEngine().detect(analysis, "Alpha").getFirst();

        assertThat(combat.ordinalLabel()).isEqualTo("Бой 1");
        assertThat(combat.id()).startsWith("combat-01-095-126-");
        assertThat(combat.participants()).extracting(Combat.Participant::player)
                .contains("Alpha", "Bravo", "Charlie", "Delta");
        assertThat(participant(combat, "Charlie").unitsLost()).containsEntry("Marine", 1);
        assertThat(participant(combat, "Delta").unitsLost()).containsEntry("Marauder", 1);
        assertThat(participant(combat, "Alpha").unitsLost()).isEmpty();
        assertThat(participant(combat, "Bravo").unitsLost()).isEmpty();
    }

    @Test
    void coversLateDeathsEvenAfterManyEarlierAttackWindows() {
        List<ReplayAnalysis.TimelineEvent> events = new ArrayList<>();
        events.add(births(90, 1, "Marine", 8));
        for (int index = 0; index < 8; index++) {
            double startedAt = 100 + index * 60;
            events.add(command(startedAt, 2, "Attack"));
            events.add(death(startedAt + 5, "Marine", "Frontdoor", "dragonDriver"));
        }
        events.add(births(1000, 1, "Battlecruiser", 7));
        events.add(death(1041, "Battlecruiser", "Frontdoor", "dragonDriver"));

        List<Combat> combats = new CombatEngine().detect(analysis(players("Frontdoor", "dragonDriver"), events), "dragonDriver");

        assertThat(combats).anySatisfy(combat ->
                assertThat(participant(combat, "Frontdoor").unitsLost()).containsEntry("Battlecruiser", 1));
    }

    @Test
    void includesTeamMateCombatWhenFocusPlayerIsNotPhysicallyPresent() {
        ReplayAnalysis analysis = analysis(
                List.of(
                        player(1, "Frontdoor", 1),
                        player(2, "dragonDriver", 2),
                        player(3, "Lulu", 2)
                ),
                List.of(
                        births(90, 1, "Battlecruiser", 2),
                        death(110, "Battlecruiser", "Frontdoor", "Lulu")
                )
        );

        List<Combat> combats = new CombatEngine().detect(analysis, "dragonDriver");

        assertThat(combats).singleElement().satisfies(combat ->
                assertThat(participant(combat, "Frontdoor").unitsLost()).containsEntry("Battlecruiser", 1));
    }

    private static Combat.Participant participant(Combat combat, String player) {
        return combat.participants().stream()
                .filter(candidate -> candidate.player().equals(player))
                .findFirst()
                .orElseThrow();
    }

    private static List<ReplayAnalysis.Player> players(String... names) {
        return java.util.stream.IntStream.range(0, names.length)
                .mapToObj(index -> player(index + 1, names[index], index + 1))
                .toList();
    }

    private static ReplayAnalysis.Player player(int pid, String name, int team) {
        return new ReplayAnalysis.Player(pid, name, null, team, null, null, null, List.of());
    }

    private static ReplayAnalysis analysis(
            List<ReplayAnalysis.Player> players,
            List<ReplayAnalysis.TimelineEvent> events
    ) {
        return new ReplayAnalysis("test", "unit-test", null, null, players,
                events.stream().flatMap(event -> {
                    Object count = event.attributes() == null ? null : event.attributes().get("repeat");
                    if (!(count instanceof Integer repeat) || repeat <= 1) return java.util.stream.Stream.of(event);
                    return java.util.stream.IntStream.range(0, repeat)
                            .mapToObj(index -> new ReplayAnalysis.TimelineEvent(
                                    event.time() + index * 0.01,
                                    event.clock(),
                                    event.player(),
                                    event.event(),
                                    event.unit(),
                                    event.upgrade(),
                                    event.victim(),
                                    event.killer(),
                                    event.ability(),
                                    event.targetUnit(),
                                    event.position(),
                                    event.targetPosition(),
                                    Map.of()
                            ));
                }).sorted(java.util.Comparator.comparingDouble(event -> event.time() == null ? 0 : event.time()))
                        .toList(),
                null);
    }

    private static ReplayAnalysis.TimelineEvent births(double time, int player, String unit, int count) {
        return new ReplayAnalysis.TimelineEvent(time, null, player, "UnitBornEvent", unit, null,
                null, null, null, null, position(40, 40), null, Map.of("repeat", count));
    }

    private static ReplayAnalysis.TimelineEvent birth(double time, int player, String unit) {
        return new ReplayAnalysis.TimelineEvent(time, null, player, "UnitBornEvent", unit, null,
                null, null, null, null, position(40, 40), null, Map.of());
    }

    private static ReplayAnalysis.TimelineEvent death(double time, String unit, String victimOwner, String killer) {
        return new ReplayAnalysis.TimelineEvent(time, null, killer, "UnitDiedEvent", unit, null,
                victimOwner, killer, null, null, position(42, 42), null, Map.of());
    }

    private static ReplayAnalysis.TimelineEvent command(double time, int player, String ability) {
        return new ReplayAnalysis.TimelineEvent(time, null, player, "TargetPointCommandEvent", null, null,
                null, null, ability, null, null, position(42, 42), Map.of());
    }

    private static ReplayAnalysis.Position position(double x, double y) {
        return new ReplayAnalysis.Position(x, y);
    }
}
