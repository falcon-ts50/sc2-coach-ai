package ai.sc2coach.domain.information;

import ai.sc2coach.domain.ReplayAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InformationEngineTest {

    private final InformationEngine engine = new InformationEngine();

    @Test
    void reaperPotentiallyObservesRoachWarrenAndBunkerIsResponseCandidate() {
        InformationReport report = engine.analyze(analysis(List.of(
                lifecycle(150, "dragonDriver", "Reaper", 20, 20),
                lifecycle(181, "Guardian", "Hatchery", 30, 20),
                lifecycle(185, "Guardian", "RoachWarren", 27, 20),
                death(205, "Reaper", "dragonDriver", "Guardian", 28, 20),
                lifecycle(239, "dragonDriver", "Bunker", 12, 12)
        )));

        assertThat(report.episodes()).hasSize(1);
        InformationEpisode episode = report.episodes().getFirst();
        assertThat(episode.scoutPlayer()).isEqualTo("dragonDriver");
        assertThat(episode.scoutUnit()).isEqualTo("Reaper");
        assertThat(episode.targetPlayer()).isEqualTo("Guardian");
        assertThat(episode.potentiallyObserved())
                .extracting(InformationObservation::subject)
                .contains("RoachWarren");
        assertThat(episode.reactionCandidates())
                .extracting(InformationReaction::action)
                .contains("Build Bunker");
        assertThat(episode.reactionCandidates().getFirst().basis())
                .isEqualTo("Potentially Observed Roach Warren");
    }

    @Test
    void overlordDiesEarlyAndCreatesMissingInformation() {
        InformationReport report = engine.analyze(analysis(List.of(
                lifecycle(90, "Guardian", "Overlord", 50, 50),
                lifecycle(94, "dragonDriver", "Barracks", 55, 50),
                death(100, "Overlord", "Guardian", "dragonDriver", 56, 50)
        )));

        InformationEpisode episode = report.episodes().getFirst();
        assertThat(episode.survived()).isFalse();
        assertThat(episode.missingInformation())
                .extracting(InformationGap::topic)
                .contains("Main Tech", "Army Composition", "Second Gas");
        assertThat(episode.confidence().value()).isLessThan(0.6);
    }

    @Test
    void observerPotentiallyObservesRoboticsFacilityAndTurretOrVikingCanBeResponseCandidate() {
        InformationReport report = engine.analyze(analysis(List.of(
                lifecycle(210, "Aurora", "Observer", 80, 80),
                lifecycle(216, "dragonDriver", "RoboticsFacility", 86, 80),
                lifecycle(225, "Aurora", "Observer", 83, 80),
                lifecycle(258, "Aurora", "MissileTurret", 70, 70),
                lifecycle(264, "Aurora", "Viking", 72, 72)
        )));

        InformationEpisode episode = report.episodes().getFirst();
        assertThat(episode.potentiallyObserved())
                .extracting(InformationObservation::subject)
                .contains("RoboticsFacility");
        assertThat(episode.reactionCandidates())
                .extracting(InformationReaction::action)
                .contains("Build MissileTurret", "Build Viking");
        assertThat(episode.reactionCandidates())
                .extracting(InformationReaction::basis)
                .contains("Potentially Observed Robotics Facility");
    }

    @Test
    void workerScoutThatDiesAfterTenSecondsHasLowConfidence() {
        InformationReport report = engine.analyze(analysis(List.of(
                lifecycle(100, "dragonDriver", "SCV", 40, 40),
                lifecycle(103, "Guardian", "SpawningPool", 45, 40),
                death(110, "SCV", "dragonDriver", "Guardian", 46, 40)
        )));

        InformationEpisode episode = report.episodes().getFirst();
        assertThat(episode.scoutUnit()).isEqualTo("SCV");
        assertThat(episode.survived()).isFalse();
        assertThat(episode.confidence().value()).isLessThan(0.6);
        assertThat(episode.missingInformation()).isNotEmpty();
    }

    @Test
    void scoutThatSurvivesHasHigherConfidence() {
        InformationReport report = engine.analyze(analysis(List.of(
                lifecycle(100, "dragonDriver", "Reaper", 40, 40),
                lifecycle(112, "Guardian", "SpawningPool", 46, 40),
                lifecycle(124, "dragonDriver", "Reaper", 43, 41),
                lifecycle(135, "Guardian", "Queen", 47, 42)
        )));

        InformationEpisode episode = report.episodes().getFirst();
        assertThat(episode.survived()).isTrue();
        assertThat(episode.confidence().value()).isGreaterThan(0.7);
    }

    @Test
    void noScoutingProducesNoReactionCandidates() {
        InformationReport report = engine.analyze(analysis(List.of(
                lifecycle(100, "dragonDriver", "Marine", 20, 20),
                lifecycle(110, "Guardian", "RoachWarren", 80, 80),
                lifecycle(140, "dragonDriver", "Bunker", 22, 20)
        )));

        assertThat(report.episodes()).isEmpty();
        assertThat(report.narratives()).isEmpty();
        assertThat(report.states())
                .flatExtracting(InformationState::entries)
                .allMatch(entry -> entry.knowledge() == InformationState.Knowledge.UNKNOWN);
    }

    @Test
    void informationStateKeepsPotentialKnowledgeWithoutSingleAdvantageScore() {
        InformationReport report = engine.analyze(analysis(List.of(
                lifecycle(150, "dragonDriver", "Reaper", 20, 20),
                lifecycle(181, "Guardian", "Hatchery", 30, 20),
                lifecycle(185, "Guardian", "RoachWarren", 27, 20),
                death(205, "Reaper", "dragonDriver", "Guardian", 28, 20)
        )));

        InformationState dragonDriver = report.states().stream()
                .filter(state -> state.player().equals("dragonDriver"))
                .findFirst()
                .orElseThrow();
        assertThat(dragonDriver.entries())
                .filteredOn(entry -> entry.topic().equals("army tech"))
                .singleElement()
                .extracting(InformationState.Entry::knowledge)
                .isEqualTo(InformationState.Knowledge.POTENTIALLY_KNOWN);
        assertThat(report.advantage().states()).contains(dragonDriver);
    }

    @Test
    void narrativeUsesUncertainLanguageOnly() {
        InformationReport report = engine.analyze(analysis(List.of(
                lifecycle(150, "dragonDriver", "Reaper", 20, 20),
                lifecycle(185, "Guardian", "RoachWarren", 27, 20),
                death(205, "Reaper", "dragonDriver", "Guardian", 28, 20),
                lifecycle(239, "dragonDriver", "Bunker", 12, 12)
        )));

        String narrative = report.narratives().getFirst().text();

        assertThat(narrative).contains("потенциально мог увидеть");
        assertThat(narrative).contains("решение согласуется");
        assertThat(narrative).doesNotContain("игрок увидел", "решил потому что");
    }

    private ReplayAnalysis analysis(List<ReplayAnalysis.TimelineEvent> timeline) {
        return new ReplayAnalysis(
                "1",
                "test",
                new ReplayAnalysis.Replay("map", "release", 1L, "2v2", 900.0, List.of("dragonDriver")),
                "dragonDriver",
                List.of(
                        new ReplayAnalysis.Player(1, "dragonDriver", "Terran", 1, "Win", null, null, List.of()),
                        new ReplayAnalysis.Player(2, "Guardian", "Zerg", 2, "Loss", null, null, List.of()),
                        new ReplayAnalysis.Player(3, "Aurora", "Protoss", 1, "Win", null, null, List.of())
                ),
                timeline,
                null
        );
    }

    private ReplayAnalysis.TimelineEvent lifecycle(
            double time,
            String player,
            String unit,
            double x,
            double y
    ) {
        return new ReplayAnalysis.TimelineEvent(
                time, null, player, "UnitBornEvent", unit, null, null, null, null,
                null, new ReplayAnalysis.Position(x, y), null, Map.of()
        );
    }

    private ReplayAnalysis.TimelineEvent death(
            double time,
            String unit,
            String owner,
            String killer,
            double x,
            double y
    ) {
        return new ReplayAnalysis.TimelineEvent(
                time, null, killer, "UnitDiedEvent", unit, null, unit, killer, null,
                null, new ReplayAnalysis.Position(x, y), null, Map.of("owner", owner)
        );
    }
}
