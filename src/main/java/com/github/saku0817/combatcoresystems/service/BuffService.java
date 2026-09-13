package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.BuffDefinition;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.TimedEffect;
import com.github.saku0817.combatcoresystems.model.StatKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BuffService {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final DamageService damage;
    private final HealService healing;
    private final Map<String, Long> nextTicks = new ConcurrentHashMap<>();
    private final Map<UUID, List<TimedEffect>> entityEffects = new HashMap<>();

    public BuffService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                       DamageService damage, HealService healing) {
        this.plugin = plugin; this.definitions = definitions; this.players = players; this.stats = stats;
        this.damage = damage; this.healing = healing;
    }

    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L); }

    public boolean apply(Player target, String id, UUID source) {
        return apply((LivingEntity) target, id, source);
    }

    public boolean apply(LivingEntity target, String id, UUID source) {
        BuffDefinition definition = definitions.snapshot().buffs().get(id);
        if (definition == null || target.isDead()) return false;
        List<TimedEffect> effects;
        if (target instanceof Player player) {
            PlayerData data = players.require(player);
            effects = definition.kind() == BuffDefinition.Kind.BUFF ? data.getBuffs() : data.getDebuffs();
        } else effects = entityEffects.computeIfAbsent(target.getUniqueId(), ignored -> new ArrayList<>());
        TimedEffect existing = effects.stream().filter(effect -> effect.getId().equals(id)).findFirst().orElse(null);
        long duration = (long) (definition.durationSeconds() * 1000);
        if (existing == null) effects.add(new TimedEffect(id, source, 1, duration, definition.permanent()));
        else switch (definition.reapply()) {
            case REFRESH -> existing.setRemainingMillis(duration);
            case STACK -> { existing.setStacks(Math.min(definition.maxStacks(), existing.getStacks() + 1)); existing.setRemainingMillis(duration); }
            case OVERWRITE -> { existing.setStacks(1); existing.setRemainingMillis(duration); }
            case CUSTOM -> { return false; }
        }
        stats.invalidate(target.getUniqueId());
        return true;
    }

    public double modifier(LivingEntity target, StatKey key) {
        List<TimedEffect> effects;
        if (target instanceof Player player) {
            PlayerData data = players.find(player.getUniqueId()).orElse(null);
            if (data == null) return 0;
            effects = java.util.stream.Stream.concat(data.getBuffs().stream(), data.getDebuffs().stream()).toList();
        } else effects = entityEffects.getOrDefault(target.getUniqueId(), List.of());
        double result = 0;
        for (TimedEffect effect : effects) {
            BuffDefinition definition = definitions.snapshot().buffs().get(effect.getId());
            if (definition != null && (effect.isPermanent() || effect.getRemainingMillis() > 0))
                result += (definition.flatModifiers().getOrDefault(key, 0.0) + definition.percentModifiers().getOrDefault(key, 0.0)) * effect.getStacks();
        }
        return result;
    }

    public boolean remove(Player target, String id) {
        PlayerData data = players.require(target);
        boolean removed = data.getBuffs().removeIf(e -> e.getId().equals(id)) | data.getDebuffs().removeIf(e -> e.getId().equals(id));
        if (removed) stats.invalidate(target.getUniqueId());
        return removed;
    }

    public void onDeath(Player player) {
        PlayerData data = players.require(player);
        data.getBuffs().removeIf(effect -> !effect.isPermanent());
        data.getDebuffs().removeIf(effect -> !effect.isPermanent());
        stats.invalidate(player.getUniqueId());
    }

    private void tick() {
        long elapsed = 250;
        var entities = entityEffects.entrySet().iterator();
        while (entities.hasNext()) {
            var entry = entities.next();
            Entity raw = Bukkit.getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                String prefix = entry.getKey() + ":"; nextTicks.keySet().removeIf(key -> key.startsWith(prefix));
                entities.remove(); continue;
            }
            updateList(living, entry.getValue(), elapsed);
            if (entry.getValue().isEmpty()) entities.remove();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = players.find(player.getUniqueId()).orElse(null);
            if (data == null) continue;
            boolean changed = updateList(player, data.getBuffs(), elapsed);
            changed |= updateList(player, data.getDebuffs(), elapsed);
            if (changed) stats.invalidate(player.getUniqueId());
        }
    }

    private boolean updateList(LivingEntity target, List<TimedEffect> effects, long elapsed) {
        boolean changed = false;
        Iterator<TimedEffect> iterator = effects.iterator();
        while (iterator.hasNext()) {
            TimedEffect effect = iterator.next();
            BuffDefinition definition = definitions.snapshot().buffs().get(effect.getId());
            if (definition == null) { nextTicks.remove(target.getUniqueId() + ":" + effect.getId()); iterator.remove(); changed = true; continue; }
            if (!effect.isPermanent()) {
                effect.setRemainingMillis(Math.max(0, effect.getRemainingMillis() - elapsed));
                if (effect.getRemainingMillis() == 0) { nextTicks.remove(target.getUniqueId() + ":" + effect.getId()); iterator.remove(); changed = true; continue; }
            }
            if (definition.tickEffect() != null) runTickEffect(target, effect, definition);
        }
        return changed;
    }

    private void runTickEffect(LivingEntity target, TimedEffect effect, BuffDefinition definition) {
        String key = target.getUniqueId() + ":" + effect.getId();
        long now = System.currentTimeMillis();
        if (nextTicks.getOrDefault(key, 0L) > now) return;
        BuffDefinition.TickEffect tick = definition.tickEffect();
        nextTicks.put(key, now + (long) (tick.intervalSeconds() * 1000));
        LivingEntity source = target;
        try {
            Entity resolved = effect.getSource().isBlank() ? null : Bukkit.getEntity(UUID.fromString(effect.getSource()));
            if (resolved instanceof LivingEntity living) source = living;
        } catch (IllegalArgumentException ignored) {}
        if (tick.healing()) healing.heal(source, target, tick.referenceStat(), tick.multiplier() * effect.getStacks(), true);
        else damage.apply(new DamageRequest(source.getUniqueId(), target.getUniqueId(), tick.referenceStat(),
                tick.multiplier() * effect.getStacks(), tick.element(), tick.critical(), tick.fixed(),
                tick.fixed() ? tick.multiplier() * effect.getStacks() : 0, "effect:" + effect.getId()));
    }
}
