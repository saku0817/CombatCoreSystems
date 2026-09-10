package com.github.saku0817.combatcoresystems.service;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillInputRegressionTest {
    @Test void sneakInputsAreRegisteredBeforeNormalDamage() throws Exception {
        var attack = SkillService.class.getMethod("onSneakAttack", io.papermc.paper.event.player.PrePlayerAttackEntityEvent.class).getAnnotation(EventHandler.class);
        assertNotNull(attack);
        assertEquals(EventPriority.LOW, attack.priority());
        assertTrue(attack.ignoreCancelled());
        assertNotNull(SkillService.class.getMethod("onSneakUse", org.bukkit.event.player.PlayerInteractEvent.class).getAnnotation(EventHandler.class));
        assertNotNull(SkillService.class.getMethod("onSneakEntityUse", org.bukkit.event.player.PlayerInteractEntityEvent.class).getAnnotation(EventHandler.class));
    }
}
