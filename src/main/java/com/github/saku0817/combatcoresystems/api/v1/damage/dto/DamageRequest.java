package com.github.saku0817.combatcoresystems.api.v1.damage.dto;

import com.github.saku0817.combatcoresystems.model.Element;
import com.github.saku0817.combatcoresystems.model.ReferenceStat;

import java.util.UUID;

public record DamageRequest(UUID attacker, UUID target, ReferenceStat referenceStat, double multiplier,
                            Element element, boolean canCritical, boolean fixedDamage, double fixedAmount,
                            String source) {
    public DamageRequest {
        if (target == null) throw new IllegalArgumentException("target is required");
        referenceStat = referenceStat == null ? ReferenceStat.ATK : referenceStat;
        element = element == null ? Element.PHYSICAL : element;
        multiplier = Math.max(0, multiplier);
        fixedAmount = Math.max(0, fixedAmount);
        source = source == null ? "external" : source;
    }
}
