package com.github.saku0817.combatcoresystems.api.v1.domain;
import com.github.saku0817.combatcoresystems.model.ReferenceStat;
import java.util.UUID;
public interface HealApi { long heal(UUID source, UUID target, ReferenceStat reference, double multiplier); }
