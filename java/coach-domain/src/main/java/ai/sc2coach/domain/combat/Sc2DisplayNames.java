package ai.sc2coach.domain.combat;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class Sc2DisplayNames {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("stimpack", "Stimpack"),
            Map.entry("zerglingmovementspeed", "Metabolic Boost"),
            Map.entry("overlordspeed", "Pneumatized Carapace"),
            Map.entry("centrifugalhooks", "Centrifugal Hooks"),
            Map.entry("burrow", "Burrow"),
            Map.entry("blinktech", "Blink"),
            Map.entry("charge", "Charge"),
            Map.entry("combatshield", "Combat Shield"),
            Map.entry("concussiveshells", "Concussive Shells"),
            Map.entry("terraninfantryweaponslevel1", "Terran Infantry Weapons +1"),
            Map.entry("terraninfantryweaponslevel2", "Terran Infantry Weapons +2"),
            Map.entry("terraninfantryweaponslevel3", "Terran Infantry Weapons +3"),
            Map.entry("terraninfantryarmorslevel1", "Terran Infantry Armor +1"),
            Map.entry("terraninfantryarmorslevel2", "Terran Infantry Armor +2"),
            Map.entry("terraninfantryarmorslevel3", "Terran Infantry Armor +3")
    );

    private Sc2DisplayNames() {
    }

    static Optional<String> upgrade(String internalName) {
        if (internalName == null || internalName.isBlank()) return Optional.empty();
        String key = internalName.toLowerCase(Locale.ROOT);
        if (isCosmeticOrReward(key)) return Optional.empty();
        return Optional.of(NAMES.getOrDefault(key, humanize(internalName)));
    }

    private static boolean isCosmeticOrReward(String value) {
        return value.startsWith("rewarddance")
                || value.startsWith("spray")
                || value.contains("portrait")
                || value.contains("skin")
                || value.contains("emoticon");
    }

    private static String humanize(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .trim();
    }
}
