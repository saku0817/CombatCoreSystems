package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.EquipmentDefinition;
import com.github.saku0817.combatcoresystems.model.EquipmentSlot;
import com.github.saku0817.combatcoresystems.model.ItemInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class EquipmentService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final CombatStateService combat;
    private final ItemService items;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Map<UUID, EnumMap<com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category, String>> lastUsed = new HashMap<>();

    public EquipmentService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players,
                            StatService stats, CombatStateService combat, ItemService items) {
        this.plugin = plugin; this.definitions = definitions; this.players = players; this.stats = stats; this.combat = combat; this.items = items;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (combat.inCombat(player.getUniqueId()) && (event.isShiftClick() || event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR)) {
            event.setCancelled(true); return;
        }
        auditLater(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (combat.inCombat(player.getUniqueId()) && event.getRawSlots().stream().anyMatch(slot -> slot >= 5 && slot <= 8)) event.setCancelled(true);
        else auditLater(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        auditLater(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true) public void onSwap(PlayerSwapHandItemsEvent event) { auditLater(event.getPlayer()); }
    @EventHandler(ignoreCancelled = true) public void onDrop(PlayerDropItemEvent event) { auditLater(event.getPlayer()); }
    @EventHandler(ignoreCancelled = true) public void onPickup(EntityPickupItemEvent event) { if (event.getEntity() instanceof Player player) auditLater(player); }
    @EventHandler(ignoreCancelled = true) public void onUse(PlayerInteractEvent event) {
        if (event.getItem() != null) markUsed(event.getPlayer(), event.getItem());
        auditLater(event.getPlayer());
    }
    @EventHandler(ignoreCancelled = true) public void onAttack(EntityDamageByEntityEvent event) {
        Player player = event.getDamager() instanceof Player direct ? direct
                : event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter ? shooter : null;
        if (player != null) markUsed(player, player.getInventory().getItemInMainHand());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (items.id(event.getItem()).isPresent()) event.setCancelled(true);
    }

    public void syncArmor(Player player) {
        if (combat.inCombat(player.getUniqueId())) return;
        players.find(player.getUniqueId()).ifPresent(data -> {
            data.getEquipment().remove(EquipmentSlot.MELEE_WEAPON);
            data.getEquipment().remove(EquipmentSlot.RANGED_WEAPON);
            EnumMap<EquipmentSlot, ItemStack> slots = new EnumMap<>(EquipmentSlot.class);
            slots.put(EquipmentSlot.HEAD, player.getInventory().getHelmet());
            slots.put(EquipmentSlot.CHEST, player.getInventory().getChestplate());
            slots.put(EquipmentSlot.LEGS, player.getInventory().getLeggings());
            slots.put(EquipmentSlot.FEET, player.getInventory().getBoots());
            for (var entry : slots.entrySet()) {
                ItemInstance instance = items.instance(entry.getValue()).orElse(null);
                if (instance != null) {
                    EquipmentDefinition definition = definitions.snapshot().equipment().get(instance.getDefinitionId());
                    if (definition != null && definition.slot() == entry.getKey()) data.getEquipment().put(entry.getKey(), instance);
                } else data.getEquipment().remove(entry.getKey());
            }
            stats.invalidate(player.getUniqueId());
        });
        auditWeapons(player);
    }

    public void markUsed(Player player, ItemStack item) {
        ItemInstance instance = items.instance(item).orElse(null);
        if (instance == null) return;
        var definition = definitions.snapshot().weapons().get(instance.getDefinitionId());
        if (definition == null) return;
        lastUsed.computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category.class))
                .put(definition.category(), instance.getInstanceId());
    }

    public void auditWeapons(Player player) {
        if (!player.isOnline()) return;
        EnumMap<com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category, List<WeaponSlot>> grouped =
                new EnumMap<>(com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category.class);
        for (int slot = 0; slot <= 8; slot++) collect(grouped, new WeaponSlot(slot, player.getInventory().getItem(slot)));
        collect(grouped, new WeaponSlot(40, player.getInventory().getItemInOffHand()));
        EnumMap<com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category, String> used = lastUsed.get(player.getUniqueId());
        boolean moved = false;
        for (var entry : grouped.entrySet()) {
            List<WeaponSlot> candidates = entry.getValue();
            if (candidates.size() <= 1) continue;
            String preferred = used == null ? null : used.get(entry.getKey());
            WeaponSlot keep = preferred == null ? null : candidates.stream().filter(value -> value.instanceId.equals(preferred)).findFirst().orElse(null);
            if (keep == null) keep = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            for (WeaponSlot candidate : candidates) if (candidate != keep) {
                remove(player, candidate.slot);
                int destination = emptyStorageSlot(player);
                if (destination >= 0) player.getInventory().setItem(destination, candidate.item);
                else player.getWorld().dropItemNaturally(player.getLocation(), candidate.item);
                moved = true;
            }
        }
        if (moved) player.sendMessage(mini.deserialize(definitions.snapshot().config("messages.yml").getString("weapon-audit-moved",
                "<yellow>同カテゴリの武器はホットバー/オフハンドに1本だけ置けます。余分な武器を移動しました。</yellow>")));
        stats.invalidate(player.getUniqueId());
    }

    private void auditLater(Player player) { Bukkit.getScheduler().runTask(plugin, () -> { if (combat.inCombat(player.getUniqueId())) auditWeapons(player); else syncArmor(player); }); }
    private void collect(Map<com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category, List<WeaponSlot>> grouped, WeaponSlot slot) {
        ItemInstance instance = items.instance(slot.item).orElse(null); if (instance == null) return;
        var definition = definitions.snapshot().weapons().get(instance.getDefinitionId()); if (definition == null) return;
        grouped.computeIfAbsent(definition.category(), ignored -> new ArrayList<>()).add(new WeaponSlot(slot.slot, slot.item, instance.getInstanceId()));
    }
    private int emptyStorageSlot(Player player) { for (int slot = 9; slot <= 35; slot++) { ItemStack item = player.getInventory().getItem(slot); if (item == null || item.getType().isAir()) return slot; } return -1; }
    private void remove(Player player, int slot) { if (slot == 40) player.getInventory().setItemInOffHand(null); else player.getInventory().setItem(slot, null); }
    private record WeaponSlot(int slot, ItemStack item, String instanceId) { WeaponSlot(int slot, ItemStack item) { this(slot, item, ""); } }
}
