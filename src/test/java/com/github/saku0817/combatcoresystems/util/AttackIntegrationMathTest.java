package com.github.saku0817.combatcoresystems.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttackIntegrationMathTest {
    @Test void weaponAttackIsAddedOnceBeforePercentAndFlatBonuses() {
        assertEquals(9, CoreMath.attack(2, 7, 0, 0));
        assertEquals(79.6, CoreMath.attack(40, 18, 0.2, 10), 1e-9);
    }
    @Test void nativeWeaponModifiersDoNotDoubleCountTheCcsResult() {
        assertEquals(3, CoreMath.nativeAttackBase(9, 6, 0, 1));
        double base = CoreMath.nativeAttackBase(79.6, 6, 0.2, 1.5);
        assertEquals(79.6, (base + 6) * 1.2 * 1.5, 1e-9);
    }
}
