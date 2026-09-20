package com.github.saku0817.combatcoresystems.model;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public record SetTrigger(String id, Event event, double hpBelow, long cooldownMillis, Target target, List<String> effects) {
    public enum Event { SKILL, ULTIMATE, HIT, TAKE_DAMAGE, HP_BELOW }
    public enum Target { SELF, OTHER }
    public static List<SetTrigger> parse(ConfigurationSection section, Set<String> knownBuffs) {
        if (section == null) return List.of();
        List<SetTrigger> result = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) throw new IllegalArgumentException("Set trigger must be a map: " + id);
            Event event = Event.valueOf(s.getString("event", "").toUpperCase(Locale.ROOT));
            Target target = Target.valueOf(s.getString("target", "SELF").toUpperCase(Locale.ROOT));
            double hp = s.getDouble("hp-percent", 0.5), cooldown = s.getDouble("cooldown-seconds", 1);
            if (!Double.isFinite(hp) || hp < 0 || hp > 1 || !Double.isFinite(cooldown) || cooldown < 0 || cooldown > 86400)
                throw new IllegalArgumentException("Invalid set trigger HP/cooldown: " + id);
            if (event == Event.HP_BELOW && target != Target.SELF) throw new IllegalArgumentException("HP_BELOW requires SELF");
            List<String> effects = s.getStringList("effects");
            if (effects.isEmpty() || !knownBuffs.containsAll(effects)) throw new IllegalArgumentException("Unknown/empty buffs.yml effects: " + id);
            result.add(new SetTrigger(id, event, hp, Math.round(cooldown * 1000), target, List.copyOf(effects)));
        }
        return List.copyOf(result);
    }
}
