package com.github.saku0817.combatcoresystems.listener;

import com.github.saku0817.combatcoresystems.model.MobDefinition;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.service.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerLifecycleListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerDataService players;
    private final LevelService levels;
    private final CombatStateService combat;
    private final ElementService elements;
    private final BuffService buffs;
    private final EquipmentService equipment;
    private final HudService hud;
    private final MobService mobs;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public PlayerLifecycleListener(JavaPlugin plugin, PlayerDataService players, LevelService levels,
                                   CombatStateService combat, ElementService elements, BuffService buffs,
                                   EquipmentService equipment, HudService hud, MobService mobs) {
        this.plugin = plugin; this.players = players; this.levels = levels; this.combat = combat; this.elements = elements;
        this.buffs = buffs; this.equipment = equipment; this.hud = hud; this.mobs = mobs;
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        players.load(player, data -> {
            elements.resumePlayer(data); equipment.syncArmor(player); registerWeapons(player, data); levels.apply(player, data, false);
            if (data.getCombatLogoutCount() > 0) player.sendMessage(mini.deserialize("<yellow>Combat Logout警告: " + data.getCombatLogoutCount() + "/4（4回目でPlayer Lv -1）</yellow>"));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer(); PlayerData data = players.find(player.getUniqueId()).orElse(null); if (data == null) return;
        if (combat.inCombat(player.getUniqueId())) {
            int violations = data.getCombatLogoutCount() + 1;
            if (violations >= 4) { data.setLevel(Math.max(1, data.getLevel() - 1)); data.setCombatLogoutCount(0); }
            else data.setCombatLogoutCount(violations);
        }
        data.setHealth(player.getHealth()); elements.pausePlayer(data); hud.remove(player); combat.forget(player.getUniqueId()); players.saveAndUnload(player);
    }

    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> players.find(event.getPlayer().getUniqueId()).ifPresent(data -> levels.apply(event.getPlayer(), data, true)));
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        event.setKeepLevel(true); event.setDroppedExp(0); combat.clear(event.getPlayer().getUniqueId()); elements.clear(event.getPlayer().getUniqueId());
        players.find(event.getPlayer().getUniqueId()).ifPresent(data -> { data.setHealth(0); buffs.onDeath(event.getPlayer()); });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExperience(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) return; int amount = event.getAmount(); event.setAmount(0);
        players.find(event.getPlayer().getUniqueId()).ifPresent(data -> levels.addExp(event.getPlayer(), data, amount));
    }

    @EventHandler public void onMobDeath(EntityDeathEvent event) {
        if (event instanceof PlayerDeathEvent) return;
        MobDefinition definition = mobs.definition(event.getEntity()).orElse(null); if (definition == null || event.getEntity().getKiller() == null) return;
        Player killer = event.getEntity().getKiller(); players.find(killer.getUniqueId()).ifPresent(data -> levels.addExp(killer, data, definition.exp()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNaturalRegeneration(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (event.getItem().hasItemMeta() && event.getItem().getItemMeta().isUnbreakable()) event.setCancelled(true);
    }

    private void registerWeapons(Player player, PlayerData data) {
        var melee = data.getEquipment().get(com.github.saku0817.combatcoresystems.model.EquipmentSlot.MELEE_WEAPON);
        var ranged = data.getEquipment().get(com.github.saku0817.combatcoresystems.model.EquipmentSlot.RANGED_WEAPON);
        combat.registerWeapons(player.getUniqueId(), melee == null ? "" : melee.getDefinitionId(), ranged == null ? "" : ranged.getDefinitionId());
    }
}
