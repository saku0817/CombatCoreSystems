package com.github.saku0817.combatcoresystems.service;
import com.github.saku0817.combatcoresystems.command.CcsAdminCommand;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class V144RegressionTest {
    @Test void zeroAndSelfDamageDoNotEnterCombat() {
        UUID player = UUID.randomUUID(), mob = UUID.randomUUID();
        assertFalse(DamageService.entersCombat(0, player, mob)); assertFalse(DamageService.entersCombat(10, player, player));
        assertFalse(DamageService.entersCombat(10, null, player)); assertTrue(DamageService.entersCombat(1, player, mob));
    }
    @Test void selectorIndexesOnlyTargetArguments() {
        assertEquals(2, CcsAdminCommand.targetIndex(new String[]{"give", "item", "@s"}));
        assertEquals(3, CcsAdminCommand.targetIndex(new String[]{"edit", "force", "on", "@a"}));
        assertEquals(3, CcsAdminCommand.targetIndex(new String[]{"edit", "buff", "add", "@s", "x"}));
        assertEquals(-1, CcsAdminCommand.targetIndex(new String[]{"region", "create", "@s"}));
    }
    @Test void resonanceCountsTowardSetButHeartDoesNot() {
        var equipment = new EquipmentDefinition("echo", "echo", "PAPER", EquipmentSlot.RESONANCE, 5, 15, StatKey.FIRE_DAMAGE, .05, .2, List.of(), "series", null, List.of());
        var snapshot = new DefinitionRegistry.Snapshot(Map.of(), Map.of(), Map.of(), Map.of("echo", equipment), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        PlayerData data = new PlayerData(); ItemInstance item = new ItemInstance(); item.setDefinitionId("echo");
        data.getEquipment().put(EquipmentSlot.RESONANCE, item); data.getEquipment().put(EquipmentSlot.DIVINE_HEART, item);
        assertEquals(1, SetEffectService.counts(snapshot, data).get("series"));
    }
}
