package ai.sc2coach.domain.coach;

import java.time.Duration;
import java.util.List;

public record CoachFeed(
        String headline,
        List<Card> cards,
        List<String> nextGameRecommendations
) {
    public CoachFeed {
        headline = headline == null ? "Матч проанализирован" : headline;
        cards = cards == null ? List.of() : List.copyOf(cards);
        nextGameRecommendations = nextGameRecommendations == null ? List.of() : List.copyOf(nextGameRecommendations);
    }

    public record Card(
            Duration at,
            Kind kind,
            Impact impact,
            String title,
            String explanation,
            double confidence
    ) {
        public Card {
            at = at == null ? Duration.ZERO : at;
            kind = kind == null ? Kind.INFO : kind;
            impact = impact == null ? Impact.MEDIUM : impact;
            title = title == null ? "Наблюдение" : title;
            explanation = explanation == null ? "" : explanation;
            confidence = Math.max(0, Math.min(1, confidence));
        }
    }

    public enum Kind { GOOD, RISKY, POOR, INFO }
    public enum Impact { LOW, MEDIUM, HIGH, GAME_CHANGING }
}
