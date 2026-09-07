package com.github.saku0817.combatcoresystems.model;

import java.util.List;
import java.util.Map;

public record WeaponDefinition(
        String id,
        String name,
        String material,
        Category category,
        int rarity,
        double attackAtLevel1,
        double attackAtLevel100,
        Element bonusElement,
        double elementBonus,
        int minimumEquipLevel,
        int maximumEquipLevel,
        Integer customModelData,
        List<String> lore,
        SkillDefinition skill,
        SkillDefinition ultimate,
        Map<Integer, Map<String, Object>> limitBreaks
) {
    public enum Category { MELEE, RANGED }

    public record SkillDefinition(String id, String name, ReferenceStat referenceStat, double multiplier,
                                  Element element, double cooldownSeconds, int charges, double radius,
                                  String target, Map<String, Object> conditions) {}
}
