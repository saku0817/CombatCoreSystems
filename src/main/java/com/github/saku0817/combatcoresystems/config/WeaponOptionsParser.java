package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.*;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Strict optional fields: invalid combat definitions fail reload instead of failing on use. */
final class WeaponOptionsParser {
    private WeaponOptionsParser() {}
    static WeaponOptions weapon(ConfigurationSection s) {
        var t = s.getConfigurationSection("talent");
        WeaponOptions.Talent talent = null;
        if (t != null) {
            Map<StatKey, Double> modifiers = new EnumMap<>(StatKey.class);
            var values = t.getConfigurationSection("modifiers");
            if (values != null) for (String key : values.getKeys(false))
                modifiers.put(StatKey.valueOf(key.toUpperCase(Locale.ROOT)), finite(values, key, 0));
            talent = new WeaponOptions.Talent(t.getString("name", "天賦"), description(t),
                    WeaponOptions.Hand.valueOf(t.getString("hand", "MAIN_HAND").toUpperCase(Locale.ROOT)),
                    nonnegative(t, "multiplier", 1), Map.copyOf(modifiers));
        }
        return new WeaponOptions(element(s.getString("normal-attack.attribute", "PHYSICAL")), talent,
                visual(s.getConfigurationSection("normal-attack.visual")));
    }
    static WeaponOptions.Ability ability(ConfigurationSection s) {
        nonnegative(s, "multiplier", 1);
        nonnegative(s, "cooldown-seconds", s.getDouble("cooldown", 0));
        nonnegative(s, "radius", 0);
        double hpCondition = nonnegative(s, "conditions.min-hp-percent", 0);
        if (hpCondition > 1) throw new IllegalArgumentException("conditions.min-hp-percent must be 0..1");
        nonnegative(s, "conditions.max-distance", 0);
        List<WeaponOptions.Component> components = new ArrayList<>();
        for (Map<?, ?> raw : s.getMapList("damage-components")) {
            var c = new org.bukkit.configuration.MemoryConfiguration();
            raw.forEach((key, value) -> c.set(String.valueOf(key), value));
            components.add(new WeaponOptions.Component(ReferenceStat.valueOf(c.getString("reference", "ATK").toUpperCase(Locale.ROOT)),
                    nonnegative(c, "multiplier", 1), element(c.getString("bonus-attribute", "PHYSICAL"))));
        }
        if (s.contains("damage-components") && (!s.isList("damage-components") || components.isEmpty()))
            throw new IllegalArgumentException("damage-components must be a nonempty list");
        double cost = nonnegative(s, "cost.current-hp-percent", 0);
        if (cost >= 1) throw new IllegalArgumentException("cost.current-hp-percent must be less than 1");
        return new WeaponOptions.Ability(description(s), s.getBoolean("damage-enabled", !s.contains("actions")), cost,
                List.copyOf(components), s.getStringList("self-effects"), s.getStringList("target-effects"),
                visual(s.getConfigurationSection("visual")));
    }
    private static List<String> description(ConfigurationSection s) {
        return s.isList("description") ? List.copyOf(s.getStringList("description"))
                : s.contains("description") ? List.of(s.getString("description", "")) : List.of();
    }
    private static Element element(String value) {
        return Element.parse(value).orElseThrow(() -> new IllegalArgumentException("invalid attribute: " + value));
    }
    private static double finite(ConfigurationSection s, String key, double fallback) {
        if (s.contains(key) && !(s.get(key) instanceof Number)) throw new IllegalArgumentException(key + " must be numeric");
        double value = s.getDouble(key, fallback);
        if (!Double.isFinite(value)) throw new IllegalArgumentException(key + " must be finite");
        return value;
    }
    private static double nonnegative(ConfigurationSection s, String key, double fallback) {
        double value = finite(s, key, fallback);
        if (value < 0) throw new IllegalArgumentException(key + " must be nonnegative");
        return value;
    }
    private static WeaponOptions.Visual visual(ConfigurationSection s) {
        if (s == null) return WeaponOptions.Visual.NONE;
        String particle = s.getString("particle", "").toUpperCase(Locale.ROOT);
        if (!particle.isBlank() && Particle.valueOf(particle).getDataType() != Void.class)
            throw new IllegalArgumentException("visual particle requires additional data; use a data-free particle");
        String sound = s.getString("sound", "");
        if (!sound.isBlank() && !sound.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+"))
            throw new IllegalArgumentException("visual.sound must be a namespaced sound key");
        int count = s.getInt("count", 20);
        double spread = nonnegative(s, "spread", .5), volume = nonnegative(s, "volume", 1), pitch = nonnegative(s, "pitch", 1);
        if (count < 0 || count > 500 || spread > 16 || volume > 4 || pitch > 2)
            throw new IllegalArgumentException("visual exceeds limits: count 0..500, spread 0..16, volume 0..4, pitch 0..2");
        return new WeaponOptions.Visual(particle, count, spread, sound, (float) volume, (float) pitch);
    }
}
