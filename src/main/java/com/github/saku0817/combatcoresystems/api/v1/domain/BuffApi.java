package com.github.saku0817.combatcoresystems.api.v1.domain;
import java.util.UUID;
public interface BuffApi { boolean apply(UUID target, String definitionId, UUID source); boolean remove(UUID target, String definitionId); }
