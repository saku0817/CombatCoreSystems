package com.github.saku0817.combatcoresystems.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentGrowthTableTest {
    @Test void percentagesAreRatiosAndNonLinearValuesArePreserved() {
        assertEquals(.117,EquipmentGrowthTable.mainValue(EquipmentSlot.HEAD,StatKey.HP_PERCENT,2),1e-12);
        assertEquals(231,EquipmentGrowthTable.mainValue(EquipmentSlot.HEAD,StatKey.HP_FLAT,3));
        assertEquals(.345,EquipmentGrowthTable.mainValue(EquipmentSlot.FEET,StatKey.HEALING_POWER,15),1e-12);
        assertEquals(.388,EquipmentGrowthTable.mainValue(EquipmentSlot.RESONANCE,StatKey.THUNDER_RESISTANCE,15),1e-12);
    }
    @Test void rarityAndUpgradeTablesAreExact() {
        for (int rarity=3;rarity<=5;rarity++) {
            assertEquals(rarity*3,EquipmentGrowthTable.maximumLevel(rarity));
            assertEquals(rarity-1,EquipmentGrowthTable.maximumSubstats(rarity));
        }
        assertEquals(228,EquipmentGrowthTable.subValue(StatKey.HP_FLAT,5));
        assertEquals(.174,EquipmentGrowthTable.subValue(StatKey.CRIT_RATE,5),1e-12);
        assertEquals(.058,EquipmentGrowthTable.subValue(StatKey.CRIT_DAMAGE,0),1e-12);
    }
    @Test void invalidSelectionsAndOutOfRangeLevelsAreRejected() {
        assertEquals(8,EquipmentGrowthTable.subCandidates().size());
        assertFalse(EquipmentGrowthTable.subCandidates().contains(StatKey.FIRE_DAMAGE));
        assertThrows(IllegalArgumentException.class,()->EquipmentGrowthTable.mainValue(EquipmentSlot.CHEST,StatKey.HP_FLAT,1));
        assertThrows(IllegalArgumentException.class,()->EquipmentGrowthTable.mainValue(EquipmentSlot.LEGS,StatKey.CRIT_RATE,16));
        assertThrows(IllegalArgumentException.class,()->EquipmentGrowthTable.subValue(StatKey.FIRE_RESISTANCE,0));
        assertThrows(IllegalArgumentException.class,()->EquipmentGrowthTable.subValue(StatKey.ATK_FLAT,6));
    }
}
