package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.WeaponDefinition;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeaponConfigurationTest {
    @Test void missingSkillIsNotInvented() throws Exception {
        var method = DefinitionRegistry.class.getDeclaredMethod("parseSkill", String.class, org.bukkit.configuration.ConfigurationSection.class, List.class);
        method.setAccessible(true);
        assertNull(method.invoke(new DefinitionRegistry(null), "sword:skill", null, new ArrayList<String>()));
    }
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
                      level-1: 18
                      level-100: 420
                    equip-level:
                      min: 20
                      max: 100
                    attribute-bonus:
                      type: FIRE_DAMAGE
                      value: 0.15
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
        assertEquals(18, weapon.attackAtLevel1());
        assertEquals(420, weapon.attackAtLevel100());
        assertEquals(20, weapon.minimumEquipLevel());
        assertEquals(com.github.saku0817.combatcoresystems.model.Element.FIRE, weapon.bonusElement());
        assertEquals(0.15, weapon.elementBonus());
        assertFalse(weapon.canEquip(19));
        assertTrue(weapon.canEquip(20));
        assertEquals(0, weapon.attackFor(19, 1));
        assertEquals(18, weapon.attackFor(20, 1));
        assertEquals(420, weapon.attackFor(20, 100));
        assertEquals(58, com.github.saku0817.combatcoresystems.util.CoreMath.attack(40, weapon.attackFor(20, 1), 0, 0));
        assertEquals(2.5, weapon.skill().multiplier());
        assertEquals(8, weapon.skill().cooldownSeconds());
        assertEquals(20, weapon.ultimate().cooldownSeconds());
    }
}
