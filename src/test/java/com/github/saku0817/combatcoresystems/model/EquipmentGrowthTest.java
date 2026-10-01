package com.github.saku0817.combatcoresystems.model;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentGrowthTest {
    private EquipmentDefinition definition(int rarity) {
        return new EquipmentDefinition("gear","test","IRON_CHESTPLATE",EquipmentSlot.CHEST,rarity,rarity*3,
                StatKey.CRIT_RATE,.1,.6,List.of(),"test",null,List.of());
    }
    private ItemInstance fresh(int rarity) {
        var item=new ItemInstance(); item.setDefinitionId("gear");
        EquipmentGrowth.initialize(item,definition(rarity),List.of(),List.of(),new Random(1){
            @Override public int nextInt(int bound) { return bound-1; }
        },false);
        return item;
    }
    @Test void lockedStatsAreEligibleAndRepeatedUpgradesAccumulate() {
        var item=fresh(5); assertEquals(0,item.getUnlockedSubstats());
        String last=new ArrayList<>(item.getSubstats().keySet()).getLast();
        for(int level=2;level<=15;level++) {
            EquipmentGrowth.advance(item,definition(5));
            assertEquals(Math.min(4,level/3),item.getUnlockedSubstats());
            assertEquals(level/3,item.getSubstatUpgrades().get(last));
        }
        assertEquals(5,item.getSubstatUpgrades().get(last));
        assertEquals(EquipmentGrowthTable.subValue(StatKey.valueOf(last),5),item.getSubstats().get(last));
    }
    @Test void threeStarStopsUnlockingAtTwoButUpgradesThreeTimes() {
        var item=fresh(3);
        for(int level=2;level<=12;level++) EquipmentGrowth.advance(item,definition(3));
        assertEquals(9,item.getLevel()); assertEquals(2,item.getUnlockedSubstats());
        assertEquals(3,item.getSubstatUpgrades().values().stream().mapToInt(Integer::intValue).sum());
    }
    @Test void migrationClampsRepairsAndIsIdempotentAcrossSavedCopies() {
        var legacy=new ItemInstance(); legacy.setLevel(25); legacy.setExp(999); legacy.setDefinitionId("gear");
        legacy.setMainStat(StatKey.CRIT_DAMAGE,.5,.9);
        legacy.getSubstats().put("ATK_FLAT",999.0); legacy.getSubstats().put("FIRE_DAMAGE",.5);
        var gson=new Gson(); var copy=gson.fromJson(gson.toJson(legacy),ItemInstance.class);
        assertTrue(EquipmentGrowth.migrate(legacy,definition(5),List.of(),List.of()));
        assertTrue(EquipmentGrowth.migrate(copy,definition(5),List.of(),List.of()));
        assertEquals(gson.toJson(legacy),gson.toJson(copy));
        assertEquals(15,legacy.getLevel()); assertEquals(0,legacy.getExp());
        assertTrue(EquipmentGrowthTable.mainCandidates(EquipmentSlot.CHEST).contains(legacy.mainStat(definition(5))));
        assertTrue(legacy.getSubstats().containsKey("ATK_FLAT")); assertFalse(legacy.getSubstats().containsKey("FIRE_DAMAGE"));
        assertEquals(4,legacy.getSubstats().size());
        String snapshot=gson.toJson(legacy);
        assertFalse(EquipmentGrowth.migrate(legacy,definition(5),List.of(),List.of()));
        assertEquals(snapshot,gson.toJson(legacy));
    }
    @Test void adminLevelSettingReplaysRollsWithoutRerolling() {
        var item=fresh(5); EquipmentGrowth.setLevel(item,definition(5),15);
        var expected=new LinkedHashMap<>(item.getSubstats());
        EquipmentGrowth.setLevel(item,definition(5),1);
        assertEquals(0,item.getUnlockedSubstats());
        assertTrue(item.getSubstatUpgrades().values().stream().allMatch(n->n==0));
        EquipmentGrowth.setLevel(item,definition(5),15); assertEquals(expected,item.getSubstats());
    }
}
