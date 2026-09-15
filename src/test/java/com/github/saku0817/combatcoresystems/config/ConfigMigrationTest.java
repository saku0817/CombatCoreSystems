package com.github.saku0817.combatcoresystems.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
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
