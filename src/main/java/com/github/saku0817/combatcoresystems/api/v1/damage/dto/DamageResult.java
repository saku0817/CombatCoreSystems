package com.github.saku0817.combatcoresystems.api.v1.damage.dto;

import com.github.saku0817.combatcoresystems.model.Element;

public record DamageResult(boolean applied, long finalDamage, boolean critical, Element element,
                           double baseDamage, double defenseCoefficient, double resistance,
                           String source, String failureReason) {
    public static DamageResult failed(String source, String reason) {
        return new DamageResult(false, 0, false, Element.PHYSICAL, 0, 1, 0, source, reason);
    }
}
