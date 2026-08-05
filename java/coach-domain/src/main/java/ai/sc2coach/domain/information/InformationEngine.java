package ai.sc2coach.domain.information;

import ai.sc2coach.domain.ReplayAnalysis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class InformationEngine {

    private static final double ENEMY_AREA_RADIUS = 36.0;
    private static final double POTENTIAL_VISION_RADIUS = 13.0;
    private static final double MAX_CONTACT_GAP_SECONDS = 20.0;
    private static final double MAX_EPISODE_SECONDS = 120.0;
    private static final double RESPONSE_WINDOW_SECONDS = 90.0;
    private static final double SHORT_SCOUT_SECONDS = 15.0;

    private static final Set<String> DEFAULT_SCOUT_UNITS = Set.of(
            "reaper", "scv", "probe", "adept", "observer", "hallucinatedphoenix",
            "hallucinatedoracle", "overlord", "overseer", "changeling", "drone", "zergling"
    );

    private final Set<String> scoutUnits;

    public InformationEngine() {
        this(DEFAULT_SCOUT_UNITS);
    }

    public InformationEngine(Set<String> scoutUnits) {
        this.scoutUnits = scoutUnits == null
                ? DEFAULT_SCOUT_UNITS
                : scoutUnits.stream().map(InformationEngine::normalized).collect(Collectors.toUnmodifiableSet());
    }

    public InformationReport analyze(ReplayAnalysis analysis) {
        if (analysis == null) return new InformationReport(List.of(), List.of(), null, List.of());

        List<EventView> events = analysis.timeline().stream()
                .map(event -> new EventView(analysis, event))
                .sorted(Comparator.comparingDouble(EventView::time))
                .toList();
        List<EventView> enemyAnchors = events.stream()
                .filter(EventView::isPotentialInformation)
                .filter(event -> event.position() != null)
                .toList();

        List<InformationEpisode> episodes = detectEpisodes(analysis, events, enemyAnchors);
        List<InformationState> states = informationStates(analysis, episodes);
        List<InformationNarrative> narratives = episodes.stream()
                .map(InformationNarrative::from)
                .toList();
        return new InformationReport(episodes, states, new InformationAdvantage(states), narratives);
    }

    private List<InformationEpisode> detectEpisodes(
            ReplayAnalysis analysis,
            List<EventView> events,
            List<EventView> enemyAnchors
    ) {
        var scoutSamples = events.stream()
                .filter(event -> isScout(event.unit()))
                .filter(event -> !event.isDeath())
                .filter(event -> event.owner() != null)
                .filter(event -> event.position() != null)
                .filter(event -> nearOpponentAnchor(event, enemyAnchors))
                .toList();
        if (scoutSamples.isEmpty()) return List.of();

        List<InformationEpisode> episodes = new ArrayList<>();
        Set<EventView> consumed = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

        for (EventView seed : scoutSamples) {
            if (consumed.contains(seed)) continue;

            List<EventView> contact = scoutSamples.stream()
                    .filter(sample -> !consumed.contains(sample))
                    .filter(sample -> sameScout(seed, sample))
                    .filter(sample -> sample.time() >= seed.time())
                    .takeWhile(new ContactContinuation(seed))
                    .toList();
            contact.forEach(consumed::add);
            if (contact.isEmpty()) contact = List.of(seed);

            EventView lastContact = contact.getLast();
            Optional<EventView> death = firstScoutDeath(events, seed);
            double start = seed.time();
            double end = death.map(EventView::time).orElse(lastContact.time() + MAX_CONTACT_GAP_SECONDS);
            boolean survived = death.isEmpty();
            List<EventView> positions = new ArrayList<>(contact);
            death.ifPresent(positions::add);

            Target target = targetFor(seed, positions, enemyAnchors);
            List<InformationObservation> potentiallyObserved = potentiallyObserved(
                    analysis, target, seed.owner(), events, positions, start, end
            );
            List<InformationGap> missingInformation = missingInformation(end - start, survived, potentiallyObserved);
            List<InformationReaction> reactionCandidates = reactionCandidates(
                    seed.owner(), events, end, potentiallyObserved
            );

            episodes.add(new InformationEpisode(
                    seed.owner(),
                    target.player(),
                    target.team(),
                    seed.unit(),
                    seconds(start),
                    seconds(end),
                    survived,
                    confidence(end - start, survived, potentiallyObserved, missingInformation, positions),
                    potentiallyObserved,
                    missingInformation,
                    reactionCandidates
            ));
        }

        return episodes.stream()
                .sorted(Comparator.comparing(InformationEpisode::start))
                .toList();
    }

    private List<InformationObservation> potentiallyObserved(
            ReplayAnalysis analysis,
            Target target,
            String scoutPlayer,
            List<EventView> events,
            List<EventView> scoutPositions,
            double start,
            double end
    ) {
        if (scoutPositions.isEmpty()) return List.of();

        Map<String, InformationObservation> observations = new LinkedHashMap<>();
        events.stream()
                .filter(event -> event.time() >= start && event.time() <= end)
                .filter(EventView::isPotentialInformation)
                .filter(event -> event.position() != null)
                .filter(event -> event.owner() != null && !event.owner().equalsIgnoreCase(scoutPlayer))
                .filter(event -> target.player() == null || event.owner().equalsIgnoreCase(target.player()))
                .forEach(event -> nearestDistance(event, scoutPositions).ifPresent(distance -> {
                    if (distance > POTENTIAL_VISION_RADIUS) return;
                    String key = event.informationType() + ":" + event.subject();
                    observations.putIfAbsent(key, new InformationObservation(
                            event.informationType(),
                            event.subject(),
                            seconds(event.time()),
                            event.position(),
                            distance,
                            observationConfidence(distance, analysis, event)
                    ));
                }));
        return observations.values().stream().limit(16).toList();
    }

    private List<InformationGap> missingInformation(
            double durationSeconds,
            boolean survived,
            List<InformationObservation> observations
    ) {
        Set<String> categories = observations.stream()
                .map(InformationObservation::type)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<InformationGap> gaps = new ArrayList<>();
        if (!survived && durationSeconds <= SHORT_SCOUT_SECONDS) {
            gaps.add(gap("Main Tech", "scout ended before a stable technology read was likely"));
            gaps.add(gap("Army Composition", "scout died too early for a reliable army sample"));
        }
        if (!categories.contains("TECHNOLOGY")) gaps.add(gap("Tech Structure", "no nearby technology structure was sampled"));
        if (!categories.contains("ECONOMY")) {
            gaps.add(gap("Second Gas", "gas timing was not covered by the scouting contact"));
            gaps.add(gap("Third Base", "expansion state was not covered by the scouting contact"));
        }
        if (!categories.contains("ARMY")) gaps.add(gap("Army Composition", "no nearby army-unit sample was available"));
        return gaps.stream()
                .collect(Collectors.toMap(InformationGap::topic, gap -> gap, (first, ignored) -> first, LinkedHashMap::new))
                .values()
                .stream()
                .limit(6)
                .toList();
    }

    private List<InformationReaction> reactionCandidates(
            String player,
            List<EventView> events,
            double scoutingEnded,
            List<InformationObservation> observations
    ) {
        if (observations.isEmpty()) return List.of();
        return events.stream()
                .filter(event -> event.time() > scoutingEnded)
                .filter(event -> event.time() <= scoutingEnded + RESPONSE_WINDOW_SECONDS)
                .filter(event -> player.equalsIgnoreCase(event.owner()))
                .filter(EventView::isDecisionCandidate)
                .map(event -> responseCandidate(event, observations, scoutingEnded))
                .distinct()
                .limit(8)
                .toList();
    }

    private InformationReaction responseCandidate(
            EventView event,
            List<InformationObservation> observations,
            double scoutingEnded
    ) {
        Match match = bestMatch(event, observations);
        double confidence = match.matched() ? 0.82 : 0.56;
        if (event.isUpgrade()) confidence += 0.04;
        return new InformationReaction(
                event.owner(),
                action(event),
                seconds(event.time()),
                Math.round(event.time() - scoutingEnded),
                match.basis(),
                InformationConfidence.of(Math.min(0.92, confidence),
                        match.matched() ? "matches a potentially observed tech path" : "timing-only response candidate")
        );
    }

    private Match bestMatch(EventView event, List<InformationObservation> observations) {
        String action = normalized(event.subject());
        for (InformationObservation observation : observations) {
            String subject = normalized(observation.subject());
            if (subject.contains("roachwarren") && action.contains("bunker")) {
                return new Match(true, "Potentially Observed Roach Warren");
            }
            if (subject.contains("roboticsfacility")
                    && (action.contains("missileturret") || action.contains("viking"))) {
                return new Match(true, "Potentially Observed Robotics Facility");
            }
            if (subject.contains("spire") && (action.contains("missileturret") || action.contains("viking"))) {
                return new Match(true, "Potentially Observed Spire");
            }
        }
        return new Match(false, "post-scouting timing window");
    }

    private List<InformationState> informationStates(ReplayAnalysis analysis, List<InformationEpisode> episodes) {
        return analysis.players().stream()
                .map(player -> informationState(player.name(), episodes))
                .toList();
    }

    private InformationState informationState(String player, List<InformationEpisode> episodes) {
        List<InformationObservation> observations = episodes.stream()
                .filter(episode -> player.equalsIgnoreCase(episode.scoutPlayer()))
                .flatMap(episode -> episode.potentiallyObserved().stream())
                .toList();
        return new InformationState(player, List.of(
                stateEntry("army tech", observations, "TECHNOLOGY"),
                stateEntry("economy", observations, "ECONOMY"),
                stateEntry("expansions", observations, "ECONOMY"),
                stateEntry("upgrades", observations, "UPGRADE"),
                stateEntry("army", observations, "ARMY")
        ));
    }

    private InformationState.Entry stateEntry(
            String topic,
            List<InformationObservation> observations,
            String category
    ) {
        List<String> evidence = observations.stream()
                .filter(observation -> category.equals(observation.type()))
                .map(InformationObservation::subject)
                .distinct()
                .toList();
        return new InformationState.Entry(
                topic,
                evidence.isEmpty()
                        ? InformationState.Knowledge.UNKNOWN
                        : InformationState.Knowledge.POTENTIALLY_KNOWN,
                evidence
        );
    }

    private boolean nearOpponentAnchor(EventView scout, List<EventView> enemyAnchors) {
        return enemyAnchors.stream()
                .filter(anchor -> anchor.owner() != null && !anchor.owner().equalsIgnoreCase(scout.owner()))
                .filter(anchor -> anchor.position() != null)
                .anyMatch(anchor -> anchor.position().distanceTo(scout.position()) <= ENEMY_AREA_RADIUS);
    }

    private Target targetFor(EventView seed, List<EventView> positions, List<EventView> enemyAnchors) {
        return enemyAnchors.stream()
                .filter(anchor -> anchor.owner() != null && !anchor.owner().equalsIgnoreCase(seed.owner()))
                .filter(anchor -> anchor.position() != null)
                .min(Comparator.comparingDouble(anchor -> nearestDistance(anchor, positions).orElse(Double.MAX_VALUE)))
                .map(anchor -> new Target(anchor.owner(), anchor.team()))
                .orElse(new Target(null, null));
    }

    private Optional<EventView> firstScoutDeath(List<EventView> events, EventView seed) {
        return events.stream()
                .filter(EventView::isDeath)
                .filter(event -> event.time() >= seed.time())
                .filter(event -> event.time() <= seed.time() + MAX_EPISODE_SECONDS)
                .filter(event -> seed.owner().equalsIgnoreCase(event.victimOwner()))
                .filter(event -> normalized(seed.unit()).equals(normalized(event.unit())))
                .findFirst();
    }

    private Optional<Double> nearestDistance(EventView event, List<EventView> positions) {
        if (event.position() == null || positions.isEmpty()) return Optional.empty();
        return positions.stream()
                .map(EventView::position)
                .filter(Objects::nonNull)
                .mapToDouble(position -> position.distanceTo(event.position()))
                .min()
                .stream()
                .boxed()
                .findFirst();
    }

    private InformationConfidence observationConfidence(double distance, ReplayAnalysis analysis, EventView event) {
        double value = distance <= 6.0 ? 0.86 : 0.68;
        if (event.isUpgrade()) value += 0.04;
        if (event.team() == null && teamOf(analysis, event.owner()) == null) value -= 0.08;
        return InformationConfidence.of(value, "near scout path", "replay has coordinates but no vision log");
    }

    private InformationConfidence confidence(
            double durationSeconds,
            boolean survived,
            List<InformationObservation> observations,
            List<InformationGap> gaps,
            List<EventView> positions
    ) {
        double value = 0.42;
        List<String> factors = new ArrayList<>();
        if (survived) {
            value += 0.22;
            factors.add("scout survived contact");
        } else {
            factors.add("scout death bounds the episode");
        }
        if (durationSeconds >= 30) {
            value += 0.14;
            factors.add("contact lasted long enough for a broader read");
        }
        if (!observations.isEmpty()) {
            value += 0.18;
            factors.add("nearby potentially observed objects");
        }
        if (!positions.isEmpty()) {
            value += 0.08;
            factors.add("scout coordinates available");
        }
        if (!survived && durationSeconds <= SHORT_SCOUT_SECONDS) {
            value -= 0.22;
            factors.add("short scout death lowers confidence");
        }
        if (!gaps.isEmpty()) {
            value -= Math.min(0.16, gaps.size() * 0.03);
            factors.add("missing information remains");
        }
        return new InformationConfidence(value, factors);
    }

    private boolean sameScout(EventView left, EventView right) {
        return left.owner().equalsIgnoreCase(right.owner())
                && normalized(left.unit()).equals(normalized(right.unit()));
    }

    private boolean isScout(String unit) {
        return scoutUnits.contains(normalized(unit));
    }

    private InformationGap gap(String topic, String reason) {
        return new InformationGap(topic, reason, InformationConfidence.of(0.72, "limited scouting coverage"));
    }

    private String action(EventView event) {
        if (event.isUpgrade()) return "Research " + event.subject();
        return "Build " + event.subject();
    }

    private static Duration seconds(double seconds) {
        return Duration.ofMillis(Math.round(seconds * 1000));
    }

    private Integer teamOf(ReplayAnalysis analysis, String player) {
        if (player == null) return null;
        return analysis.players().stream()
                .filter(candidate -> player.equalsIgnoreCase(candidate.name()))
                .map(ReplayAnalysis.Player::team)
                .findFirst()
                .orElse(null);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private record Target(String player, Integer team) {}

    private record Match(boolean matched, String basis) {}

    private static final class ContactContinuation implements java.util.function.Predicate<EventView> {
        private double previous;
        private boolean first = true;

        private ContactContinuation(EventView seed) {
            this.previous = seed.time();
        }

        @Override
        public boolean test(EventView sample) {
            if (first) {
                first = false;
                previous = sample.time();
                return true;
            }
            if (sample.time() - previous > MAX_CONTACT_GAP_SECONDS) return false;
            previous = sample.time();
            return true;
        }
    }

    private final class EventView {
        private final ReplayAnalysis analysis;
        private final ReplayAnalysis.TimelineEvent event;

        private EventView(ReplayAnalysis analysis, ReplayAnalysis.TimelineEvent event) {
            this.analysis = analysis;
            this.event = event;
        }

        private double time() {
            return event.time() == null ? 0.0 : event.time();
        }

        private String owner() {
            if (isDeath()) return victimOwner();
            return playerName(event.player());
        }

        private String victimOwner() {
            if (event.victim() != null && knownPlayer(event.victim())) return playerName(event.victim());
            Object owner = event.attributes() == null ? null : event.attributes().get("owner");
            if (owner != null) return playerName(owner);
            return null;
        }

        private Integer team() {
            return teamOf(analysis, owner());
        }

        private String unit() {
            return event.unit();
        }

        private InformationPoint position() {
            ReplayAnalysis.Position position = event.position() != null ? event.position() : event.targetPosition();
            if (position == null || position.x() == null || position.y() == null) return null;
            return new InformationPoint(position.x(), position.y());
        }

        private boolean isDeath() {
            return lower(event.event()).contains("died");
        }

        private boolean isUpgrade() {
            return event.upgrade() != null && !event.upgrade().isBlank();
        }

        private boolean isPotentialInformation() {
            return isUpgrade()
                    || (event.unit() != null && (isLifecycleEvent() || isDeath()));
        }

        private boolean isDecisionCandidate() {
            return isUpgrade()
                    || (event.unit() != null && isLifecycleEvent() && !isScout(event.unit()));
        }

        private boolean isLifecycleEvent() {
            String name = lower(event.event());
            return name.contains("born") || name.contains("finished") || name.contains("init");
        }

        private String informationType() {
            if (isUpgrade()) return "UPGRADE";
            if (isEconomy(unit())) return "ECONOMY";
            if (isTechnology(unit())) return "TECHNOLOGY";
            return "ARMY";
        }

        private String subject() {
            return isUpgrade() ? event.upgrade() : event.unit();
        }

        private String playerName(Object value) {
            if (value == null) return null;
            String raw = String.valueOf(value);
            return analysis.players().stream()
                    .filter(player -> raw.equalsIgnoreCase(player.name()) || raw.equals(String.valueOf(player.pid())))
                    .map(ReplayAnalysis.Player::name)
                    .findFirst()
                    .orElse(raw);
        }

        private boolean knownPlayer(String value) {
            return value != null && analysis.players().stream()
                    .anyMatch(player -> value.equalsIgnoreCase(player.name()));
        }

        private boolean isEconomy(String unit) {
            String value = normalized(unit);
            return value.contains("commandcenter") || value.contains("orbitalcommand")
                    || value.contains("planetaryfortress") || value.contains("nexus")
                    || value.contains("hatchery") || value.contains("lair") || value.contains("hive")
                    || value.contains("refinery") || value.contains("assimilator") || value.contains("extractor");
        }

        private boolean isTechnology(String unit) {
            String value = normalized(unit);
            return value.contains("roachwarren") || value.contains("banelingnest")
                    || value.contains("spawningpool") || value.contains("hydraliskden")
                    || value.contains("spire") || value.contains("infestationpit")
                    || value.contains("ultraliskcavern") || value.contains("lair") || value.contains("hive")
                    || value.contains("twilightcouncil") || value.contains("roboticsfacility")
                    || value.contains("roboticsbay") || value.contains("stargate")
                    || value.contains("templararchive") || value.contains("darkshrine")
                    || value.contains("cyberneticscore") || value.contains("forge")
                    || value.contains("factory") || value.contains("starport")
                    || value.contains("techlab") || value.contains("reactor")
                    || value.contains("engineeringbay") || value.contains("armory")
                    || value.contains("fusioncore") || value.contains("ghostacademy");
        }

        private String lower(String value) {
            return value == null ? "" : value.toLowerCase(Locale.ROOT);
        }
    }
}
