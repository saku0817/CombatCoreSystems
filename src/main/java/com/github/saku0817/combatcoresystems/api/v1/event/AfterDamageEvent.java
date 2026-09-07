package com.github.saku0817.combatcoresystems.api.v1.event;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AfterDamageEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final DamageRequest request;
    private final DamageResult result;
    public AfterDamageEvent(DamageRequest request, DamageResult result) { this.request = request; this.result = result; }
    public DamageRequest getRequest() { return request; }
    public DamageResult getResult() { return result; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
