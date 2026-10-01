package com.github.saku0817.combatcoresystems.service;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebEditorSupportTest {
    @Test void equipmentTemplateMatchesFixedGrowthAndSlot() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        WebEditorSupport.addEntry(yaml,"equipment.yml","chest","equipment");
        assertEquals("CHEST",yaml.getString("equipment.chest.slot"));
        assertEquals(9,yaml.getInt("equipment.chest.max-level"));
        assertTrue(yaml.contains("equipment.chest.main-stat-candidates.ATK_FLAT"));
        assertTrue(yaml.contains("equipment.chest.main-stat-candidates.ATK_PERCENT"));
        assertFalse(yaml.contains("equipment.chest.main-stat"));
        assertEquals(4,yaml.getConfigurationSection("equipment.chest.substat-candidates").getKeys(false).size());
    }
    @Test void insertsIntoEmptyMappingWithoutCorruptingOtherRoots() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("data-version: 1\nmobs: {}\nvanilla-mobs: {}\nvanilla-defaults: {custom-exp: 42}\n");
        WebEditorSupport.addEntry(yaml, "mobs.yml", "test_mob", "mobs");
        assertEquals(50000, yaml.getDouble("mobs.test_mob.stats.hp.min"));
        assertEquals(42, yaml.getInt("vanilla-defaults.custom-exp"));
        assertFalse(yaml.contains("vanilla-mobs.test_mob"));
        assertThrows(IllegalArgumentException.class, () -> WebEditorSupport.addEntry(yaml, "mobs.yml", "test_mob", "mobs"));
        assertThrows(IllegalArgumentException.class, () -> WebEditorSupport.addEntry(yaml, "mobs.yml", "../test", "mobs"));
        YamlConfiguration roundTrip = new YamlConfiguration(); roundTrip.loadFromString(yaml.saveToString());
        assertEquals("ZOMBIE", roundTrip.getString("mobs.test_mob.entity-type"));
    }
    @Test void buffTemplateUsesExecutableKeys() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        WebEditorSupport.addEntry(yaml, "buffs.yml", "power", "buffs");
        assertEquals(10, yaml.getDouble("buffs.power.duration"));
        assertEquals(0.10, yaml.getDouble("buffs.power.modifiers.flat.ATK_PERCENT"));
    }
}
