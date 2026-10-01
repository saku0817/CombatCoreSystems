package com.github.saku0817.combatcoresystems.model.trigger;

public enum TriggerEvent {
    SKILL, ULTIMATE, HIT, TAKE_DAMAGE, HP_BELOW, BEFORE_HIT, NORMAL_ATTACK,
    HEAL, RECEIVE_HEAL, OVERHEAL, HP_ABOVE, BUFF_APPLIED, BUFF_REMOVED,
    STACK_CHANGED, STACK_REACHED, COMBAT_START, COMBAT_END, ENTER_FIELD, LEAVE_FIELD, TICK, BEFORE_HEAL,
    MOVE_START, MOVE_TICK, MOVE_HIT, MOVE_END;
    public static TriggerEvent parse(String name) {
        return switch(name.toUpperCase(java.util.Locale.ROOT)) {
            case "BEFORE_DAMAGE" -> BEFORE_HIT;
            case "AFTER_DAMAGE" -> HIT;
            case "AFTER_HEAL" -> HEAL;
            default -> valueOf(name.toUpperCase(java.util.Locale.ROOT));
        };
    }
}
