package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.RegionDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class SpawnService {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final MobService mobs;
    private final CombatStateService combat;
    private final Map<String, Set<UUID>> managed = new HashMap<>();
    private final Map<String, Long> nextSpawn = new HashMap<>();
    private final Map<UUID, Long> absentSince = new HashMap<>();

    public SpawnService(JavaPlugin plugin, DefinitionRegistry definitions, MobService mobs, CombatStateService combat) {
        this.plugin = plugin; this.definitions = definitions; this.mobs = mobs; this.combat = combat;
    }

    public void start() { Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L); }

    private void tick() {
        ConfigurationSection root = definitions.snapshot().config("spawns.yml").getConfigurationSection("spawns");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection spawn = root.getConfigurationSection(id);
            if (spawn == null || !spawn.getBoolean("enabled", true)) continue;
            RegionDefinition region = definitions.snapshot().regions().get(spawn.getString("region"));
            if (region == null) continue;
            Set<UUID> entities = managed.computeIfAbsent(id, ignored -> new HashSet<>());
            entities.removeIf(uuid -> Bukkit.getEntity(uuid) == null);
            manageDespawn(spawn, region, entities);
            long now = System.currentTimeMillis();
            if (entities.size() >= spawn.getInt("max-alive", 1) || nextSpawn.getOrDefault(id, 0L) > now) continue;
            int count = Math.min(spawn.getInt("amount", 1), spawn.getInt("max-alive", 1) - entities.size());
            for (int i = 0; i < count; i++) safeLocation(region).flatMap(location -> mobs.spawn(spawn.getString("definition"), spawn.getBoolean("boss"), location, true))
                    .ifPresent(entity -> entities.add(entity.getUniqueId()));
            nextSpawn.put(id, now + (long) (spawn.getDouble("interval-seconds", 60) * 1000));
        }
    }

    private void manageDespawn(ConfigurationSection spawn, RegionDefinition region, Set<UUID> entities) {
        World world = Bukkit.getWorld(region.world());
        if (world == null) return;
        double range = spawn.getDouble("player-range", 32);
        boolean playersNearby = world.getPlayers().stream().anyMatch(player -> distanceToRegion(player, region) <= range);
        long now = System.currentTimeMillis();
        for (UUID uuid : List.copyOf(entities)) {
            Entity raw = Bukkit.getEntity(uuid);
            if (!(raw instanceof LivingEntity entity) || mobs.isBoss(entity) || combat.inCombat(uuid)) continue;
            if (playersNearby) absentSince.remove(uuid);
            else if (now - absentSince.computeIfAbsent(uuid, ignored -> now) >= (long) (spawn.getDouble("despawn-seconds", 60) * 1000)) {
                entity.remove(); entities.remove(uuid); absentSince.remove(uuid);
            }
        }
    }

    private double distanceToRegion(Player player, RegionDefinition region) {
        int x = Math.max(region.minX(), Math.min(region.maxX(), player.getLocation().getBlockX()));
        int z = Math.max(region.minZ(), Math.min(region.maxZ(), player.getLocation().getBlockZ()));
        return Math.sqrt(Math.pow(player.getLocation().getX() - x, 2) + Math.pow(player.getLocation().getZ() - z, 2));
    }

    private Optional<Location> safeLocation(RegionDefinition region) {
        World world = Bukkit.getWorld(region.world());
        if (world == null) return Optional.empty();
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(region.minX(), region.maxX() + 1);
            int z = ThreadLocalRandom.current().nextInt(region.minZ(), region.maxZ() + 1);
            int y = region.minY() == null ? world.getHighestBlockYAt(x, z) + 1 : ThreadLocalRandom.current().nextInt(region.minY(), region.maxY() + 1);
            Location value = new Location(world, x + 0.5, y, z + 0.5);
            if (value.getBlock().isPassable() && value.clone().add(0, 1, 0).getBlock().isPassable() && !value.clone().add(0, -1, 0).getBlock().isPassable()) return Optional.of(value);
        }
        return Optional.empty();
    }
}
