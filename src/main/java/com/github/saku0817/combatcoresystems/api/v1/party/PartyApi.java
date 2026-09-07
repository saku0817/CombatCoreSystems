package com.github.saku0817.combatcoresystems.api.v1.party;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartyApi {
    Optional<String> partyId(UUID player);
    List<UUID> members(String partyId);
    boolean sameParty(UUID first, UUID second);
}
