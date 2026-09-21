package com.github.saku0817.combatcoresystems.model.trigger;

import com.github.saku0817.combatcoresystems.model.StatKey;
import java.util.EnumMap;
import java.util.Map;

/** Per-calculation modifiers. Never mutates cached PlayerStats. Last override wins. */
public final class EventModifier {
    private final Map<StatKey, Double> flat = new EnumMap<>(StatKey.class);
    private final Map<StatKey, Double> percent = new EnumMap<>(StatKey.class);
    private final Map<StatKey, Double> override = new EnumMap<>(StatKey.class);
    public void add(String mode, StatKey key, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite modifier");
        switch (mode) {
            case "flat" -> flat.merge(key, value, Double::sum);
            case "percent" -> percent.merge(key, value, Double::sum);
            case "override" -> override.put(key, value);
            default -> throw new IllegalArgumentException("Unknown modifier mode: " + mode);
        }
    }
    public double advanced(StatKey key, double base) {
        // CCS advanced stats are additive ratios, including modifiers.percent.CRIT_RATE.
        return override.getOrDefault(key, base + flat.getOrDefault(key, 0.0) + percent.getOrDefault(key, 0.0));
    }
    public double primary(StatKey flatKey, StatKey percentKey, double base) {
        double ratio=override.getOrDefault(percentKey,flat.getOrDefault(percentKey, 0.0) + percent.getOrDefault(percentKey, 0.0) + percent.getOrDefault(flatKey, 0.0));
        double value = (base + flat.getOrDefault(flatKey, 0.0)) * (1+ratio);
        return override.getOrDefault(flatKey, value);
    }
}
