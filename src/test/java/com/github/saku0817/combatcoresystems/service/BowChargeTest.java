package com.github.saku0817.combatcoresystems.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BowChargeTest {
    @Test void drawTimeUsesMeleeCurveWithoutProjectileVelocity() {
        for (double draw : new double[]{0,.1,.25,.5,.75,1}) {
            double force = (draw*draw+2*draw)/3;
            assertEquals(DamageService.attackMultiplier(draw),DamageService.bowAttackMultiplier(force),1e-12);
        }
        assertEquals(.4,DamageService.bowAttackMultiplier(1.25/3),1e-12);
    }
    @Test void clampsCharge() {
        assertEquals(.2,DamageService.bowAttackMultiplier(-1),1e-12);
        assertEquals(1,DamageService.bowAttackMultiplier(3),1e-12);
        assertEquals(.2,DamageService.bowAttackMultiplier(Double.NaN),1e-12);
    }
}
