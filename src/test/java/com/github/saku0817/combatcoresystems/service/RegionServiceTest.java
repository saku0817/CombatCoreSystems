package com.github.saku0817.combatcoresystems.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RegionServiceTest {
    @Test
    void emptyHandIsNotTreatedAsRegionWand() {
        assertFalse(RegionService.isWand(null, null));
    }
}
