package com.github.saku0817.combatcoresystems.api.v1.player.dto;

import java.util.Map;
import java.util.UUID;

public record PlayerStatsDto(UUID playerId, int level, int rebirthCount, double maxHp, double attack,
                             double defense, Map<String, Double> advanced) {}
