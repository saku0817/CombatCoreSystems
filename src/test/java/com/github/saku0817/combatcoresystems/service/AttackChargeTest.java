package com.github.saku0817.combatcoresystems.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AttackChargeTest {
    @Test void rapidAndFullyChargedAttacksHaveDifferentStrength() {
        assertEquals(0.2, DamageService.attackMultiplier(0), 0.00001);
        assertEquals(0.4, DamageService.attackMultiplier(0.5), 0.00001);
        assertEquals(1, DamageService.attackMultiplier(1), 0.00001);
        assertEquals(1, DamageService.attackMultiplier(2), 0.00001);
    }
}
