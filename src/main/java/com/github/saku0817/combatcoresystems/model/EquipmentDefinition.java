package com.github.saku0817.combatcoresystems.model;

import java.util.List;

public record EquipmentDefinition(
        String id,
        String name,
        String material,
        EquipmentSlot slot,
        int rarity,
        int maxLevel,
        StatKey mainStat,
        double mainAtLevel1,
        double mainAtMaxLevel,
        List<StatKey> substatCandidates,
        String setId,
        Integer customModelData,
        List<String> lore
) {}
