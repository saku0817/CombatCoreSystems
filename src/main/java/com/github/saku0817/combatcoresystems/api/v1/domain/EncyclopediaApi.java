package com.github.saku0817.combatcoresystems.api.v1.domain;
import java.util.Set;
import java.util.UUID;
public interface EncyclopediaApi { boolean discovered(UUID player, boolean boss, String definitionId); Set<String> discovered(UUID player, boolean boss); boolean setDiscovered(UUID player, boolean boss, String definitionId, boolean value); }
