package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.WeaponDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeaponConfigurationTest {
    @Test void attackAndSkillValuesSurviveConfigurationParsing() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                weapons:
                  test_sword:
                    name: Test
                    material: IRON_SWORD
                    type: MELEE
                    rarity: 4
                    base-atk:
                      level-1: 20
                      level-100: 200
                    skill:
                      multiplier: 2.5
                      attribute: FIRE
                      cooldown-seconds: 8
                    ultimate:
                      multiplier: 5
                      cooldown: 20
                """);
        var method = DefinitionRegistry.class.getDeclaredMethod("parseWeapons", YamlConfiguration.class, List.class, List.class);
        method.setAccessible(true);
        List<String> errors = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, WeaponDefinition> weapons = (Map<String, WeaponDefinition>) method.invoke(new DefinitionRegistry(null), yaml, errors, new ArrayList<String>());
        assertTrue(errors.isEmpty(), errors.toString());
        WeaponDefinition weapon = weapons.get("test_sword");
        assertNotNull(weapon);
        assertEquals(20, weapon.attackAtLevel1());
        assertEquals(200, weapon.attackAtLevel100());
        assertEquals(2.5, weapon.skill().multiplier());
        assertEquals(8, weapon.skill().cooldownSeconds());
        assertEquals(20, weapon.ultimate().cooldownSeconds());
    }
}
