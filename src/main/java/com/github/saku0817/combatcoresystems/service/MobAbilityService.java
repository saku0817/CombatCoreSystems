package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.api.v1.damage.dto.DamageRequest;
import com.github.saku0817.combatcoresystems.api.v1.event.BossPhaseChangeEvent;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.Element;
import com.github.saku0817.combatcoresystems.model.MobDefinition;
import com.github.saku0817.combatcoresystems.model.ReferenceStat;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class MobAbilityService implements Listener {
    private final JavaPlugin plugin; private final DefinitionRegistry definitions; private final MobService mobs;
    private final DamageService damage; private final ItemService items; private final MiniMessage mini = MiniMessage.miniMessage();
    private final Map<String, Long> cooldowns = new HashMap<>(); private final Map<UUID, String> phases = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>(); private final List<Respawn> respawns = new ArrayList<>();

    public MobAbilityService(JavaPlugin plugin, DefinitionRegistry definitions, MobService mobs, DamageService damage, ItemService items) {
        this.plugin = plugin; this.definitions = definitions; this.mobs = mobs; this.damage = damage; this.items = items;
    }
    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L); }

    @EventHandler public void onChunkLoad(ChunkLoadEvent event) { for (Entity entity : event.getChunk().getEntities()) if (entity instanceof LivingEntity living) mobs.track(living); }
    @EventHandler public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity(); MobDefinition definition = mobs.definition(entity).orElse(null); if (definition == null) return;
        drops(event, definition); mobs.untrack(entity.getUniqueId()); phases.remove(entity.getUniqueId()); BossBar bar = bars.remove(entity.getUniqueId());
        if (bar != null) Bukkit.getOnlinePlayers().forEach(player -> player.hideBossBar(bar));
        ConfigurationSection section = definitionSection(definition);
        if (definition.boss() && section.getBoolean("respawn.enabled", false)) respawns.add(new Respawn(definition.id(), entity.getLocation(), System.currentTimeMillis() + (long) (section.getDouble("respawn.cooldown-seconds", 300) * 1000)));
    }

    private void tick() {
        for (UUID uuid : mobs.managedIds()) {
            Entity raw = Bukkit.getEntity(uuid); if (!(raw instanceof LivingEntity entity) || entity.isDead()) continue;
            MobDefinition definition = mobs.definition(entity).orElse(null); if (definition == null) continue;
            ConfigurationSection source = definitionSection(definition); ConfigurationSection phase = definition.boss() ? updatePhase(entity, definition, source) : null;
            runSkills(entity, phase != null && phase.getConfigurationSection("skills") != null ? phase.getConfigurationSection("skills") : source.getConfigurationSection("skills"));
            if (definition.boss()) updateBossBar(entity, definition);
        }
        long now = System.currentTimeMillis(); Iterator<Respawn> iterator = respawns.iterator(); while (iterator.hasNext()) { Respawn value = iterator.next(); if (value.at > now) continue; mobs.spawn(value.id, true, value.location, false); iterator.remove(); }
    }

    private ConfigurationSection updatePhase(LivingEntity entity, MobDefinition definition, ConfigurationSection source) {
        ConfigurationSection phaseRoot = source.getConfigurationSection("phases"); if (phaseRoot == null) return null;
        double maximum = entity.getAttribute(Attribute.MAX_HEALTH) == null ? entity.getHealth() : entity.getAttribute(Attribute.MAX_HEALTH).getValue();
        double ratio = entity.getHealth() / Math.max(1, maximum); String selectedId = ""; ConfigurationSection selected = null;
        List<String> keys = new ArrayList<>(phaseRoot.getKeys(false)); keys.sort(Comparator.comparingDouble(id -> phaseRoot.getDouble(id + ".hp-at-or-below", 1)).reversed());
        for (String id : keys) if (ratio <= phaseRoot.getDouble(id + ".hp-at-or-below", 1)) { selectedId = id; selected = phaseRoot.getConfigurationSection(id); }
        String old = phases.getOrDefault(entity.getUniqueId(), "");
        if (!selectedId.equals(old)) {
            phases.put(entity.getUniqueId(), selectedId); double attack = selected == null ? 1 : selected.getDouble("stats.attack-multiplier", 1); double defense = selected == null ? 1 : selected.getDouble("stats.defense-multiplier", 1);
            mobs.setPhaseModifiers(entity, attack, defense); Bukkit.getPluginManager().callEvent(new BossPhaseChangeEvent(entity.getUniqueId(), definition.id(), old, selectedId));
        }
        return selected;
    }

    private void runSkills(LivingEntity source, ConfigurationSection skills) {
        if (skills == null) return;
        for (String id : skills.getKeys(false)) {
            ConfigurationSection skill = skills.getConfigurationSection(id); if (skill == null) continue;
            String key = source.getUniqueId() + ":" + id; long now = System.currentTimeMillis(); if (cooldowns.getOrDefault(key, 0L) > now) continue;
            double range = skill.getDouble("range", 16); Player target = source.getWorld().getNearbyPlayers(source.getLocation(), range).stream().min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(source.getLocation()))).orElse(null);
            if (target == null) continue;
            ReferenceStat reference; try { reference = ReferenceStat.valueOf(skill.getString("reference", "ATK").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException ex) { reference = ReferenceStat.ATK; }
            damage.apply(new DamageRequest(source.getUniqueId(), target.getUniqueId(), reference, skill.getDouble("multiplier", 1), Element.parse(skill.getString("element")).orElse(Element.PHYSICAL), skill.getBoolean("critical", true), skill.getBoolean("fixed", false), skill.getDouble("fixed-amount", 0), "mob_skill:" + id));
            cooldowns.put(key, now + (long) (skill.getDouble("cooldown", 10) * 1000));
        }
    }

    private void updateBossBar(LivingEntity entity, MobDefinition definition) {
        BossBar bar = bars.computeIfAbsent(entity.getUniqueId(), ignored -> BossBar.bossBar(mini.deserialize(definition.name()), 1, BossBar.Color.RED, BossBar.Overlay.PROGRESS));
        double max = entity.getAttribute(Attribute.MAX_HEALTH) == null ? entity.getHealth() : entity.getAttribute(Attribute.MAX_HEALTH).getValue(); bar.progress((float) Math.max(0, Math.min(1, entity.getHealth() / Math.max(1, max))));
        for (Player player : Bukkit.getOnlinePlayers()) { boolean nearby = player.getWorld().equals(entity.getWorld()) && player.getLocation().distanceSquared(entity.getLocation()) <= 64 * 64; if (nearby) player.showBossBar(bar); else player.hideBossBar(bar); }
    }

    private void drops(EntityDeathEvent event, MobDefinition definition) {
        ConfigurationSection drops = definitionSection(definition).getConfigurationSection("drops"); if (drops == null) return;
        for (String key : drops.getKeys(false)) { ConfigurationSection drop = drops.getConfigurationSection(key); if (drop == null || ThreadLocalRandom.current().nextDouble() > drop.getDouble("chance", 1)) continue; items.create(drop.getString("item"), Math.max(1, drop.getInt("amount", 1))).ifPresent(event.getDrops()::add); }
    }
    private ConfigurationSection definitionSection(MobDefinition definition) { return definitions.snapshot().config(definition.boss() ? "bosses.yml" : "mobs.yml").getConfigurationSection((definition.boss() ? "bosses." : "mobs.") + definition.id()); }
    private record Respawn(String id, Location location, long at) {}
}
