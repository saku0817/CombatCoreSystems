package com.github.saku0817.combatcoresystems;

import com.github.saku0817.combatcoresystems.api.v1.CombatCoreApi;
import com.github.saku0817.combatcoresystems.api.v1.internal.CombatCoreApiImpl;
import com.github.saku0817.combatcoresystems.command.CcsAdminCommand;
import com.github.saku0817.combatcoresystems.command.CcsCommand;
import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.integration.CcsPlaceholderExpansion;
import com.github.saku0817.combatcoresystems.listener.PlayerLifecycleListener;
import com.github.saku0817.combatcoresystems.service.*;
import com.github.saku0817.combatcoresystems.storage.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class CombatCoreSystems extends JavaPlugin {
    private DefinitionRegistry definitions;
    private StorageService storage;
    private PlayerDataService players;
    private PartyService parties;
    private CombatCoreApi api;

    @Override public void onEnable() {
        definitions = new DefinitionRegistry(this);
        if (!definitions.loadInitial()) { getLogger().severe("Core configuration is invalid; CCS will not overwrite data."); Bukkit.getPluginManager().disablePlugin(this); return; }
        storage = new StorageService(this);
        try { storage.initialize(definitions.snapshot().config("storage.yml")); }
        catch (Exception ex) { getLogger().log(Level.SEVERE, "Could not initialize CCS storage", ex); Bukkit.getPluginManager().disablePlugin(this); return; }

        long combatMillis = (long) (definitions.snapshot().config("config.yml").getDouble("combat-duration-seconds", 10) * 1000);
        CombatStateService combat = new CombatStateService(this, combatMillis);
        players = new PlayerDataService(this, storage, definitions);
        parties = new PartyService(storage, players);
        try { parties.load().get(15, TimeUnit.SECONDS); }
        catch (Exception ex) { getLogger().log(Level.SEVERE, "Could not load party data", ex); Bukkit.getPluginManager().disablePlugin(this); return; }

        ItemService items = new ItemService(this, definitions);
        StatService stats = new StatService(this, definitions, combat, items);
        LevelService levels = new LevelService(definitions, stats);
        RegionService regions = new RegionService(this, definitions);
        MobService mobs = new MobService(this, definitions);
        ElementService elements = new ElementService(this, definitions);
        DamageDisplayService displays = new DamageDisplayService(this, definitions);
        DamageService damage = new DamageService(this, definitions, players, stats, combat, elements, mobs, parties, displays, regions, levels);
        MobAbilityService mobAbilities = new MobAbilityService(this, definitions, mobs, damage, items);
        HealService healing = new HealService(players, stats, displays, levels);
        BuffService buffs = new BuffService(this, definitions, players, stats, damage, healing);
        EquipmentService equipment = new EquipmentService(this, definitions, players, stats, combat, items);
        SkillService skills = new SkillService(this, definitions, players, stats, combat, damage, items);
        SkillTreeService skillTrees = new SkillTreeService(definitions, players, stats);
        EnhancementService enhancement = new EnhancementService(definitions, players, stats, items);
        EncyclopediaService encyclopedia = new EncyclopediaService(players, mobs, definitions);
        SpawnService spawns = new SpawnService(this, definitions, mobs, combat);
        DebugService debug = new DebugService(this);
        HudService hud = new HudService(this, definitions, players, stats, levels, combat, elements, skills);
        GuiService gui = new GuiService(this, definitions, players, stats, levels, combat, equipment, skillTrees, parties, enhancement, items);
        BackupService backups = new BackupService(this, definitions, storage, players, parties, stats, levels, elements);

        registerListeners(List.of(regions, mobs, stats, damage, equipment, skills, encyclopedia, gui, mobAbilities,
                new PlayerLifecycleListener(this, players, levels, combat, elements, buffs, equipment, hud, mobs)));

        CcsCommand general = new CcsCommand(players, combat, gui, parties, debug);
        CcsAdminCommand admin = new CcsAdminCommand(this, definitions, storage, players, levels, stats, buffs, elements,
                items, mobs, regions, backups, encyclopedia, parties, combat);
        configureCommand("ccs", general, general);
        configureCommand("ccsadmin", admin, admin);

        api = new CombatCoreApiImpl(players, stats, combat, damage, parties, healing, elements, buffs, equipment, skills,
                levels, mobs, encyclopedia);
        Bukkit.getServicesManager().register(CombatCoreApi.class, api, this, ServicePriority.Normal);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) new CcsPlaceholderExpansion(this, definitions, players, stats, levels, combat, elements, skills, parties, items).register();
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null) getLogger().info("Floodgate detected; Bedrock players use the common CCS controls and GUI flow.");

        combat.start(); mobs.start(); elements.start(); displays.start(); buffs.start(); spawns.start(); mobAbilities.start(); debug.start(); hud.start(); players.schedule(); backups.schedule();
        Bukkit.getOnlinePlayers().forEach(player -> players.load(player, data -> { elements.resumePlayer(data); equipment.syncArmor(player); levels.restore(player, data); }));
        getLogger().info("CombatCoreSystems v" + getPluginMeta().getVersion() + " enabled with " + storage.backend() + " storage.");
    }

    @Override public void onDisable() {
        if (storage == null) return;
        try {
            var playerSave = players == null ? java.util.concurrent.CompletableFuture.completedFuture(null) : players.saveAll();
            var partySave = parties == null ? java.util.concurrent.CompletableFuture.completedFuture(null) : parties.save();
            java.util.concurrent.CompletableFuture.allOf(playerSave, partySave).get(20, TimeUnit.SECONDS);
        } catch (Exception ex) { getLogger().log(Level.SEVERE, "Final CCS save did not complete cleanly", ex); }
        storage.close();
    }

    public CombatCoreApi getApi() { return api; }

    private void registerListeners(List<Listener> listeners) { listeners.forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this)); }
    private void configureCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
        PluginCommand command = getCommand(name); if (command == null) throw new IllegalStateException(name + " is missing from plugin.yml");
        command.setExecutor(executor); command.setTabCompleter(completer);
    }
}
