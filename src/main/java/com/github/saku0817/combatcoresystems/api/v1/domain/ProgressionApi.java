package com.github.saku0817.combatcoresystems.api.v1.domain;
import java.util.UUID;
public interface ProgressionApi { boolean addExperience(UUID player, long amount); boolean rebirth(UUID player); long requiredExperience(int level); }
