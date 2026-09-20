package com.github.saku0817.combatcoresystems.model;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
import java.util.random.RandomGenerator;

/** Weighted draws are performed once on acquisition; selected values are persisted in ItemInstance. */
public final class EquipmentRolls {
    private EquipmentRolls() {}
    public record Candidate(StatKey key, double first, double last, double weight) {}
    public static List<Candidate> candidates(ConfigurationSection section, boolean main) {
        if (section == null) return List.of();
        List<Candidate> result = new ArrayList<>();
        for (String raw : section.getKeys(false)) {
            StatKey key = StatKey.valueOf(raw.toUpperCase(Locale.ROOT));
            if (result.stream().anyMatch(c -> c.key() == key)) throw new IllegalArgumentException("Duplicate stat: " + raw);
            ConfigurationSection value = section.getConfigurationSection(raw);
            if (value == null) throw new IllegalArgumentException("Stat candidate must be a map: " + raw);
            double first = value.getDouble(main ? "level-1" : "value", key.name().endsWith("_FLAT") ? 25 : 0.05);
            double last = main ? value.getDouble("max-level", first) : first;
            double weight = value.getDouble("weight", 1);
            if (!Double.isFinite(first) || !Double.isFinite(last) || !Double.isFinite(weight) || weight <= 0)
                throw new IllegalArgumentException("Invalid candidate value/weight: " + raw);
            result.add(new Candidate(key, first, last, weight));
        }
        double total = result.stream().mapToDouble(Candidate::weight).sum();
        if (!Double.isFinite(total)) throw new IllegalArgumentException("Candidate weights overflow");
        return List.copyOf(result);
    }
    public static List<Candidate> draw(List<Candidate> candidates, int count, RandomGenerator random) {
        List<Candidate> pool = new ArrayList<>(candidates), result = new ArrayList<>();
        while (!pool.isEmpty() && result.size() < count) {
            double pick = random.nextDouble() * pool.stream().mapToDouble(Candidate::weight).sum();
            int index = 0;
            while (index < pool.size() - 1 && (pick -= pool.get(index).weight()) >= 0) index++;
            Candidate chosen = pool.remove(index); result.add(chosen);
            pool.removeIf(candidate -> candidate.key() == chosen.key());
        }
        return result;
    }
}
