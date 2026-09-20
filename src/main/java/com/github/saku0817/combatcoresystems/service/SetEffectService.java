package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.event.AfterDamageEvent;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class SetEffectService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final BuffService buffs;
    private DefinitionRegistry.Snapshot snapshot;
    private final Map<String, List<SetTrigger>> rules = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Set<String>> hpActive = new HashMap<>();
    public SetEffectService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats, BuffService buffs) {
        this.plugin = plugin; this.definitions = definitions; this.players = players; this.stats = stats; this.buffs = buffs;
    }
    public static Map<String, Integer> counts(DefinitionRegistry.Snapshot snapshot, PlayerData data) {
        Map<String, Integer> counts = new HashMap<>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.RESONANCE)) {
            ItemInstance item = data.getEquipment().get(slot);
            EquipmentDefinition definition = item == null ? null : snapshot.equipment().get(item.getDefinitionId());
            if (definition != null && !definition.setId().isBlank()) counts.merge(definition.setId(), 1, Integer::sum);
        }
        return counts;
    }
    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, () -> {
        refresh();
        if (rules.values().stream().flatMap(Collection::stream).noneMatch(r -> r.event() == SetTrigger.Event.HP_BELOW)) return;
        for (Player player : Bukkit.getOnlinePlayers()) fire(player, SetTrigger.Event.HP_BELOW, null);
    }, 5, 5); }
    private void refresh() {
        if (snapshot == definitions.snapshot()) return;
        snapshot = definitions.snapshot(); rules.clear(); hpActive.clear();
        var root = snapshot.config("sets.yml").getConfigurationSection("sets");
        if (root != null) for (String id : root.getKeys(false)) for (String tier : List.of("two-piece", "four-piece")) {
            try { rules.put(id + ":" + tier, SetTrigger.parse(root.getConfigurationSection(id + "." + tier + ".triggers"), snapshot.buffs().keySet())); }
            catch (IllegalArgumentException ignored) { /* Already reported by DefinitionRegistry; omit invalid initial definitions. */ }
        }
    }
    public void fire(Player player, SetTrigger.Event event, LivingEntity other) {
        refresh();
        PlayerData data = players.find(player.getUniqueId()).orElse(null);
        if (data == null || player.isDead()) return;
        Set<String> active = hpActive.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        Set<String> seen = new HashSet<>();
        for (var set : counts(snapshot, data).entrySet()) for (String tier : List.of("two-piece", "four-piece")) {
            if (set.getValue() < (tier.equals("two-piece") ? 2 : 4)) continue;
            String prefix = set.getKey() + ":" + tier;
            for (SetTrigger rule : rules.getOrDefault(prefix, List.of())) {
                if (rule.event() != event) continue;
                String key = prefix + ":" + rule.id();
                if (event == SetTrigger.Event.HP_BELOW) {
                    if (data.getHealth() / Math.max(1, stats.get(player, data).maxHp()) > rule.hpBelow()) continue;
                    seen.add(key);
                    if (!active.add(key)) continue;
                }
                LivingEntity target = rule.target() == SetTrigger.Target.SELF ? player : other;
                if (target == null || target.isDead()) continue;
                long now = System.currentTimeMillis();
                Map<String, Long> timers = cooldowns.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
                if (timers.getOrDefault(key, 0L) > now) continue;
                timers.put(key, now + rule.cooldownMillis());
                for (String effect : rule.effects()) buffs.apply(target, effect, player.getUniqueId());
            }
        }
        if (event == SetTrigger.Event.HP_BELOW) active.retainAll(seen);
    }
    @EventHandler public void onDamage(AfterDamageEvent event) {
        if (!event.getResult().applied() || event.getResult().finalDamage() <= 0) return;
        refresh();
        if (rules.values().stream().flatMap(Collection::stream).noneMatch(rule -> rule.event() == SetTrigger.Event.HIT || rule.event() == SetTrigger.Event.TAKE_DAMAGE)) return;
        var request = event.getRequest();
        var attacker = request.attacker() == null ? null : Bukkit.getEntity(request.attacker());
        var target = Bukkit.getEntity(request.target());
        // DOT callbacks may run while BuffService iterates effects. Apply new effects on the next tick.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (attacker instanceof Player player && player.isOnline() && target instanceof LivingEntity other && !player.equals(other)) fire(player, SetTrigger.Event.HIT, other);
            if (target instanceof Player player && player.isOnline()) fire(player, SetTrigger.Event.TAKE_DAMAGE, attacker instanceof LivingEntity other ? other : null);
        });
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { cooldowns.remove(event.getPlayer().getUniqueId()); hpActive.remove(event.getPlayer().getUniqueId()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnvironmentDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent || event.getFinalDamage() <= 0 || !(event.getEntity() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> { if (player.isOnline()) fire(player, SetTrigger.Event.TAKE_DAMAGE, null); });
    }
}
