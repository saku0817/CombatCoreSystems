package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.event.CombatStateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatStateService {
    private final JavaPlugin plugin;
    private final Map<UUID, State> states = new ConcurrentHashMap<>();
    private final Map<UUID, RegisteredWeapons> registrations = new ConcurrentHashMap<>();
    private final Set<UUID> forced = ConcurrentHashMap.newKeySet();
    private volatile long durationMillis;

    public CombatStateService(JavaPlugin plugin, long durationMillis) {
        this.plugin = plugin;
        this.durationMillis = durationMillis;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::expire, 5L, 5L);
    }

    public void touch(LivingEntity entity, String activeWeaponId) {
        UUID uuid = entity.getUniqueId();
        long end = forced.contains(uuid) ? Long.MAX_VALUE : System.currentTimeMillis() + durationMillis;
        State prior = states.get(uuid);
        RegisteredWeapons registered = registrations.getOrDefault(uuid, new RegisteredWeapons("", ""));
        String active = activeWeaponId == null || activeWeaponId.isBlank() ? (prior == null ? "" : prior.activeWeapon()) : activeWeaponId;
        State old = states.put(uuid, new State(end, active, registered.melee(), registered.ranged()));
        if (old == null) Bukkit.getPluginManager().callEvent(new CombatStateEvent(uuid, true));
    }

    public void forceOn(LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        boolean wasInCombat = inCombat(uuid);
        State prior = oldState(uuid);
        forced.add(uuid);
        states.put(uuid, new State(Long.MAX_VALUE, prior.activeWeapon(), prior.meleeWeapon(), prior.rangedWeapon()));
        if (!wasInCombat) Bukkit.getPluginManager().callEvent(new CombatStateEvent(uuid, true));
    }

    public void forceOff(UUID uuid) { clear(uuid); }

    public void registerWeapons(UUID player, String melee, String ranged) {
        RegisteredWeapons registered = new RegisteredWeapons(melee == null ? "" : melee, ranged == null ? "" : ranged);
        registrations.put(player, registered);
        states.computeIfPresent(player, (ignored, old) -> new State(old.endsAt(), old.activeWeapon(), registered.melee(), registered.ranged()));
    }

    public boolean inCombat(UUID uuid) { return forced.contains(uuid) || remainingMillis(uuid) > 0; }
    public boolean isForced(UUID uuid) { return forced.contains(uuid); }
    public long remainingMillis(UUID uuid) {
        if (forced.contains(uuid)) return Long.MAX_VALUE;
        State state = states.get(uuid);
        return state == null ? 0 : Math.max(0, state.endsAt - System.currentTimeMillis());
    }
    public State state(UUID uuid) { return oldState(uuid); }

    public void clear(UUID uuid) {
        forced.remove(uuid);
        if (states.remove(uuid) != null) Bukkit.getPluginManager().callEvent(new CombatStateEvent(uuid, false));
    }
    public void forget(UUID uuid) { clear(uuid); registrations.remove(uuid); }

    private void expire() {
        long now = System.currentTimeMillis();
        states.entrySet().removeIf(entry -> {
            if (forced.contains(entry.getKey())) return false;
            if (entry.getValue().endsAt > now) return false;
            Bukkit.getPluginManager().callEvent(new CombatStateEvent(entry.getKey(), false));
            return true;
        });
    }

    private State oldState(UUID uuid) {
        State state = states.get(uuid); if (state != null) return state;
        RegisteredWeapons registered = registrations.getOrDefault(uuid, new RegisteredWeapons("", ""));
        return new State(0, "", registered.melee(), registered.ranged());
    }
    public void setDurationMillis(long durationMillis) { this.durationMillis = Math.max(0, durationMillis); }
    public record State(long endsAt, String activeWeapon, String meleeWeapon, String rangedWeapon) {}
    private record RegisteredWeapons(String melee, String ranged) {}
}
