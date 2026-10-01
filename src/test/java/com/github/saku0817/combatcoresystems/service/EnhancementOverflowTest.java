package com.github.saku0817.combatcoresystems.service;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EnhancementOverflowTest {
    private List<EnhancementService.MaterialInfo> materials() { return List.of(
        new EnhancementService.MaterialInfo("low","low","PAPER",100),
        new EnhancementService.MaterialInfo("mid","mid","PAPER",1000),
        new EnhancementService.MaterialInfo("high","high","PAPER",10000)); }
    @Test void overflowOnlyAppliesAfterMaximumAndRoundsDownToHundreds() {
        assertEquals(0,EnhancementService.overflow(1,3,0,150,l->100));
        assertEquals(199,EnhancementService.overflow(1,3,0,399,l->100));
        assertEquals(Map.of("low",1L),EnhancementService.refundPlan(199,materials()));
        assertEquals(Map.of(),EnhancementService.refundPlan(99,materials()));
        assertEquals(Map.of("high",1L,"mid",2L,"low",3L),EnhancementService.refundPlan(12399,materials()));
    }
    @Test void missingDenominationsCannotSilentlyLoseRefundableExperience() {
        assertNull(EnhancementService.refundPlan(500,List.of(new EnhancementService.MaterialInfo("big","big","PAPER",1000))));
        assertEquals(Map.of(),EnhancementService.refundPlan(50,List.of()));
    }
}
