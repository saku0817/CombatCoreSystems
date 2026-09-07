package com.github.saku0817.combatcoresystems.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PartyData {
    private int dataVersion = PlayerData.DATA_VERSION;
    private String id = UUID.randomUUID().toString();
    private String leader = "";
    private List<Member> members = new ArrayList<>();

    public PartyData() {}

    public PartyData(UUID leader) {
        this.leader = leader.toString();
        members.add(new Member(leader.toString(), System.currentTimeMillis()));
    }

    public int getDataVersion() { return dataVersion; }
    public String getId() { return id; }
    public UUID leader() { return UUID.fromString(leader); }
    public String getLeader() { return leader; }
    public List<Member> getMembers() { return members; }
    public void setLeader(UUID leader) { this.leader = leader.toString(); }
    public boolean contains(UUID uuid) { return members.stream().anyMatch(m -> m.uuid.equals(uuid.toString())); }

    public record Member(String uuid, long joinOrder) {
        public UUID asUuid() { return UUID.fromString(uuid); }
    }
}
