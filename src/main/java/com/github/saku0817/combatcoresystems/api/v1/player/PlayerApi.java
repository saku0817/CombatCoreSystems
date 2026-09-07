package com.github.saku0817.combatcoresystems.api.v1.player;

import com.github.saku0817.combatcoresystems.api.v1.player.dto.PlayerStatsDto;

import java.util.Optional;
import java.util.UUID;

public interface PlayerApi {
    Optional<PlayerStatsDto> stats(UUID playerId);
    boolean isInCombat(UUID playerId);
}
