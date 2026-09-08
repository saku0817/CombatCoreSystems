package com.github.saku0817.combatcoresystems.model;

import java.util.*;

public final class PlayerData {
    public static final int DATA_VERSION = 2;

    private int dataVersion = DATA_VERSION;
    private String uuid = "";
    private String lastName = "";
    private int level = 1;
    private long exp;
    private int rebirthCount;
    private double health = 20.0;
    private int skillPoints;
    private Set<Integer> grantedLevelPoints = new HashSet<>();
    private Map<String, Integer> skillNodes = new LinkedHashMap<>();
    private Map<Integer, Map<String, Integer>> skillPresets = new LinkedHashMap<>();
    private int activePreset = 1;
    private long presetCooldownEnd;
    private EnumMap<EquipmentSlot, ItemInstance> equipment = new EnumMap<>(EquipmentSlot.class);
    private boolean pvpEnabled = true;
    private boolean hudEnabled = true;
    private String controls = "DEFAULT";
    private int combatLogoutCount;
    private List<TimedEffect> buffs = new ArrayList<>();
    private List<TimedEffect> debuffs = new ArrayList<>();
    private List<ElementAttachment> elements = new ArrayList<>();
    private Set<String> discoveredMobs = new HashSet<>();
    private Set<String> discoveredBosses = new HashSet<>();
    private String partyId = "";
    private long lastSaveEpochMillis;
    private Map<String, Long> enhancementMaterials = new LinkedHashMap<>();

    public PlayerData() {}

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid.toString();
        this.lastName = name;
    }

    public int getDataVersion() { return dataVersion; }
    public UUID uuid() { return UUID.fromString(uuid); }
    public String getUuid() { return uuid; }
    public String getLastName() { return lastName; }
    public int getLevel() { return level; }
    public long getExp() { return exp; }
    public int getRebirthCount() { return rebirthCount; }
    public double getHealth() { return health; }
    public int getSkillPoints() { return skillPoints; }
    public Set<Integer> getGrantedLevelPoints() { return grantedLevelPoints; }
    public Map<String, Integer> getSkillNodes() { return skillNodes; }
    public Map<Integer, Map<String, Integer>> getSkillPresets() { return skillPresets; }
    public int getActivePreset() { return activePreset; }
    public long getPresetCooldownEnd() { return presetCooldownEnd; }
    public EnumMap<EquipmentSlot, ItemInstance> getEquipment() { return equipment; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public boolean isHudEnabled() { return hudEnabled; }
    public String getControls() { return controls; }
    public int getCombatLogoutCount() { return combatLogoutCount; }
    public List<TimedEffect> getBuffs() { return buffs; }
    public List<TimedEffect> getDebuffs() { return debuffs; }
    public List<ElementAttachment> getElements() { return elements; }
    public Set<String> getDiscoveredMobs() { return discoveredMobs; }
    public Set<String> getDiscoveredBosses() { return discoveredBosses; }
    public String getPartyId() { return partyId; }
    public long getLastSaveEpochMillis() { return lastSaveEpochMillis; }
    public Map<String, Long> getEnhancementMaterials() { return enhancementMaterials; }

    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(100, level)); }
    public void setExp(long exp) { this.exp = Math.max(0, exp); }
    public void setRebirthCount(int rebirthCount) { this.rebirthCount = Math.max(0, rebirthCount); }
    public void setHealth(double health) { this.health = Math.max(0, health); }
    public void setSkillPoints(int skillPoints) { this.skillPoints = Math.max(0, skillPoints); }
    public void setActivePreset(int activePreset) { this.activePreset = Math.max(1, Math.min(5, activePreset)); }
    public void setPresetCooldownEnd(long value) { this.presetCooldownEnd = Math.max(0, value); }
    public void setPvpEnabled(boolean value) { this.pvpEnabled = value; }
    public void setHudEnabled(boolean value) { this.hudEnabled = value; }
    public void setControls(String value) { this.controls = value == null ? "DEFAULT" : value; }
    public void setCombatLogoutCount(int value) { this.combatLogoutCount = Math.max(0, value); }
    public void setPartyId(String partyId) { this.partyId = partyId == null ? "" : partyId; }
    public void setLastSaveEpochMillis(long value) { this.lastSaveEpochMillis = value; }

    public PlayerData normalize() {
        if (grantedLevelPoints == null) grantedLevelPoints = new HashSet<>();
        if (skillNodes == null) skillNodes = new LinkedHashMap<>();
        if (skillPresets == null) skillPresets = new LinkedHashMap<>();
        if (equipment == null) equipment = new EnumMap<>(EquipmentSlot.class);
        if (buffs == null) buffs = new ArrayList<>();
        if (debuffs == null) debuffs = new ArrayList<>();
        if (elements == null) elements = new ArrayList<>();
        if (discoveredMobs == null) discoveredMobs = new HashSet<>();
        if (discoveredBosses == null) discoveredBosses = new HashSet<>();
        if (controls == null) controls = "DEFAULT";
        if (partyId == null) partyId = "";
        if (enhancementMaterials == null) enhancementMaterials = new LinkedHashMap<>();
        enhancementMaterials.replaceAll((id, amount) -> Math.max(0L, amount == null ? 0L : amount));
        enhancementMaterials.entrySet().removeIf(entry -> entry.getValue() <= 0);
        dataVersion = DATA_VERSION;
        return this;
    }
}
