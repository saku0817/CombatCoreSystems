package com.github.saku0817.combatcoresystems.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @Test void renamesOnlyUncustomizedLegacyLabels() {
        var yaml=new YamlConfiguration();
        yaml.set("divine-heart-tooltip.slot","<white>装備部位：<u>神心</u></white>");
        yaml.set("display-names.HEALING_POWER","独自の回復表示");
        assertTrue(DefinitionRegistry.migrateDisplayNames("messages.yml",yaml));
        assertEquals("<white>装備部位：<u>追憶</u></white>",yaml.getString("divine-heart-tooltip.slot"));
        assertEquals("独自の回復表示",yaml.getString("display-names.HEALING_POWER"));
        assertFalse(DefinitionRegistry.migrateDisplayNames("messages.yml",yaml));
    }
    @Test void preservesUserValuesAndExplicitlyRemovedDefinitions() throws Exception {
        YamlConfiguration current = new YamlConfiguration(), defaults = new YamlConfiguration();
        current.loadFromString("controls: {drop-skill: false}\nweapons: {}\ncustom: 42\nlegacy: false\n");
        defaults.loadFromString("controls: {drop-skill: true, allow-empty-cast: true}\nweapons: {example: {name: test}}\nlegacy: {new-key: 1}\n");
        assertTrue(DefinitionRegistry.mergeMissing(current, defaults));
        assertFalse(current.getBoolean("controls.drop-skill"));
        assertTrue(current.getBoolean("controls.allow-empty-cast"));
        assertEquals(42, current.getInt("custom"));
        assertFalse(current.contains("weapons.example"));
        assertEquals(false, current.get("legacy"));
        assertFalse(DefinitionRegistry.mergeMissing(current, defaults));
    }
}
