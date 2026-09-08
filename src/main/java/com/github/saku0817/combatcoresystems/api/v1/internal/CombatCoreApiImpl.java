package com.github.saku0817.combatcoresystems.api.v1.internal;

import com.github.saku0817.combatcoresystems.api.v1.CombatCoreApi;
import com.github.saku0817.combatcoresystems.api.v1.damage.DamageApi;
import com.github.saku0817.combatcoresystems.api.v1.domain.*;
import com.github.saku0817.combatcoresystems.api.v1.party.PartyApi;
import com.github.saku0817.combatcoresystems.api.v1.player.PlayerApi;
import com.github.saku0817.combatcoresystems.api.v1.player.dto.PlayerStatsDto;
import com.github.saku0817.combatcoresystems.model.PlayerStats;
import com.github.saku0817.combatcoresystems.service.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

public final class CombatCoreApiImpl implements CombatCoreApi, PlayerApi, CombatApi, HealApi, ElementApi, BuffApi,
        EquipmentApi, SkillApi, ProgressionApi, MobApi, EncyclopediaApi {
    private final PlayerDataService players;
    private final StatService stats;
    private final CombatStateService combat;
    private final DamageApi damage;
    private final PartyApi parties;
    private final HealService healing;
    private final ElementService elements;
    private final BuffService buffs;
    private final EquipmentService equipment;
    private final SkillService skills;
    private final LevelService progression;
    private final MobService mobs;
    private final EncyclopediaService encyclopedia;

    public CombatCoreApiImpl(PlayerDataService players, StatService stats, CombatStateService combat, DamageApi damage, PartyApi parties,
                             HealService healing, ElementService elements, BuffService buffs, EquipmentService equipment,
                             SkillService skills, LevelService progression, MobService mobs, EncyclopediaService encyclopedia) {
        this.players = players; this.stats = stats; this.combat = combat; this.damage = damage; this.parties = parties;
        this.healing = healing; this.elements = elements; this.buffs = buffs; this.equipment = equipment; this.skills = skills;
        this.progression = progression; this.mobs = mobs; this.encyclopedia = encyclopedia;
    }
    @Override public PlayerApi players() { return this; }
    @Override public DamageApi damage() { return damage; }
    @Override public PartyApi parties() { return parties; }
    @Override public CombatApi combat() { return this; }
    @Override public HealApi healing() { return this; }
    @Override public ElementApi elements() { return this; }
    @Override public BuffApi buffs() { return this; }
    @Override public EquipmentApi equipment() { return this; }
    @Override public SkillApi skills() { return this; }
    @Override public ProgressionApi progression() { return this; }
    @Override public MobApi mobs() { return this; }
    @Override public EncyclopediaApi encyclopedia() { return this; }
    @Override public Optional<PlayerStatsDto> stats(UUID playerId) {
        var player = Bukkit.getPlayer(playerId); var data = players.find(playerId);
        if (player == null || data.isEmpty()) return Optional.empty();
        PlayerStats value = stats.get(player, data.get());
        LinkedHashMap<String, Double> advanced = new LinkedHashMap<>(); value.advanced().forEach((key, amount) -> advanced.put(key.name(), amount));
        return Optional.of(new PlayerStatsDto(playerId, data.get().getLevel(), data.get().getRebirthCount(), value.maxHp(), value.atk(), value.def(), advanced));
    }
    @Override public boolean isInCombat(UUID playerId) { return combat.inCombat(playerId); }
    @Override public boolean active(UUID entity) { return combat.inCombat(entity); }
    @Override public long remainingMillis(UUID entity) { return combat.remainingMillis(entity); }
    @Override public long heal(UUID source, UUID target, com.github.saku0817.combatcoresystems.model.ReferenceStat reference, double multiplier) {
        var sourceEntity = Bukkit.getEntity(source); var targetEntity = Bukkit.getEntity(target);
        if (!(sourceEntity instanceof LivingEntity from) || !(targetEntity instanceof LivingEntity to)) return 0;
        return healing.heal(from, to, reference, multiplier, false);
    }
    @Override public boolean attach(UUID target, com.github.saku0817.combatcoresystems.model.Element element, double seconds) {
        var entity = Bukkit.getEntity(target); if (!(entity instanceof LivingEntity living)) return false; elements.attach(living, element, seconds); return true;
    }
    @Override public boolean remove(UUID target, com.github.saku0817.combatcoresystems.model.Element element) { return elements.remove(target, element); }
    @Override public java.util.Map<com.github.saku0817.combatcoresystems.model.Element, Long> remaining(UUID target) { return elements.remaining(target); }
    @Override public boolean apply(UUID target, String definitionId, UUID source) { Player player = Bukkit.getPlayer(target); return player != null && buffs.apply(player, definitionId, source); }
    @Override public boolean remove(UUID target, String definitionId) { Player player = Bukkit.getPlayer(target); return player != null && buffs.remove(player, definitionId); }
    @Override public java.util.Map<com.github.saku0817.combatcoresystems.model.EquipmentSlot, String> equipped(UUID playerId) {
        var data = players.find(playerId); if (data.isEmpty()) return java.util.Map.of();
        java.util.EnumMap<com.github.saku0817.combatcoresystems.model.EquipmentSlot, String> result = new java.util.EnumMap<>(com.github.saku0817.combatcoresystems.model.EquipmentSlot.class);
        data.get().getEquipment().forEach((slot, item) -> result.put(slot, item.getDefinitionId())); return java.util.Map.copyOf(result);
    }
    @Deprecated @Override public boolean equipMainHand(UUID playerId, com.github.saku0817.combatcoresystems.model.EquipmentSlot slot) { return false; }
    @Override public boolean activate(UUID playerId, UUID targetId, boolean ultimate) { Player player = Bukkit.getPlayer(playerId); var entity = targetId == null ? null : Bukkit.getEntity(targetId); return player != null && skills.activate(player, entity instanceof LivingEntity living ? living : null, ultimate); }
    @Override public SkillApi.Status status(UUID player, boolean ultimate) { var state = skills.status(player, ultimate); return new SkillApi.Status(state.ready(), state.remainingSeconds(), state.charges()); }
    @Override public boolean addExperience(UUID playerId, long amount) { Player player = Bukkit.getPlayer(playerId); var data = players.find(playerId); if (player == null || data.isEmpty()) return false; progression.addExp(player, data.get(), amount); return true; }
    @Override public boolean rebirth(UUID playerId) { Player player = Bukkit.getPlayer(playerId); var data = players.find(playerId); return player != null && data.isPresent() && progression.rebirth(player, data.get()); }
    @Override public long requiredExperience(int level) { return progression.requiredExp(level); }
    @Override public Optional<UUID> spawn(String definitionId, boolean boss, Location location) { return mobs.spawn(definitionId, boss, location, false).map(org.bukkit.entity.Entity::getUniqueId); }
    @Override public boolean discovered(UUID player, boolean boss, String definitionId) { return players.find(player).map(data -> (boss ? data.getDiscoveredBosses() : data.getDiscoveredMobs()).contains(definitionId)).orElse(false); }
    @Override public java.util.Set<String> discovered(UUID player, boolean boss) { return players.find(player).map(data -> java.util.Set.copyOf(boss ? data.getDiscoveredBosses() : data.getDiscoveredMobs())).orElse(java.util.Set.of()); }
    @Override public boolean setDiscovered(UUID player, boolean boss, String definitionId, boolean value) { return players.find(player).map(data -> encyclopedia.set(data, boss, definitionId, value)).orElse(false); }
}
