package com.github.saku0817.combatcoresystems.api.v1.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public final class BossPhaseChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final UUID boss; private final String definitionId; private final String oldPhase; private final String newPhase;
    public BossPhaseChangeEvent(UUID boss, String definitionId, String oldPhase, String newPhase) { this.boss = boss; this.definitionId = definitionId; this.oldPhase = oldPhase; this.newPhase = newPhase; }
    public UUID getBoss() { return boss; } public String getDefinitionId() { return definitionId; } public String getOldPhase() { return oldPhase; } public String getNewPhase() { return newPhase; }
    @Override public HandlerList getHandlers() { return HANDLERS; } public static HandlerList getHandlerList() { return HANDLERS; }
}
