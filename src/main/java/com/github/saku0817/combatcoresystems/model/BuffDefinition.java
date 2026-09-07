package com.github.saku0817.combatcoresystems.model;

import java.util.Map;

public record BuffDefinition(
        String id,
        Kind kind,
        Target target,
        double durationSeconds,
        boolean permanent,
        int maxStacks,
        Reapply reapply,
        Map<StatKey, Double> flatModifiers,
        Map<StatKey, Double> percentModifiers,
        TickEffect tickEffect
) {
    public enum Kind { BUFF, DEBUFF }
    public enum Target { SELF, ALLY, ENEMY }
    public enum Reapply { REFRESH, STACK, OVERWRITE, CUSTOM }
    public record TickEffect(boolean healing, boolean fixed, ReferenceStat referenceStat, double multiplier,
                             Element element, double intervalSeconds, boolean critical) {}
}
