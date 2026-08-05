package ai.sc2coach.domain.information;

import java.util.List;

public record InformationReport(
        List<InformationEpisode> episodes,
        List<InformationState> states,
        InformationAdvantage advantage,
        List<InformationNarrative> narratives
) {
    public InformationReport {
        episodes = episodes == null ? List.of() : List.copyOf(episodes);
        states = states == null ? List.of() : List.copyOf(states);
        advantage = advantage == null ? new InformationAdvantage(states) : advantage;
        narratives = narratives == null ? List.of() : List.copyOf(narratives);
    }
}
