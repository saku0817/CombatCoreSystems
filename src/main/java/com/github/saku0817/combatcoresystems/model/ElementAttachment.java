package com.github.saku0817.combatcoresystems.model;

public final class ElementAttachment {
    private Element element = Element.PHYSICAL;
    private long remainingMillis;
    private long attachedOrder;

    public ElementAttachment() {}

    public ElementAttachment(Element element, long remainingMillis, long attachedOrder) {
        this.element = element;
        this.remainingMillis = Math.max(0, remainingMillis);
        this.attachedOrder = attachedOrder;
    }

    public Element getElement() { return element; }
    public long getRemainingMillis() { return remainingMillis; }
    public long getAttachedOrder() { return attachedOrder; }
    public void setRemainingMillis(long remainingMillis) { this.remainingMillis = Math.max(0, remainingMillis); }
}
