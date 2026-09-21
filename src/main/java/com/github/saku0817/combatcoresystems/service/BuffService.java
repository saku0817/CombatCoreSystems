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
    private final Map<UUID, Map<String, TimedEffect>> ownedEffects = new HashMap<>();
    private long lastOrder;
    private long order() { return lastOrder=Math.max(System.currentTimeMillis()*1000,lastOrder+1); }
    private java.util.function.BiConsumer<LivingEntity, String> applied = (target,id) -> {};
    private java.util.function.BiConsumer<LivingEntity, String> removed = (target,id) -> {};
    public void bindEvents(java.util.function.BiConsumer<LivingEntity,String> applied, java.util.function.BiConsumer<LivingEntity,String> removed) {
        this.applied=applied; this.removed=removed;
    }
    public List<TimedEffect> effects(LivingEntity target) {
        List<TimedEffect> result=new ArrayList<>();
        if (target instanceof Player p) players.find(p.getUniqueId()).ifPresent(data -> { result.addAll(data.getBuffs()); result.addAll(data.getDebuffs()); });
        else result.addAll(entityEffects.getOrDefault(target.getUniqueId(),List.of()));
        result.addAll(ownedEffects.getOrDefault(target.getUniqueId(),Map.of()).values());
        result.sort(Comparator.comparingLong(TimedEffect::getAppliedOrder));
        return List.copyOf(result);
    }
    public boolean has(LivingEntity target,String id) { return effects(target).stream().anyMatch(e -> e.getId().equals(id) && (e.isPermanent() || e.getRemainingMillis()>0)); }
    public void acquire(LivingEntity target,String lease,String id,UUID source) {
        if (!definitions.snapshot().buffs().containsKey(id) || target.isDead()) return;
        var values=ownedEffects.computeIfAbsent(target.getUniqueId(),ignored -> new LinkedHashMap<>());
        TimedEffect effect=new TimedEffect(id,source,1,0,true); effect.setAppliedOrder(order());
        if (values.putIfAbsent(lease,effect)==null) {
            stats.invalidate(target.getUniqueId()); applied.accept(target,id);
        }
    }
    public void release(UUID target,String lease) {
        var values=ownedEffects.get(target); if (values==null) return;
        TimedEffect old=values.remove(lease);
        if (values.isEmpty()) ownedEffects.remove(target);
        if (old!=null) { stats.invalidate(target); if (Bukkit.getEntity(target) instanceof LivingEntity entity) removed.accept(entity,old.getId()); }
    }
    public boolean remove(LivingEntity target,String id) {
        if (target instanceof Player p) return remove(p,id);
        boolean changed=entityEffects.getOrDefault(target.getUniqueId(),new ArrayList<>()).removeIf(e -> e.getId().equals(id));
        if (changed) { stats.invalidate(target.getUniqueId()); removed.accept(target,id); }
        return changed;
    }
    public void refresh(LivingEntity target,String id,UUID source) {
        BuffDefinition definition=definitions.snapshot().buffs().get(id);
        if (definition==null || target.isDead()) return;
        List<TimedEffect> effects;
        if (target instanceof Player player) {
            PlayerData data=players.require(player);
            effects=definition.kind()==BuffDefinition.Kind.BUFF ? data.getBuffs() : data.getDebuffs();
        } else effects=entityEffects.getOrDefault(target.getUniqueId(),List.of());
        TimedEffect existing=effects.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
        if (existing==null) { apply(target,id,source); return; }
        existing.setRemainingMillis((long)(definition.durationSeconds()*1000)); existing.setAppliedOrder(order());
        stats.invalidate(target.getUniqueId()); applied.accept(target,id);
    }

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
        if (existing == null) { existing=new TimedEffect(id, source, 1, duration, definition.permanent()); effects.add(existing); }
        else switch (definition.reapply()) {
            case REFRESH -> existing.setRemainingMillis(duration);
            case STACK -> { existing.setStacks(Math.min(definition.maxStacks(), existing.getStacks() + 1)); existing.setRemainingMillis(duration); }
            case OVERWRITE -> { existing.setStacks(1); existing.setRemainingMillis(duration); }
            case CUSTOM -> { return false; }
        }
        existing.setAppliedOrder(order());
        stats.invalidate(target.getUniqueId());
        applied.accept(target,id);
        return true;
    }

    public double modifier(LivingEntity target, StatKey key) {
        List<TimedEffect> effects = effects(target);
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
        if (removed) { stats.invalidate(target.getUniqueId()); this.removed.accept(target,id); }
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
        for (var entry : List.copyOf(entityEffects.entrySet())) {
            Entity raw = Bukkit.getEntity(entry.getKey());
            if (!(raw instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                String prefix = entry.getKey() + ":"; nextTicks.keySet().removeIf(key -> key.startsWith(prefix));
                entityEffects.remove(entry.getKey()); continue;
            }
            updateList(living, entry.getValue(), elapsed);
            if (entry.getValue().isEmpty()) entityEffects.remove(entry.getKey());
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = players.find(player.getUniqueId()).orElse(null);
            if (data == null) continue;
            boolean changed = updateList(player, data.getBuffs(), elapsed);
            changed |= updateList(player, data.getDebuffs(), elapsed);
            if (changed) stats.invalidate(player.getUniqueId());
        }
        for (var entry : List.copyOf(ownedEffects.entrySet())) {
            if (!(Bukkit.getEntity(entry.getKey()) instanceof LivingEntity target) || target.isDead()) continue;
            for (TimedEffect effect : List.copyOf(entry.getValue().values())) {
                BuffDefinition definition=definitions.snapshot().buffs().get(effect.getId());
                if (definition!=null && definition.tickEffect()!=null) runTickEffect(target,effect,definition);
            }
        }
    }

    private boolean updateList(LivingEntity target, List<TimedEffect> effects, long elapsed) {
        boolean changed = false;
        for (TimedEffect effect : List.copyOf(effects)) {
            if (!effects.contains(effect)) continue;
            BuffDefinition definition = definitions.snapshot().buffs().get(effect.getId());
            if (definition == null) { nextTicks.remove(target.getUniqueId() + ":" + effect.getId()); effects.remove(effect); changed = true; removed.accept(target,effect.getId()); continue; }
            if (!effect.isPermanent()) {
                effect.setRemainingMillis(Math.max(0, effect.getRemainingMillis() - elapsed));
                if (effect.getRemainingMillis() == 0) { nextTicks.remove(target.getUniqueId() + ":" + effect.getId()); effects.remove(effect); changed = true; removed.accept(target,effect.getId()); continue; }
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
