package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.EquipmentDefinition;
import com.github.saku0817.combatcoresystems.model.ItemInstance;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.StatKey;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class EnhancementService {
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final ItemService items;

    public EnhancementService(DefinitionRegistry definitions, PlayerDataService players, StatService stats, ItemService items) {
        this.definitions = definitions; this.players = players; this.stats = stats; this.items = items;
    }

    public Result enhanceHeld(Player player) {
        ItemStack target = player.getInventory().getItemInMainHand();
        ItemInstance instance = items.instance(target).orElse(null);
        if (instance == null) return new Result(false, 0, 0, "main_hand_not_enhanceable");
        boolean weapon = definitions.snapshot().weapons().containsKey(instance.getDefinitionId());
        EquipmentDefinition equipment = definitions.snapshot().equipment().get(instance.getDefinitionId());
        if (!weapon && equipment == null) return new Result(false, 0, 0, "unsupported_item");
        String type = weapon ? "WEAPON" : "EQUIPMENT";
        int maximum = weapon ? 100 : equipment.maxLevel();
        if (instance.getLevel() >= maximum) return new Result(false, 0, instance.getLevel(), "already_maximum");

        Map<String, Long> selection = new LinkedHashMap<>();
        for (MaterialInfo material : materials(type)) selection.put(material.id(), available(player, material.id()));
        long gained = selectedExp(selection);
        if (gained <= 0) return new Result(false, 0, instance.getLevel(), "no_materials");
        return enhanceItem(player, instance.getInstanceId(), selection);
    }

    public Result enhancePlayer(Player player, LevelService levels) {
        Map<String, Long> selection = new LinkedHashMap<>();
        for (MaterialInfo material : materials("PLAYER")) selection.put(material.id(), available(player, material.id()));
        return enhancePlayer(player, levels, selection);
    }

    public Result enhancePlayer(Player player, LevelService levels, Map<String, Long> selection) {
        long gained = selectedExp(selection);
        if (gained <= 0) return new Result(false, 0, players.require(player).getLevel(), "no_player_materials");
        if (!consume(player, selection, "PLAYER")) return new Result(false, 0, players.require(player).getLevel(), "materials_changed");
        levels.addExp(player, players.require(player), gained);
        return new Result(true, gained, players.require(player).getLevel(), "");
    }

    public Result enhanceItem(Player player, String instanceId, Map<String, Long> selection) {
        LocatedItem located = find(player, instanceId);
        if (located == null) return new Result(false, 0, 0, "target_not_found");
        ItemInstance instance = located.instance;
        boolean weapon = definitions.snapshot().weapons().containsKey(instance.getDefinitionId());
        EquipmentDefinition equipment = definitions.snapshot().equipment().get(instance.getDefinitionId());
        if (!weapon && equipment == null) return new Result(false, 0, instance.getLevel(), "unsupported_item");
        int maximum = weapon ? 100 : equipment.maxLevel();
        if (instance.getLevel() >= maximum) return new Result(false, 0, instance.getLevel(), "already_maximum");
        String type = weapon ? "WEAPON" : "EQUIPMENT";
        long gained = selectedExp(selection);
        if (gained <= 0) return new Result(false, 0, instance.getLevel(), "no_materials");
        if (!consume(player, selection, type)) return new Result(false, 0, instance.getLevel(), "materials_changed");
        instance.setExp(instance.getExp() + gained);
        while (instance.getLevel() < maximum) {
            long needed = required(instance.getLevel(), weapon);
            if (instance.getExp() < needed) break;
            instance.setExp(instance.getExp() - needed);
            instance.setLevel(instance.getLevel() + 1);
            if (!weapon && instance.getLevel() % 3 == 0) progressSubstats(instance, equipment);
        }
        if (instance.getLevel() >= maximum) instance.setExp(0);
        items.writeInstance(located.stack, instance);
        player.getInventory().setItem(located.slot, located.stack);
        players.require(player).getEquipment().replaceAll((slot, old) -> old.getInstanceId().equals(instance.getInstanceId()) ? instance : old);
        stats.invalidate(player.getUniqueId());
        return new Result(true, gained, instance.getLevel(), "");
    }

    public Preview previewItem(ItemInstance original, Map<String, Long> selection) {
        boolean weapon = definitions.snapshot().weapons().containsKey(original.getDefinitionId());
        EquipmentDefinition equipment = definitions.snapshot().equipment().get(original.getDefinitionId());
        int maximum = weapon ? 100 : equipment == null ? original.getLevel() : equipment.maxLevel();
        int level = original.getLevel(); long exp = original.getExp() + selectedExp(selection);
        while (level < maximum && exp >= required(level, weapon)) { exp -= required(level, weapon); level++; }
        if (level >= maximum) exp = 0;
        return new Preview(original.getLevel(), original.getExp(), level, exp, selectedExp(selection));
    }

    public Preview previewPlayer(Player player, LevelService levels, Map<String, Long> selection) {
        PlayerData data = players.require(player); int level = data.getLevel(); long exp = data.getExp() + selectedExp(selection);
        int maximum = definitions.snapshot().config("levels.yml").getInt("player.max-level", 100);
        while (level < maximum && exp >= levels.requiredExp(level)) { exp -= levels.requiredExp(level); level++; }
        if (level >= maximum) exp = 0;
        return new Preview(data.getLevel(), data.getExp(), level, exp, selectedExp(selection));
    }

    public List<MaterialInfo> materials(String type) {
        List<MaterialInfo> result = new ArrayList<>();
        ConfigurationSection root = definitions.snapshot().config("levels.yml").getConfigurationSection("materials");
        if (root == null) return result;
        for (String id : root.getKeys(false)) {
            ConfigurationSection value = root.getConfigurationSection(id);
            if (value != null && type.equalsIgnoreCase(value.getString("type"))) result.add(new MaterialInfo(id, value.getString("name", id), value.getString("material", "PAPER"), Math.max(0, value.getLong("exp"))));
        }
        result.sort(Comparator.comparingLong(MaterialInfo::exp));
        return result;
    }

    public long available(Player player, String id) {
        long count = players.require(player).getEnhancementMaterials().getOrDefault(id, 0L);
        for (ItemStack stack : player.getInventory().getContents()) if (id.equals(items.id(stack).orElse(""))) count += stack.getAmount();
        return count;
    }

    public boolean deposit(Player player, String id, long amount) {
        long remaining = Math.max(1, amount);
        for (int slot = 0; slot < player.getInventory().getSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot); if (!id.equals(items.id(stack).orElse(""))) continue;
            int take = (int) Math.min(remaining, stack.getAmount()); stack.setAmount(stack.getAmount() - take); remaining -= take;
        }
        long moved = Math.max(1, amount) - remaining; if (moved <= 0) return false;
        players.require(player).getEnhancementMaterials().merge(id, moved, Long::sum); return true;
    }

    public boolean withdraw(Player player, String id, long amount) {
        PlayerData data = players.require(player); long held = data.getEnhancementMaterials().getOrDefault(id, 0L); long move = Math.min(held, Math.max(1, amount));
        if (move <= 0) return false;
        long remaining = move;
        while (remaining > 0) {
            int batch = (int) Math.min(64, remaining); ItemStack stack = items.create(id, batch).orElse(null); if (stack == null) return false;
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack); if (!overflow.isEmpty()) { overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item)); }
            remaining -= batch;
        }
        setVirtual(data, id, held - move); return true;
    }

    public boolean exchangeTier(Player player, String id, boolean upward) {
        ConfigurationSection source = definitions.snapshot().config("levels.yml").getConfigurationSection("materials." + id); if (source == null) return false;
        List<MaterialInfo> tiers = materials(source.getString("type", "")); int index = -1;
        for (int i = 0; i < tiers.size(); i++) if (tiers.get(i).id().equals(id)) index = i;
        int otherIndex = upward ? index + 1 : index - 1; if (index < 0 || otherIndex < 0 || otherIndex >= tiers.size()) return false;
        MaterialInfo from = tiers.get(index), to = tiers.get(otherIndex); long ratio = upward ? Math.max(1, to.exp() / Math.max(1, from.exp())) : Math.max(1, from.exp() / Math.max(1, to.exp()));
        PlayerData data = players.require(player); long have = data.getEnhancementMaterials().getOrDefault(id, 0L);
        long consume = upward ? ratio : 1; long produce = upward ? 1 : ratio; if (have < consume) return false;
        setVirtual(data, id, have - consume); data.getEnhancementMaterials().merge(to.id(), produce, Long::sum); return true;
    }

    private long selectedExp(Map<String, Long> selection) {
        long total = 0; ConfigurationSection root = definitions.snapshot().config("levels.yml").getConfigurationSection("materials"); if (root == null) return 0;
        for (var entry : selection.entrySet()) {
            long count = Math.max(0, entry.getValue()), value = Math.max(0, root.getLong(entry.getKey() + ".exp"));
            if (value > 0 && count > Long.MAX_VALUE / value) return Long.MAX_VALUE;
            long contribution = count * value; if (Long.MAX_VALUE - total < contribution) return Long.MAX_VALUE; total += contribution;
        }
        return total;
    }

    private boolean consume(Player player, Map<String, Long> selection, String type) {
        for (var entry : selection.entrySet()) {
            ConfigurationSection material = definitions.snapshot().config("levels.yml").getConfigurationSection("materials." + entry.getKey());
            if (material == null || !type.equalsIgnoreCase(material.getString("type")) || available(player, entry.getKey()) < entry.getValue()) return false;
        }
        PlayerData data = players.require(player);
        for (var entry : selection.entrySet()) {
            long remaining = Math.max(0, entry.getValue()); long virtual = data.getEnhancementMaterials().getOrDefault(entry.getKey(), 0L); long useVirtual = Math.min(virtual, remaining);
            setVirtual(data, entry.getKey(), virtual - useVirtual); remaining -= useVirtual;
            for (int slot = 0; slot < player.getInventory().getSize() && remaining > 0; slot++) {
                ItemStack stack = player.getInventory().getItem(slot); if (!entry.getKey().equals(items.id(stack).orElse(""))) continue;
                int take = (int) Math.min(remaining, stack.getAmount()); stack.setAmount(stack.getAmount() - take); remaining -= take;
            }
        }
        return true;
    }

    private LocatedItem find(Player player, String instanceId) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) { ItemStack stack = player.getInventory().getItem(slot); ItemInstance value = items.instance(stack).orElse(null); if (value != null && value.getInstanceId().equals(instanceId)) return new LocatedItem(slot, stack, value); }
        return null;
    }
    private void setVirtual(PlayerData data, String id, long amount) { if (amount <= 0) data.getEnhancementMaterials().remove(id); else data.getEnhancementMaterials().put(id, amount); }

    public Result limitBreakHeld(Player player) {
        ItemInstance instance = items.instance(player.getInventory().getItemInMainHand()).orElse(null);
        return instance == null ? new Result(false, 0, 0, "main_hand_not_weapon") : limitBreak(player, instance.getInstanceId());
    }

    public Result limitBreak(Player player, String instanceId) {
        LocatedItem target = find(player, instanceId); ItemInstance instance = target == null ? null : target.instance;
        if (instance == null || !definitions.snapshot().weapons().containsKey(instance.getDefinitionId())) return new Result(false, 0, 0, "target_not_weapon");
        if (instance.getLimitBreak() >= 5) return new Result(false, 0, instance.getLevel(), "limit_break_maximum");
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot); if (candidate == null || candidate == target.stack) continue;
            ItemInstance copy = items.instance(candidate).orElse(null); if (copy == null || !copy.getDefinitionId().equals(instance.getDefinitionId())) continue;
            candidate.setAmount(candidate.getAmount() - 1); instance.setLimitBreak(instance.getLimitBreak() + 1); items.writeInstance(target.stack, instance);
            player.getInventory().setItem(target.slot, target.stack);
            players.require(player).getEquipment().replaceAll((key, old) -> old.getInstanceId().equals(instance.getInstanceId()) ? instance : old);
            stats.invalidate(player.getUniqueId()); return new Result(true, 0, instance.getLevel(), "");
        }
        return new Result(false, 0, instance.getLevel(), "duplicate_weapon_required");
    }

    private long required(int level, boolean weapon) {
        String root = weapon ? "weapon-exp" : "equipment-exp";
        var yaml = definitions.snapshot().config("levels.yml");
        if (yaml.getString(root + ".mode", "QUADRATIC").equalsIgnoreCase("TABLE")) return yaml.getLong(root + ".table." + level, Long.MAX_VALUE);
        return CoreMath.quadraticExp(yaml.getLong(root + ".base", 100), yaml.getLong(root + ".growth", 25), level);
    }

    private void progressSubstats(ItemInstance instance, EquipmentDefinition definition) {
        if (instance.getLevel() < definition.maxLevel()) instance.setUnlockedSubstats(Math.min(definition.rarity() - 1, instance.getUnlockedSubstats() + 1));
        if (instance.getSubstats().isEmpty()) return;
        List<String> keys = new ArrayList<>(instance.getSubstats().keySet());
        String chosen = keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        StatKey key = StatKey.valueOf(chosen);
        double increment = chosen.endsWith("_FLAT") ? 25.0 : 0.05;
        instance.getSubstats().merge(chosen, increment, Double::sum);
        instance.getSubstatUpgrades().merge(chosen, 1, Integer::sum);
    }

    public record Result(boolean success, long consumedExp, int resultingLevel, String failure) {}
    public record Preview(int beforeLevel, long beforeExp, int afterLevel, long afterExp, long gainedExp) {}
    public record MaterialInfo(String id, String name, String material, long exp) {}
    private record LocatedItem(int slot, ItemStack stack, ItemInstance instance) {}
}
