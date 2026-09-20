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
    private LevelService levels;
    private final Map<UUID, Double> synchronizedMaximum = new HashMap<>();
    private final Set<UUID> pendingAudits = new HashSet<>();
    public void bindLevels(LevelService levels) { this.levels = levels; }
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void onDrop(PlayerDropItemEvent event) { auditLater(event.getPlayer()); }
    @EventHandler(ignoreCancelled = true) public void onPickup(EntityPickupItemEvent event) { if (event.getEntity() instanceof Player player) auditLater(player); }
    @EventHandler(ignoreCancelled = true) public void onUse(PlayerInteractEvent event) {
        if (event.getItem() != null) markUsed(event.getPlayer(), event.getItem());
        // A click/mining swing does not change inventory. Held/click/drop/pickup events audit actual changes.
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
                    else data.getEquipment().remove(entry.getKey());
                } else data.getEquipment().remove(entry.getKey());
            }
            for (EquipmentSlot accessory : List.of(EquipmentSlot.RESONANCE, EquipmentSlot.DIVINE_HEART)) {
                ItemInstance registered = data.getEquipment().get(accessory);
                if (registered == null) continue;
                if (accessory == EquipmentSlot.RESONANCE && !data.getResonanceItem().isBlank()) continue;
                ItemInstance actual = Arrays.stream(player.getInventory().getContents()).map(items::instance).flatMap(Optional::stream)
                        .filter(value -> value.getInstanceId().equals(registered.getInstanceId())).findFirst().orElse(null);
                if (actual == null) data.getEquipment().remove(accessory);
                else {
                    data.getEquipment().put(accessory, actual);
                    if (accessory == EquipmentSlot.RESONANCE) {
                        // One-time migration from inventory registration to a dedicated persisted slot.
                        for (int i = 0; i < player.getInventory().getSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (items.instance(stack).map(value -> value.getInstanceId().equals(actual.getInstanceId())).orElse(false)) {
                                data.setResonanceItem(Base64.getEncoder().encodeToString(stack.serializeAsBytes()));
                                player.getInventory().setItem(i, null); break;
                            }
                        }
                    }
                }
            }
            Map<String, Integer> setCounts = new HashMap<>();
            for (EquipmentSlot armor : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.RESONANCE)) {
                ItemInstance equipped = data.getEquipment().get(armor);
                EquipmentDefinition definition = equipped == null ? null : definitions.snapshot().equipment().get(equipped.getDefinitionId());
                if (definition != null && !definition.setId().isBlank()) setCounts.merge(definition.setId(), 1, Integer::sum);
            }
            for (ItemStack stack : player.getInventory().getContents()) items.instance(stack).ifPresent(instance -> {
                EquipmentDefinition definition = definitions.snapshot().equipment().get(instance.getDefinitionId());
                items.writeInstance(stack, instance, definition == null ? 0 : setCounts.getOrDefault(definition.setId(), 0));
            });
            stats.invalidate(player.getUniqueId());
        });
        auditWeapons(player);
    }

    public boolean equip(Player player, int inventorySlot) {
        if (combat.inCombat(player.getUniqueId()) || inventorySlot < 0 || inventorySlot >= 36) return false;
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        ItemInstance instance = items.instance(stack).orElse(null);
        if (instance == null) return false;
        EquipmentSlot slot = slotOf(instance.getDefinitionId());
        if (slot == null) return false;
        int physical = physicalSlot(slot);
        if (physical >= 0) {
            ItemStack previous = player.getInventory().getItem(physical);
            player.getInventory().setItem(physical, stack);
            player.getInventory().setItem(inventorySlot, previous);
        } else if (slot == EquipmentSlot.RESONANCE) {
            var data = players.require(player);
            ItemStack previous = storedResonance(data);
            data.setResonanceItem(Base64.getEncoder().encodeToString(stack.serializeAsBytes()));
            data.getEquipment().put(slot, instance);
            player.getInventory().setItem(inventorySlot, previous);
        } else {
            // Accessory registration refers to the real inventory instance; no duplicate item is created.
            players.require(player).getEquipment().put(slot, instance);
        }
        syncArmor(player);
        return true;
    }

    public EquipmentSlot slotOf(String id) {
        if (id == null || id.isBlank()) return null;
        EquipmentDefinition definition = definitions.snapshot().equipment().get(id);
        if (definition != null) return definition.slot();
        return definitions.snapshot().config("divine_hearts.yml").isConfigurationSection("divine-hearts." + id) ? EquipmentSlot.DIVINE_HEART : null;
    }

    public boolean unequip(Player player, EquipmentSlot slot) {
        if (combat.inCombat(player.getUniqueId())) return false;
        if (slot == EquipmentSlot.RESONANCE) {
            var data = players.require(player);
            ItemStack stored = storedResonance(data);
            if (stored != null) {
                int empty = player.getInventory().firstEmpty();
                if (empty < 0 || empty >= 36) return false;
                player.getInventory().setItem(empty, stored);
                data.setResonanceItem("");
            }
        }
        int physical = physicalSlot(slot);
        if (physical >= 0) {
            int empty = player.getInventory().firstEmpty();
            if (empty < 0 || empty >= 36) return false;
            player.getInventory().setItem(empty, player.getInventory().getItem(physical));
            player.getInventory().setItem(physical, null);
        }
        players.require(player).getEquipment().remove(slot);
        syncArmor(player);
        return true;
    }

    private int physicalSlot(EquipmentSlot slot) {
        return switch (slot) { case HEAD -> 39; case CHEST -> 38; case LEGS -> 37; case FEET -> 36; default -> -1; };
    }

    public ItemStack storedResonance(com.github.saku0817.combatcoresystems.model.PlayerData data) {
        if (data.getResonanceItem().isBlank()) return null;
        ItemStack stack = ItemStack.deserializeBytes(Base64.getDecoder().decode(data.getResonanceItem()));
        ItemInstance instance = data.getEquipment().get(EquipmentSlot.RESONANCE);
        if (instance != null) {
            EquipmentDefinition definition = definitions.snapshot().equipment().get(instance.getDefinitionId());
            int count = definition == null ? 0 : SetEffectService.counts(definitions.snapshot(), data).getOrDefault(definition.setId(), 0);
            items.writeInstance(stack, instance, count);
        }
        return stack;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (event.getKeepInventory()) return;
        players.find(event.getPlayer().getUniqueId()).ifPresent(data -> {
            ItemStack stored = storedResonance(data);
            if (stored == null) return;
            event.getDrops().add(stored); data.setResonanceItem(""); data.getEquipment().remove(EquipmentSlot.RESONANCE);
            stats.invalidate(event.getPlayer().getUniqueId());
        });
    }

    public void markUsed(Player player, ItemStack item) {
        ItemInstance instance = items.instance(item).orElse(null);
        if (instance == null) return;
        var definition = definitions.snapshot().weapon(instance);
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

    private void auditLater(Player player) {
        if (!pendingAudits.add(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
        pendingAudits.remove(player.getUniqueId());
        if (!player.isOnline() || player.isDead()) return;
        syncArmor(player);
        if (levels != null) players.find(player.getUniqueId()).ifPresent(data -> {
            double maximum = stats.get(player, data).maxHp();
            Double previous = synchronizedMaximum.put(player.getUniqueId(), maximum);
            if (previous == null || Double.compare(previous, maximum) != 0) levels.apply(player, data, false);
        });
    }); }
    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) { synchronizedMaximum.remove(event.getPlayer().getUniqueId()); }
    private void collect(Map<com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category, List<WeaponSlot>> grouped, WeaponSlot slot) {
        ItemInstance instance = items.instance(slot.item).orElse(null); if (instance == null) return;
        var definition = definitions.snapshot().weapon(instance); if (definition == null) return;
        if (definition.category() == com.github.saku0817.combatcoresystems.model.WeaponDefinition.Category.UNCATEGORIZED) return;
        grouped.computeIfAbsent(definition.category(), ignored -> new ArrayList<>()).add(new WeaponSlot(slot.slot, slot.item, instance.getInstanceId()));
    }
    private int emptyStorageSlot(Player player) { for (int slot = 9; slot <= 35; slot++) { ItemStack item = player.getInventory().getItem(slot); if (item == null || item.getType().isAir()) return slot; } return -1; }
    private void remove(Player player, int slot) { if (slot == 40) player.getInventory().setItemInOffHand(null); else player.getInventory().setItem(slot, null); }
    private record WeaponSlot(int slot, ItemStack item, String instanceId) { WeaponSlot(int slot, ItemStack item) { this(slot, item, ""); } }
}
