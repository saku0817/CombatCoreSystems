package com.github.saku0817.combatcoresystems.api.v1.domain;
import com.github.saku0817.combatcoresystems.model.EquipmentSlot;
import java.util.Map;
import java.util.UUID;
public interface EquipmentApi { Map<EquipmentSlot, String> equipped(UUID player); boolean equipMainHand(UUID player, EquipmentSlot slot); }
