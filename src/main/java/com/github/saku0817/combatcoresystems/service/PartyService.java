package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.party.PartyApi;
import com.github.saku0817.combatcoresystems.model.PartyData;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.storage.StorageService;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PartyService implements PartyApi {
    public static final int MAX_SIZE = 4;
    private final StorageService storage;
    private final PlayerDataService players;
    private final Map<String, PartyData> parties = new ConcurrentHashMap<>();
    private final Map<UUID, List<Invite>> invites = new ConcurrentHashMap<>();

    public PartyService(StorageService storage, PlayerDataService players) {
        this.storage = storage;
        this.players = players;
    }

    public java.util.concurrent.CompletableFuture<Void> load() { return storage.loadParties().thenAccept(values -> { parties.clear(); parties.putAll(values); }); }
    public Collection<PartyData> all() { return Collections.unmodifiableCollection(parties.values()); }

    public Optional<PartyData> create(UUID leader) {
        if (partyId(leader).isPresent()) return Optional.empty();
        PartyData party = new PartyData(leader);
        parties.put(party.getId(), party);
        players.find(leader).ifPresent(data -> data.setPartyId(party.getId()));
        return Optional.of(party);
    }

    public boolean invite(UUID leader, UUID target) {
        PartyData party = findByPlayer(leader).orElse(null);
        if (party == null || !party.leader().equals(leader) || party.getMembers().size() >= MAX_SIZE || partyId(target).isPresent()) return false;
        invites.computeIfAbsent(target, ignored -> new ArrayList<>()).removeIf(Invite::expired);
        invites.get(target).add(new Invite(leader, party.getId(), System.currentTimeMillis() + 60_000));
        return true;
    }

    public boolean accept(UUID target, UUID inviter) {
        List<Invite> received = invites.getOrDefault(target, List.of());
        Invite invite = received.stream().filter(i -> i.inviter().equals(inviter) && !i.expired()).findFirst().orElse(null);
        if (invite == null || partyId(target).isPresent()) return false;
        PartyData party = parties.get(invite.partyId());
        if (party == null || party.getMembers().size() >= MAX_SIZE) return false;
        party.getMembers().add(new PartyData.Member(target.toString(), System.currentTimeMillis()));
        players.find(target).ifPresent(data -> data.setPartyId(party.getId()));
        invites.remove(target);
        return true;
    }

    public boolean decline(UUID target, UUID inviter) {
        return invites.getOrDefault(target, new ArrayList<>()).removeIf(i -> i.inviter().equals(inviter));
    }

    public boolean leave(UUID member) {
        PartyData party = findByPlayer(member).orElse(null);
        if (party == null) return false;
        party.getMembers().removeIf(m -> m.asUuid().equals(member));
        players.find(member).ifPresent(data -> data.setPartyId(""));
        if (party.getMembers().isEmpty()) { parties.remove(party.getId()); return true; }
        if (party.leader().equals(member)) transferToOldest(party);
        return true;
    }

    public boolean kick(UUID leader, UUID target) {
        PartyData party = findByPlayer(leader).orElse(null);
        if (party == null || !party.leader().equals(leader) || leader.equals(target) || !party.contains(target)) return false;
        party.getMembers().removeIf(m -> m.asUuid().equals(target));
        players.find(target).ifPresent(data -> data.setPartyId(""));
        return true;
    }

    public boolean transfer(UUID leader, UUID target) {
        PartyData party = findByPlayer(leader).orElse(null);
        if (party == null || !party.leader().equals(leader) || !party.contains(target)) return false;
        party.setLeader(target);
        return true;
    }

    public boolean disband(UUID leader) {
        PartyData party = findByPlayer(leader).orElse(null);
        if (party == null || !party.leader().equals(leader)) return false;
        parties.remove(party.getId());
        party.getMembers().forEach(member -> players.find(member.asUuid()).ifPresent(data -> data.setPartyId("")));
        return true;
    }

    private void transferToOldest(PartyData party) {
        Comparator<PartyData.Member> byJoin = Comparator.comparingLong(PartyData.Member::joinOrder);
        PartyData.Member next = party.getMembers().stream().filter(m -> Bukkit.getPlayer(m.asUuid()) != null).min(byJoin)
                .orElseGet(() -> party.getMembers().stream().min(byJoin).orElseThrow());
        party.setLeader(next.asUuid());
    }

    public Optional<PartyData> findByPlayer(UUID uuid) {
        PlayerData data = players.find(uuid).orElse(null);
        if (data != null && !data.getPartyId().isBlank()) return Optional.ofNullable(parties.get(data.getPartyId()));
        return parties.values().stream().filter(p -> p.contains(uuid)).findFirst();
    }

    public java.util.concurrent.CompletableFuture<Void> save() { return storage.saveParties(parties.values()); }
    @Override public Optional<String> partyId(UUID player) { return findByPlayer(player).map(PartyData::getId); }
    @Override public List<UUID> members(String partyId) {
        PartyData party = parties.get(partyId);
        return party == null ? List.of() : party.getMembers().stream().map(PartyData.Member::asUuid).toList();
    }
    @Override public boolean sameParty(UUID first, UUID second) {
        Optional<String> firstParty = partyId(first);
        return firstParty.isPresent() && firstParty.equals(partyId(second));
    }

    public record Invite(UUID inviter, String partyId, long expiresAt) {
        public boolean expired() { return System.currentTimeMillis() >= expiresAt; }
    }
}
