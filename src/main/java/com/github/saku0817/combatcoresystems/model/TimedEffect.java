package com.github.saku0817.combatcoresystems.model;

import java.util.UUID;

public final class TimedEffect {
    private String id = "";
    private String source = "";
    private int stacks = 1;
    private long remainingMillis;
    private boolean permanent;

    public TimedEffect() {}

    public TimedEffect(String id, UUID source, int stacks, long remainingMillis, boolean permanent) {
        this.id = id;
        this.source = source == null ? "" : source.toString();
        this.stacks = Math.max(1, stacks);
        this.remainingMillis = Math.max(0, remainingMillis);
        this.permanent = permanent;
    }

    public String getId() { return id; }
    public String getSource() { return source; }
    public int getStacks() { return stacks; }
    public long getRemainingMillis() { return remainingMillis; }
    public boolean isPermanent() { return permanent; }
    public void setStacks(int stacks) { this.stacks = Math.max(1, stacks); }
    public void setRemainingMillis(long remainingMillis) { this.remainingMillis = Math.max(0, remainingMillis); }
}
