package com.github.saku0817.combatcoresystems.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ItemInstance {
    private String instanceId = UUID.randomUUID().toString();
    private String definitionId = "";
    private int level = 1;
    private long exp;
    private int limitBreak;
    private Map<String, Double> substats = new LinkedHashMap<>();
    private Map<String, Integer> substatUpgrades = new LinkedHashMap<>();
    private int unlockedSubstats;
    private StatKey mainStat;
    private double mainAtLevel1;
    private double mainAtMaxLevel;

    public StatKey mainStat(EquipmentDefinition definition) { return mainStat == null ? definition.mainStat() : mainStat; }
    public double mainValue(EquipmentDefinition definition) {
        return com.github.saku0817.combatcoresystems.util.CoreMath.linear(mainStat == null ? definition.mainAtLevel1() : mainAtLevel1,
                mainStat == null ? definition.mainAtMaxLevel() : mainAtMaxLevel, level, definition.maxLevel());
    }
    public void setMainStat(StatKey key, double first, double last) {
        if (key == null || !Double.isFinite(first) || !Double.isFinite(last)) throw new IllegalArgumentException("Invalid main stat");
        mainStat = key; mainAtLevel1 = first; mainAtMaxLevel = last;
    }

    public String getInstanceId() { return instanceId; }
    public String getDefinitionId() { return definitionId; }
    public int getLevel() { return level; }
    public long getExp() { return exp; }
    public int getLimitBreak() { return limitBreak; }
    public Map<String, Double> getSubstats() { return substats; }
    public Map<String, Integer> getSubstatUpgrades() { return substatUpgrades; }
    public int getUnlockedSubstats() { return unlockedSubstats; }
    public void setDefinitionId(String definitionId) { this.definitionId = definitionId; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(100, level)); }
    public void setExp(long exp) { this.exp = Math.max(0L, exp); }
    public void setLimitBreak(int limitBreak) { this.limitBreak = Math.max(0, Math.min(5, limitBreak)); }
    public void setUnlockedSubstats(int count) { this.unlockedSubstats = Math.max(0, Math.min(4, count)); }
}
