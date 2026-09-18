package com.github.saku0817.combatcoresystems.model;

import java.util.List;
import java.util.Map;

/** Immutable, validated optional weapon features. Legacy definitions retain their behavior. */
public record WeaponOptions(Element normalElement, Talent talent, Visual visual) {
    public static final WeaponOptions EMPTY = new WeaponOptions(Element.PHYSICAL, null, Visual.NONE);
    public enum Hand {
        MAIN_HAND, OFF_HAND, EITHER_HAND, HOT_BAR, INVENTORY;
        public boolean includes(int slot, int selected) {
            return switch (this) {
                case MAIN_HAND -> slot == selected;
                case OFF_HAND -> slot == 40;
                case EITHER_HAND -> slot == selected || slot == 40;
                case HOT_BAR -> slot >= 0 && slot <= 8;
                case INVENTORY -> slot >= 0 && slot <= 35 || slot == 40;
            };
        }
    }
    public record Talent(String name, List<String> description, Hand hand, double multiplier,
                         Map<StatKey, Double> modifiers) {}
    public record Component(ReferenceStat reference, double multiplier, Element bonusElement) {}
    public record Ability(List<String> description, boolean damageEnabled, double currentHpCost,
                          List<Component> components, List<String> selfEffects, List<String> targetEffects, Visual visual) {
        public static final Ability EMPTY = new Ability(List.of(), true, 0, List.of(), List.of(), List.of(), Visual.NONE);
    }
    /** Only data-free particles are accepted so malformed options cannot throw during combat. */
    public record Visual(String particle, int count, double spread, String sound, float volume, float pitch) {
        public static final Visual NONE = new Visual("", 0, 0, "", 1, 1);
    }
}
