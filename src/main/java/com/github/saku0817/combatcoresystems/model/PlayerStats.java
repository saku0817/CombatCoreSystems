package com.github.saku0817.combatcoresystems.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class PlayerStats {
    private final int level;
    private final double maxHp;
    private final double atk;
    private final double def;
    private final EnumMap<StatKey, Double> advanced;

    public PlayerStats(int level, double maxHp, double atk, double def, Map<StatKey, Double> advanced) {
        this.level = level;
        this.maxHp = Math.max(0.0, maxHp);
        this.atk = Math.max(0.0, atk);
        this.def = Math.max(0.0, def);
        this.advanced = new EnumMap<>(StatKey.class);
        this.advanced.putAll(advanced);
    }

    public int level() { return level; }
    public double maxHp() { return maxHp; }
    public double atk() { return atk; }
    public double def() { return def; }
    public double value(StatKey key) { return advanced.getOrDefault(key, 0.0); }
    public Map<StatKey, Double> advanced() { return Collections.unmodifiableMap(advanced); }

    public double elementDamage(Element element) {
        return switch (element) {
            case FIRE -> value(StatKey.FIRE_DAMAGE);
            case WATER -> value(StatKey.WATER_DAMAGE);
            case WIND -> value(StatKey.WIND_DAMAGE);
            case THUNDER -> value(StatKey.THUNDER_DAMAGE);
            case MOON -> value(StatKey.MOON_DAMAGE);
            case PHYSICAL -> 0.0;
        };
    }

    public double resistance(Element element) {
        return switch (element) {
            case FIRE -> value(StatKey.FIRE_RESISTANCE);
            case WATER -> value(StatKey.WATER_RESISTANCE);
            case WIND -> value(StatKey.WIND_RESISTANCE);
            case THUNDER -> value(StatKey.THUNDER_RESISTANCE);
            case MOON -> value(StatKey.MOON_RESISTANCE);
            case PHYSICAL -> 0.0;
        };
    }

    public double resistanceDown(Element element) {
        return switch (element) {
            case FIRE -> value(StatKey.FIRE_RESISTANCE_DOWN);
            case WATER -> value(StatKey.WATER_RESISTANCE_DOWN);
            case WIND -> value(StatKey.WIND_RESISTANCE_DOWN);
            case THUNDER -> value(StatKey.THUNDER_RESISTANCE_DOWN);
            case MOON -> value(StatKey.MOON_RESISTANCE_DOWN);
            case PHYSICAL -> 0.0;
        };
    }

    public double resistanceIgnore(Element element) {
        return switch (element) {
            case FIRE -> value(StatKey.FIRE_RESISTANCE_IGNORE);
            case WATER -> value(StatKey.WATER_RESISTANCE_IGNORE);
            case WIND -> value(StatKey.WIND_RESISTANCE_IGNORE);
            case THUNDER -> value(StatKey.THUNDER_RESISTANCE_IGNORE);
            case MOON -> value(StatKey.MOON_RESISTANCE_IGNORE);
            case PHYSICAL -> 0.0;
        };
    }
}
