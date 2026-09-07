package com.github.saku0817.combatcoresystems.api.v1.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class CombatStateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID entityId;
    private final boolean entering;
    public CombatStateEvent(UUID entityId, boolean entering) { this.entityId = entityId; this.entering = entering; }
    public UUID getEntityId() { return entityId; }
    public boolean isEntering() { return entering; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
