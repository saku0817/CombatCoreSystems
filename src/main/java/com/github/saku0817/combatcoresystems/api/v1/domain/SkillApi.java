package com.github.saku0817.combatcoresystems.api.v1.domain;
import java.util.UUID;
public interface SkillApi { boolean activate(UUID player, UUID target, boolean ultimate); Status status(UUID player, boolean ultimate); record Status(boolean ready, double remainingSeconds, int charges) {} }
