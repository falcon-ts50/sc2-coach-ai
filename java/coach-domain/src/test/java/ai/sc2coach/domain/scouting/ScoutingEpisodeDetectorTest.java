package ai.sc2coach.domain.scouting;

import ai.sc2coach.domain.ReplayAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScoutingEpisodeDetectorTest {

    private final ScoutingEpisodeDetector detector = new ScoutingEpisodeDetector();

    @Test
    void treatsEarlyReaperDeathAsScoutingAndCorrelatesNearbyInformation() {
        var analysis = analysis(List.of(
                event(150, "dragonDriver", "UnitBornEvent", "Reaper", null, null, 20, 20),
                event(185, "Lulu", "UnitBornEvent", "RoachWarren", null, null, 29, 20),
                event(195, "Lulu", "UnitBornEvent", "Roach", null, null, 30, 20),
                event(205, "Lulu", "UnitDiedEvent", "Reaper", "dragonDriver", "Lulu", 31, 20),
                event(240, "dragonDriver", "UnitBornEvent", "Bunker", null, null, 10, 10)
        ));

        var episodes = detector.detect(analysis, "dragonDriver");

        assertThat(episodes).hasSize(1);
        var episode = episodes.getFirst();
        assertThat(episode.scoutUnit()).isEqualTo("Reaper");
        assertThat(episode.potentiallyObserved())
                .extracting(ScoutingEpisode.ObservedFact::subject)
                .contains("RoachWarren", "Roach");
        assertThat(episode.responseCandidates())
                .extracting(ScoutingEpisode.ResponseCandidate::action)
                .contains("Produced/built: Bunker");
        assertThat(episode.confidence()).isGreaterThan(0.7);
    }

    @Test
    void doesNotClaimFarAwayEnemyEventsWereObserved() {
        var analysis = analysis(List.of(
                event(150, "dragonDriver", "UnitBornEvent", "Reaper", null, null, 20, 20),
                event(190, "Lulu", "UnitBornEvent", "RoachWarren", null, null, 80, 80),
                event(205, "Lulu", "UnitDiedEvent", "Reaper", "dragonDriver", "Lulu", 21, 20)
        ));

        var episode = detector.detect(analysis, "dragonDriver").getFirst();

        assertThat(episode.potentiallyObserved()).isEmpty();
        assertThat(episode.responseCandidates()).isEmpty();
    }

    @Test
    void ignoresLateDeathsForEarlyScoutingClassification() {
        var analysis = analysis(List.of(
                event(600, "Lulu", "UnitDiedEvent", "Reaper", "dragonDriver", "Lulu", 20, 20)
        ));

        assertThat(detector.detect(analysis, "dragonDriver")).isEmpty();
    }

    private ReplayAnalysis analysis(List<ReplayAnalysis.TimelineEvent> timeline) {
        return new ReplayAnalysis(
                "1", "test", new ReplayAnalysis.Replay("map", "release", 1L, "2v2", 900.0, List.of("dragonDriver")),
                "dragonDriver",
                List.of(
                        new ReplayAnalysis.Player(1, "dragonDriver", "Terran", 1, "Win", null, null, List.of()),
                        new ReplayAnalysis.Player(2, "Lulu", "Zerg", 2, "Loss", null, null, List.of())
                ),
                timeline,
                null
        );
    }

    private ReplayAnalysis.TimelineEvent event(
            double time,
            Object player,
            String event,
            String unit,
            String victim,
            String killer,
            double x,
            double y
    ) {
        return new ReplayAnalysis.TimelineEvent(
                time, null, player, event, unit, null, victim, killer, null, null,
                new ReplayAnalysis.Position(x, y), null, Map.of()
        );
    }
}
