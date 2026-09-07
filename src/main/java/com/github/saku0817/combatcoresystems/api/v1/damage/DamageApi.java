package com.github.saku0817.combatcoresystems.api.v1.damage;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageResult;

public interface DamageApi {
    DamageResult apply(DamageRequest request);
}
