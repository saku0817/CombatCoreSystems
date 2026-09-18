package com.github.saku0817.combatcoresystems.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/** Item clients otherwise inherit purple/italic lore. Explicit child formatting is retained. */
public final class ItemText {
    private ItemText() {}
    public static Component parse(String text) { return defaults(MiniMessage.miniMessage().deserialize(text)); }
    public static Component defaults(Component text) {
        return Component.empty().color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).append(text);
    }
}
