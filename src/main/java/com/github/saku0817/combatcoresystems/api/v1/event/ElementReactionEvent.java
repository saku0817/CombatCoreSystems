package com.github.saku0817.combatcoresystems.api.v1.event;

import com.github.saku0817.combatcoresystems.model.Element;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class ElementReactionEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID target;
    private final String reactionId;
    private final Element existingElement;
    private final Element incomingElement;
    private final long damage;
    public ElementReactionEvent(UUID target, String reactionId, Element existingElement, Element incomingElement, long damage) {
        this.target = target; this.reactionId = reactionId; this.existingElement = existingElement; this.incomingElement = incomingElement; this.damage = damage;
    }
    public UUID getTarget() { return target; }
    public String getReactionId() { return reactionId; }
    public Element getExistingElement() { return existingElement; }
    public Element getIncomingElement() { return incomingElement; }
    public long getDamage() { return damage; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
