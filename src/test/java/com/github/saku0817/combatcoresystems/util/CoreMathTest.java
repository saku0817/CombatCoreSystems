package com.github.saku0817.combatcoresystems.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreMathTest {
    @Test void playerGrowthMatchesSpecification() {
        assertEquals(20.0, CoreMath.linear(20, 3000, 1, 100), 0.0001);
        assertEquals(3000.0, CoreMath.linear(20, 3000, 100, 100), 0.0001);
        assertEquals(2.0, CoreMath.linear(2, 200, 1, 100), 0.0001);
        assertEquals(100.0, CoreMath.linear(0, 100, 100, 100), 0.0001);
    }

    @Test void defenseFormulaMatchesSpecification() {
        double effective = CoreMath.effectiveDefense(100, 0.2);
        assertEquals(80.0, effective, 0.0001);
        assertEquals(200.0 / 480.0, CoreMath.defenseCoefficient(100, 100, effective), 0.0001);
    }

    @Test void resistanceCanBeNegativeButCapsAtOneHundredPercent() {
        assertEquals(-0.5, CoreMath.finalResistance(-0.5, 0), 0.0001);
        assertEquals(1.0, CoreMath.finalResistance(1.5, 0), 0.0001);
        assertEquals(0.8, CoreMath.finalResistance(1.0, 0.2), 0.0001);
    }

    @Test void cooldownCapsAtOneHundredPercent() {
        assertEquals(5.0, CoreMath.cooldownSeconds(10, 0.5), 0.0001);
        assertEquals(0.0, CoreMath.cooldownSeconds(10, 5), 0.0001);
    }

    @Test void quadraticExperienceGrows() {
        assertEquals(100, CoreMath.quadraticExp(100, 25, 1));
        assertEquals(200, CoreMath.quadraticExp(100, 25, 3));
    }
}
