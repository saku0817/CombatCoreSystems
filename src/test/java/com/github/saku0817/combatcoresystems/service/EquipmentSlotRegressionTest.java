package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.EquipmentSlot;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentSlotRegressionTest {
    @Test void emptyInventoryIdMustNotMatchDivineHeartRoot() throws Exception {
        YamlConfiguration hearts = new YamlConfiguration();
        hearts.loadFromString("""
                divine-hearts:
                  divine-heart-flame:
                    name: 炎の神心
                    material: NETHER_STAR
                    modifiers:
                      FIRE_DAMAGE: 0.2
                """);
        // v1.3.0 treated this root section as an item, then cloned an empty inventory slot.
        assertTrue(hearts.contains("divine-hearts."));
        DefinitionRegistry registry = new DefinitionRegistry(null);
        var field = DefinitionRegistry.class.getDeclaredField("current");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<DefinitionRegistry.Snapshot> current = (AtomicReference<DefinitionRegistry.Snapshot>) field.get(registry);
        current.set(new DefinitionRegistry.Snapshot(Map.of("divine_hearts.yml", hearts), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));
        EquipmentService service = new EquipmentService(null, registry, null, null, null, null);
        assertNull(service.slotOf(null));
        assertNull(service.slotOf(""));
        assertNull(service.slotOf("unknown"));
        assertEquals(EquipmentSlot.DIVINE_HEART, service.slotOf("divine-heart-flame"));
    }
}
