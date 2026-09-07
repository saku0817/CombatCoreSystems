package com.github.saku0817.combatcoresystems.model;

import java.util.EnumMap;
import java.util.Map;

public record ReactionDefinition(
        String id,
        String name,
        Element first,
        Element second,
        double radius,
        double cooldownSeconds,
        int hits,
        Element multiHitElement,
        double levitation,
        Map<Element, Double> components,
        Element resistanceDownElement,
        double resistanceDown,
        double resistanceDownSeconds
) {
    public ReactionDefinition {
        components = Map.copyOf(new EnumMap<>(components));
        hits = Math.max(1, hits);
    }

    public boolean matches(Element a, Element b) {
        return (first == a && second == b) || (first == b && second == a);
    }
}
