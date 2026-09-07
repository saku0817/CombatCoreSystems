package com.github.saku0817.combatcoresystems.api.v1.domain;
import java.util.UUID;
public interface CombatApi { boolean active(UUID entity); long remainingMillis(UUID entity); }
