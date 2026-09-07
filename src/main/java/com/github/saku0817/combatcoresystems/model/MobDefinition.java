package com.github.saku0817.combatcoresystems.model;

import java.util.Map;
import java.util.Set;

public record MobDefinition(
        String id,
        String name,
        String entityType,
        boolean boss,
        int minLevel,
        int maxLevel,
        double hpAtMin,
        double hpAtMax,
        double atkAtMin,
        double atkAtMax,
        double defAtMin,
        double defAtMax,
        Element nativeElement,
        Set<Element> immunities,
        Map<Element, Double> resistances,
        long exp,
        boolean dropCustomExp,
        boolean showLevel,
        boolean vanilla
) {}
