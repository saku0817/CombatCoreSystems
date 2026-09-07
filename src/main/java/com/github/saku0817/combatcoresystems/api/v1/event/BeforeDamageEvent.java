package com.github.saku0817.combatcoresystems.api.v1.event;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class BeforeDamageEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final DamageRequest request;
    private boolean cancelled;

    public BeforeDamageEvent(DamageRequest request) { this.request = request; }
    public DamageRequest getRequest() { return request; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
