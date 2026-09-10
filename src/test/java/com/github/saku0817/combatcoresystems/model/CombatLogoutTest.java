package com.github.saku0817.combatcoresystems.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CombatLogoutTest {
    @Test void violationsExpireIndividuallyEvenWithoutOnlineTicks() {
        PlayerData data = new PlayerData();
        long start = 1_800_000_000_000L;
        assertEquals(1, data.recordCombatLogout(start));
        assertEquals(2, data.recordCombatLogout(start + 3_600_000));
        assertEquals(2, data.expireCombatLogouts(start + 86_399_999));
        assertEquals(1, data.expireCombatLogouts(start + 86_400_000));
        assertEquals(0, data.expireCombatLogouts(start + 90_000_000));
    }
    @Test void penaltyResetClearsTimestamps() {
        PlayerData data = new PlayerData();
        data.recordCombatLogout(System.currentTimeMillis());
        data.setCombatLogoutCount(0);
        assertEquals(0, data.getCombatLogoutCount());
    }
}
