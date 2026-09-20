package com.github.saku0817.combatcoresystems.model;

import com.google.gson.Gson;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentRollsTest {
    private EquipmentDefinition definition() { return new EquipmentDefinition("chest", "test", "IRON_CHESTPLATE", EquipmentSlot.CHEST, 5, 15,
            StatKey.CRIT_RATE, .05, .15, List.of(StatKey.CRIT_RATE), "test", null, List.of()); }
    @Test void weightedMainAndUniqueSubstatsAllowOverlapWithMain() throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString("""
            main:
              CRIT_RATE: {level-1: 0.1, max-level: 0.3, weight: 1}
              CRIT_DAMAGE: {level-1: 0.2, max-level: 0.6, weight: 2}
            sub:
              CRIT_RATE: {value: 0.05}
              CRIT_DAMAGE: {value: 0.1}
            """);
        var main = EquipmentRolls.candidates(yaml.getConfigurationSection("main"), true);
        var sub = EquipmentRolls.candidates(yaml.getConfigurationSection("sub"), false);
        for (int seed = 0; seed < 100; seed++) {
            var chosen = EquipmentRolls.draw(main, 1, new Random(seed)).getFirst();
            var selected = EquipmentRolls.draw(sub, 4, new Random(seed));
            assertEquals(2, selected.size()); assertEquals(2, selected.stream().map(EquipmentRolls.Candidate::key).distinct().count());
            assertTrue(selected.stream().anyMatch(c -> c.key() == chosen.key()));
        }
    }
    @Test void selectedMainPersistsAndScalesWithoutRerolling() {
        ItemInstance original = new ItemInstance(); original.setDefinitionId("chest"); original.setMainStat(StatKey.CRIT_DAMAGE, .2, .6);
        var copy = new Gson().fromJson(new Gson().toJson(original), ItemInstance.class);
        assertEquals(original.getInstanceId(), copy.getInstanceId()); assertEquals(StatKey.CRIT_DAMAGE, copy.mainStat(definition()));
        assertEquals(.2, copy.mainValue(definition()), 1e-9); copy.setLevel(15); assertEquals(.6, copy.mainValue(definition()), 1e-9);
    }
    @Test void legacyInstancesKeepDefinitionMain() { assertEquals(StatKey.CRIT_RATE, new ItemInstance().mainStat(definition())); assertEquals(.05, new ItemInstance().mainValue(definition()), 1e-9); }
    @Test void invalidWeightsAndDuplicateKeysRejected() throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString("pool: {CRIT_RATE: {weight: 0}}");
        assertThrows(IllegalArgumentException.class, () -> EquipmentRolls.candidates(yaml.getConfigurationSection("pool"), true));
        yaml.loadFromString("pool: {CRIT_RATE: {value: 0.1}, crit_rate: {value: 0.2}}");
        assertThrows(IllegalArgumentException.class, () -> EquipmentRolls.candidates(yaml.getConfigurationSection("pool"), false));
    }
    @Test void badMainValueRejected() { assertThrows(IllegalArgumentException.class, () -> new ItemInstance().setMainStat(StatKey.CRIT_RATE, Double.NaN, 1)); }
}
