package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.ItemText;
import net.kyori.adventure.text.format.*;
import org.bukkit.event.inventory.InventoryType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class V143RegressionTest {
    @Test void survivalAndCreativeHandDropsAreSupported() {
        assertTrue(SkillService.isHandDropView("CRAFTING"));
        assertTrue(SkillService.isHandDropView("CREATIVE"));
        assertFalse(SkillService.isHandDropView("CHEST"));
        assertFalse(SkillService.isHandDropView("ANVIL"));
    }
    @Test void limitBreakRejectsSelfEvenWhenDeserializedIntoDifferentObjects() {
        ItemInstance target = new ItemInstance(); target.setDefinitionId("a");
        var gson = new com.google.gson.Gson();
        ItemInstance alias = gson.fromJson(gson.toJson(target), ItemInstance.class);
        assertFalse(EnhancementService.validDuplicate(target, alias));
        ItemInstance other = new ItemInstance(); other.setDefinitionId("a");
        assertTrue(EnhancementService.validDuplicate(target, other));
        other.setDefinitionId("b"); assertFalse(EnhancementService.validDuplicate(target, other));
        assertFalse(EnhancementService.validDuplicate(target, null));
    }
    @Test void loreDefaultsAreWhiteNonItalicWithoutRemovingExplicitFormatting() {
        var plain = ItemText.parse("description");
        assertEquals(NamedTextColor.WHITE, plain.color());
        assertEquals(TextDecoration.State.FALSE, plain.decoration(TextDecoration.ITALIC));
        var styled = ItemText.parse("<red><italic>explicit</italic></red>");
        assertEquals(NamedTextColor.RED, styled.children().getFirst().color());
        assertEquals(TextDecoration.State.TRUE, styled.children().getFirst().decoration(TextDecoration.ITALIC));
    }
    @Test void passiveInventoryRangesExcludeArmorAndIncludeOffhandOnlyWhereSpecified() {
        assertTrue(WeaponOptions.Hand.HOT_BAR.includes(0, 4));
        assertFalse(WeaponOptions.Hand.HOT_BAR.includes(40, 4));
        assertTrue(WeaponOptions.Hand.INVENTORY.includes(35, 4));
        assertTrue(WeaponOptions.Hand.INVENTORY.includes(40, 4));
        assertFalse(WeaponOptions.Hand.INVENTORY.includes(38, 4));
        assertFalse(WeaponOptions.Hand.MAIN_HAND.includes(0, 4));
        assertTrue(WeaponOptions.Hand.EITHER_HAND.includes(40, 4));
    }
}
