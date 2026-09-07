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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;

public final class EquipmentService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final CombatStateService combat;
    private final ItemService items;

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
        Bukkit.getScheduler().runTask(plugin, () -> syncArmor(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (combat.inCombat(player.getUniqueId()) && event.getRawSlots().stream().anyMatch(slot -> slot >= 5 && slot <= 8)) event.setCancelled(true);
        else Bukkit.getScheduler().runTask(plugin, () -> syncArmor(player));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        if (!combat.inCombat(event.getPlayer().getUniqueId())) return;
        String next = items.id(event.getPlayer().getInventory().getItem(event.getNewSlot())).orElse("");
        CombatStateService.State state = combat.state(event.getPlayer().getUniqueId());
        if (!next.isBlank() && !next.equals(state.meleeWeapon()) && !next.equals(state.rangedWeapon())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (items.id(event.getItem()).isPresent()) event.setCancelled(true);
    }

    public void syncArmor(Player player) {
        if (combat.inCombat(player.getUniqueId())) return;
        players.find(player.getUniqueId()).ifPresent(data -> {
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
    }

    public boolean registerWeapon(Player player, EquipmentSlot slot, ItemStack item) {
        if (slot != EquipmentSlot.MELEE_WEAPON && slot != EquipmentSlot.RANGED_WEAPON || combat.inCombat(player.getUniqueId())) return false;
        ItemInstance instance = items.instance(item).orElse(null);
        if (instance == null) return false;
        var definition = definitions.snapshot().weapons().get(instance.getDefinitionId());
        if (definition == null || (slot == EquipmentSlot.MELEE_WEAPON) != (definition.category() == com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category.MELEE)) return false;
        var data = players.require(player);
        if (player.getLevel() < definition.minimumEquipLevel() || player.getLevel() > definition.maximumEquipLevel()) return false;
        data.getEquipment().put(slot, instance);
        combat.registerWeapons(player.getUniqueId(),
                data.getEquipment().get(EquipmentSlot.MELEE_WEAPON) == null ? "" : data.getEquipment().get(EquipmentSlot.MELEE_WEAPON).getDefinitionId(),
                data.getEquipment().get(EquipmentSlot.RANGED_WEAPON) == null ? "" : data.getEquipment().get(EquipmentSlot.RANGED_WEAPON).getDefinitionId());
        stats.invalidate(player.getUniqueId());
        return true;
    }
}
