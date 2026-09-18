package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeaponStagesTest {
    @Test @SuppressWarnings("unchecked") void stagesResolveCumulativelyAndValidateAllAbilities() throws Exception {
        var yaml = new YamlConfiguration();
        yaml.loadFromString("""
            weapons:
              test:
                material: IRON_SWORD
                category: MELEE
                base-atk: {level-1: 10, level-100: 100}
                talent:
                  name: base
                  hand: MAIN_HAND
                  modifiers: {HP_PERCENT: 0.10}
                skill: {multiplier: 1, cooldown-seconds: 10}
                limit-breaks:
                  1:
                    base-atk: {level-1: 20}
                    talent: {hand: HOT_BAR, modifiers: {HP_PERCENT: 0.2}}
                    skill: {name: upgraded, cooldown: 5, description: ['stage 1']}
                  2:
                    skill: {multiplier: 3, self-effects: [test_buff]}
                    ultimate: {reference: HP, multiplier: 0.5, attribute: FIRE}
            """);
        var parser = new DefinitionRegistry(null);
        var parse = DefinitionRegistry.class.getDeclaredMethod("parseWeapons", YamlConfiguration.class, List.class, List.class);
        parse.setAccessible(true);
        List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
        var base = (Map<String, WeaponDefinition>) parse.invoke(parser, yaml, errors, warnings);
        var stages = DefinitionRegistry.class.getDeclaredMethod("parseWeaponStages", YamlConfiguration.class, Map.class, List.class, List.class);
        stages.setAccessible(true);
        var resolved = ((Map<String, List<WeaponDefinition>>) stages.invoke(parser, yaml, base, errors, warnings)).get("test");
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(6, resolved.size());
        assertEquals(10, resolved.get(0).attackAt(1));
        assertEquals(20, resolved.get(5).attackAt(1));
        assertEquals(100, resolved.get(5).attackAt(100));
        assertEquals(WeaponOptions.Hand.HOT_BAR, resolved.get(5).options().talent().hand());
        assertEquals(.2, resolved.get(5).options().talent().modifiers().get(StatKey.HP_PERCENT));
        assertEquals(5, resolved.get(5).skill().cooldownSeconds());
        assertEquals(3, resolved.get(5).skill().multiplier());
        assertEquals(List.of("stage 1"), resolved.get(5).skill().options().description());
        assertEquals(List.of("test_buff"), resolved.get(5).skill().options().selfEffects());
        assertEquals(ReferenceStat.HP, resolved.get(5).ultimate().referenceStat());
        assertNull(resolved.get(1).ultimate());
    }
}
