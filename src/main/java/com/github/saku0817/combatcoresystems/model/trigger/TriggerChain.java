package com.github.saku0817.combatcoresystems.model.trigger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Shared by synchronous damage/heal/stack child events. */
public final class TriggerChain {
    private final UUID id = UUID.randomUUID();
    private final Set<String> executed = new HashSet<>();
    private int depth;
    public UUID id() { return id; }
    public int depth() { return depth; }
    public boolean enter(UUID owner, String source, String trigger) {
        if (depth >= 16 || !executed.add(owner + ":" + source + ":" + trigger)) return false;
        depth++;
        return true;
    }
    public void leave() {
        if (depth <= 0) throw new IllegalStateException("Unbalanced trigger chain");
        depth--;
    }
}
