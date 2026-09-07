package com.github.saku0817.combatcoresystems.util;

public final class CoreMath {
    private CoreMath() {}

    public static double linear(double start, double end, int level, int maxLevel) {
        if (maxLevel <= 1) return end;
        int clamped = Math.max(1, Math.min(maxLevel, level));
        return start + (end - start) * (clamped - 1.0) / (maxLevel - 1.0);
    }

    public static long quadraticExp(long base, long growth, int level) {
        int l = Math.max(1, level);
        long offset = l - 1L;
        return Math.max(0L, base + growth * offset * offset);
    }

    public static double effectiveDefense(double defense, double defenseDown) {
        return Math.max(0.0, defense) * (1.0 - defenseDown);
    }

    public static double defenseCoefficient(int attackerLevel, int defenderLevel, double effectiveDefense) {
        double attacker = Math.max(1, attackerLevel) + 100.0;
        double denominator = attacker + Math.max(1, defenderLevel) + 100.0 + effectiveDefense;
        return denominator <= 0 ? 0 : attacker / denominator;
    }

    public static double finalResistance(double base, double down) {
        return Math.min(1.0, base - down);
    }

    public static double cooldownSeconds(double baseSeconds, double cooldownStat) {
        return Math.max(0.0, baseSeconds * (1.0 - Math.min(1.0, cooldownStat)));
    }

    public static double heal(double reference, double multiplier, double healingPower) {
        return Math.max(0.0, reference * multiplier * (1.0 + healingPower));
    }

    public static long roundedDamage(double damage) {
        return Math.max(0L, Math.round(damage));
    }

    public static double preservedHealth(double current, double maximum) {
        return Math.min(Math.max(0.1, current), Math.max(0.1, maximum));
    }

    public static long overdamage(long damage, double currentHealth) {
        return Math.max(0L, damage - Math.round(Math.max(0, currentHealth)));
    }
}
