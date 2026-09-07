package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.EquipmentDefinition;
import com.github.saku0817.combatcoresystems.model.ItemInstance;
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

        long gained = 0;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            String id = items.id(stack).orElse("");
            ConfigurationSection material = definitions.snapshot().config("levels.yml").getConfigurationSection("materials." + id);
            if (material == null || !type.equalsIgnoreCase(material.getString("type"))) continue;
            gained += material.getLong("exp") * stack.getAmount();
            player.getInventory().setItem(slot, null);
        }
        if (gained <= 0) return new Result(false, 0, instance.getLevel(), "no_materials");
        instance.setExp(instance.getExp() + gained);
        while (instance.getLevel() < maximum) {
            long required = required(instance.getLevel(), weapon);
            if (instance.getExp() < required) break;
            instance.setExp(instance.getExp() - required);
            instance.setLevel(instance.getLevel() + 1);
            if (!weapon && instance.getLevel() % 3 == 0) progressSubstats(instance, equipment);
        }
        if (instance.getLevel() >= maximum) instance.setExp(0);
        items.writeInstance(target, instance);
        player.getInventory().setItemInMainHand(target);
        var data = players.require(player);
        data.getEquipment().replaceAll((slot, old) -> old.getInstanceId().equals(instance.getInstanceId()) ? instance : old);
        stats.invalidate(player.getUniqueId());
        return new Result(true, gained, instance.getLevel(), "");
    }

    public Result enhancePlayer(Player player, LevelService levels) {
        long gained = 0;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot); String id = items.id(stack).orElse("");
            ConfigurationSection material = definitions.snapshot().config("levels.yml").getConfigurationSection("materials." + id);
            if (material == null || !"PLAYER".equalsIgnoreCase(material.getString("type"))) continue;
            gained += material.getLong("exp") * stack.getAmount(); player.getInventory().setItem(slot, null);
        }
        if (gained <= 0) return new Result(false, 0, players.require(player).getLevel(), "no_player_materials");
        levels.addExp(player, players.require(player), gained); return new Result(true, gained, players.require(player).getLevel(), "");
    }

    public Result limitBreakHeld(Player player) {
        ItemStack target = player.getInventory().getItemInMainHand(); ItemInstance instance = items.instance(target).orElse(null);
        if (instance == null || !definitions.snapshot().weapons().containsKey(instance.getDefinitionId())) return new Result(false, 0, 0, "main_hand_not_weapon");
        if (instance.getLimitBreak() >= 5) return new Result(false, 0, instance.getLevel(), "limit_break_maximum");
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot); if (candidate == null || candidate == target) continue;
            ItemInstance copy = items.instance(candidate).orElse(null); if (copy == null || !copy.getDefinitionId().equals(instance.getDefinitionId())) continue;
            candidate.setAmount(candidate.getAmount() - 1); instance.setLimitBreak(instance.getLimitBreak() + 1); items.writeInstance(target, instance);
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
}
