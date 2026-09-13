package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComponentDamageTest {
    @Test void onlySecondTermReceivesFireBonus() {
        double first = DamageService.componentAmount(new WeaponOptions.Component(ReferenceStat.ATK, 2, Element.PHYSICAL), 1000, 100, 50, .15);
        double second = DamageService.componentAmount(new WeaponOptions.Component(ReferenceStat.ATK, 1, Element.FIRE), 1000, 100, 50, .15);
        assertEquals(200, first); assertEquals(115, second, 1e-9); assertEquals(315, first + second, 1e-9);
    }
    @Test void hpAndDefenseReferencesAreIndependent() {
        assertEquals(200, DamageService.componentAmount(new WeaponOptions.Component(ReferenceStat.HP, .2, Element.PHYSICAL), 1000, 100, 50, .5));
        assertEquals(150, DamageService.componentAmount(new WeaponOptions.Component(ReferenceStat.DEF, 2, Element.WATER), 1000, 100, 50, .5));
    }
    @Test void oldDamageApiConstructorStillWorks() {
        var request = new com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest(null, java.util.UUID.randomUUID(), ReferenceStat.ATK, 2, Element.FIRE, true, false, 0, "test");
        assertTrue(request.components().isEmpty());
    }
}
