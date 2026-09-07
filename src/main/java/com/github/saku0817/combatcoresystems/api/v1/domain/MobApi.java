package com.github.saku0817.combatcoresystems.api.v1.domain;
import org.bukkit.Location;
import java.util.Optional;
import java.util.UUID;
public interface MobApi { Optional<UUID> spawn(String definitionId, boolean boss, Location location); }
