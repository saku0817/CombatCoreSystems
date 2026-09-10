package com.github.saku0817.combatcoresystems.util;

import com.github.saku0817.combatcoresystems.model.StatKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DisplayNamesTest {
    @Test void everyStatHasAPlayerFacingJapaneseName() {
        for (StatKey key : StatKey.values()) assertNotEquals(key.name(), DisplayNames.japanese(key.name()), key.name());
        assertEquals("炎属性ダメージ", DisplayNames.japanese("FIRE-DAMAGE"));
        assertEquals("神の心", DisplayNames.japanese("DIVINE_HEART"));
    }
}
