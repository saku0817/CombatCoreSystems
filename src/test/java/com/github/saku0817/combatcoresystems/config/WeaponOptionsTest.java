package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WeaponOptionsTest {
    @Test void bundledExampleIsExecutableAndReferencesSeparateBuffDefinitions() throws Exception {
        var yaml = YamlConfiguration.loadConfiguration(new File("src/main/resources/weapons.yml"));
        var method = DefinitionRegistry.class.getDeclaredMethod("parseWeapons", YamlConfiguration.class, List.class, List.class);
        method.setAccessible(true);
        var errors = new ArrayList<String>();
        @SuppressWarnings("unchecked") var weapons = (Map<String, WeaponDefinition>) method.invoke(new DefinitionRegistry(null), yaml, errors, new ArrayList<String>());
        assertEquals(List.of(), errors);
        var w = weapons.get("guiding_star");
        assertNotNull(w);
        assertEquals(Element.FIRE, w.options().normalElement());
        assertEquals(WeaponOptions.Hand.MAIN_HAND, w.options().talent().hand());
        assertEquals(.5, w.options().talent().modifiers().get(StatKey.HP_PERCENT));
        assertEquals(.3, w.skill().options().currentHpCost());
        assertFalse(w.skill().options().damageEnabled());
        assertEquals(List.of("departure_crit"), w.skill().options().selfEffects());
        assertEquals(List.of("sunset"), w.ultimate().options().targetEffects());
        assertEquals(2, w.ultimate().options().components().size());
        assertEquals(Element.PHYSICAL, w.ultimate().options().components().getFirst().bonusElement());
        assertEquals(Element.FIRE, w.ultimate().options().components().getLast().bonusElement());
        assertFalse(w.skill().options().description().isEmpty());
        var buffs = YamlConfiguration.loadConfiguration(new File("src/main/resources/buffs.yml"));
        assertEquals(-.3, buffs.getDouble("buffs.sunset.modifiers.percent.ATK_PERCENT"));
        assertEquals(.4, buffs.getDouble("buffs.sunset.modifiers.percent.DEF_IGNORED_WHEN_HIT"));
        assertEquals(3, buffs.getDouble("buffs.sunset.duration"));
    }
    @Test void invalidHpCostIsRejected() {
        var s = new YamlConfiguration(); s.set("cost.current-hp-percent", 1);
        assertThrows(IllegalArgumentException.class, () -> WeaponOptionsParser.ability(s));
        s.set("cost.current-hp-percent", Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> WeaponOptionsParser.ability(s));
    }
    @Test void invalidParticleCannotReachCombat() {
        var s = new YamlConfiguration(); s.set("normal-attack.visual.particle", "DOES_NOT_EXIST");
        assertThrows(IllegalArgumentException.class, () -> WeaponOptionsParser.weapon(s));
        s.set("normal-attack.visual.particle", "DUST");
        assertThrows(IllegalArgumentException.class, () -> WeaponOptionsParser.weapon(s));
    }
    @Test void excessiveVisualCountIsRejected() {
        var s = new YamlConfiguration(); s.set("normal-attack.visual.count", 501);
        assertThrows(IllegalArgumentException.class, () -> WeaponOptionsParser.weapon(s));
    }
    @Test void arbitraryReferenceAndDescriptionAreParsed() throws Exception {
        var s = new YamlConfiguration(); s.loadFromString("""
                description: '<red>防御力に応じて攻撃</red>'
                damage-components:
                  - reference: DEF
                    multiplier: 3
                    bonus-attribute: WATER
                """);
        var ability = WeaponOptionsParser.ability(s);
        assertEquals(ReferenceStat.DEF, ability.components().getFirst().reference());
        assertEquals(3, ability.components().getFirst().multiplier());
        assertEquals(1, ability.description().size());
    }
    @Test void unsupportedTalentKeyIsRejected() {
        var s = new YamlConfiguration(); s.set("talent.modifiers.MADE_UP_ATK", .5);
        assertThrows(IllegalArgumentException.class, () -> WeaponOptionsParser.weapon(s));
    }
    @Test void uncategorizedIsCaseInsensitive() throws Exception {
        var yaml = YamlConfiguration.loadConfiguration(new File("src/main/resources/weapons.yml"));
        yaml.set("weapons.guiding_star.category", "uncategorized");
        var method = DefinitionRegistry.class.getDeclaredMethod("parseWeapons", YamlConfiguration.class, List.class, List.class);
        method.setAccessible(true); var errors = new ArrayList<String>();
        @SuppressWarnings("unchecked") var weapons = (Map<String, WeaponDefinition>) method.invoke(new DefinitionRegistry(null), yaml, errors, new ArrayList<String>());
        assertTrue(errors.isEmpty());
        assertEquals(WeaponDefinition.Category.UNCATEGORIZED, weapons.get("guiding_star").category());
    }
}
