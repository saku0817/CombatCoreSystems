package com.github.saku0817.combatcoresystems.api.v1;

import com.github.saku0817.combatcoresystems.api.v1.damage.DamageApi;
import com.github.saku0817.combatcoresystems.api.v1.domain.*;
import com.github.saku0817.combatcoresystems.api.v1.party.PartyApi;
import com.github.saku0817.combatcoresystems.api.v1.player.PlayerApi;

public interface CombatCoreApi {
    PlayerApi players();
    DamageApi damage();
    PartyApi parties();
    CombatApi combat();
    HealApi healing();
    ElementApi elements();
    BuffApi buffs();
    EquipmentApi equipment();
    SkillApi skills();
    ProgressionApi progression();
    MobApi mobs();
    EncyclopediaApi encyclopedia();
}
