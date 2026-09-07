package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.google.gson.Gson;
import com.google.common.collect.ImmutableMultimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class ItemService {
    private final DefinitionRegistry definitions;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Gson gson = new Gson();
    private final NamespacedKey idKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey instanceKey;

    public ItemService(JavaPlugin plugin, DefinitionRegistry definitions) {
        this.definitions = definitions;
        idKey = new NamespacedKey(plugin, "item_id");
        typeKey = new NamespacedKey(plugin, "item_type");
        instanceKey = new NamespacedKey(plugin, "item_instance");
    }

    public Optional<ItemStack> create(String id, int amount) {
        WeaponDefinition weapon = definitions.snapshot().weapons().get(id);
        if (weapon != null) return Optional.of(build(id, "WEAPON", weapon.name(), weapon.material(), weapon.customModelData(), weapon.lore(), amount, true));
        EquipmentDefinition equipment = definitions.snapshot().equipment().get(id);
        if (equipment != null) return Optional.of(build(id, "EQUIPMENT", equipment.name(), equipment.material(), equipment.customModelData(), equipment.lore(), amount, true));
        ConfigurationSection heart = definitions.snapshot().config("divine_hearts.yml").getConfigurationSection("divine-hearts." + id);
        if (heart != null) return Optional.of(build(id, "DIVINE_HEART", heart.getString("name", id), heart.getString("material", "NETHER_STAR"),
                heart.contains("custom-model-data") ? heart.getInt("custom-model-data") : null, heart.getStringList("lore"), amount, true));
        ConfigurationSection material = definitions.snapshot().config("levels.yml").getConfigurationSection("materials." + id);
        if (material != null) return Optional.of(build(id, "MATERIAL", material.getString("name", id), material.getString("material", "PAPER"),
                material.contains("custom-model-data") ? material.getInt("custom-model-data") : null, material.getStringList("lore"), amount, false));
        return Optional.empty();
    }

    private ItemStack build(String id, String type, String name, String materialName, Integer model, List<String> lore, int amount, boolean instance) {
        Material material = Material.matchMaterial(materialName);
        ItemStack stack = new ItemStack(material == null ? Material.BARRIER : material, instance ? 1 : Math.max(1, Math.min(99, amount)));
        ItemMeta meta = stack.getItemMeta();
        meta.itemName(mini.deserialize(name));
        List<Component> lines = new ArrayList<>();
        lore.forEach(line -> lines.add(mini.deserialize(line)));
        meta.lore(lines);
        meta.setUnbreakable(true);
        if (instance) meta.setAttributeModifiers(ImmutableMultimap.of());
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        if (model != null) meta.setCustomModelData(model);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(idKey, PersistentDataType.STRING, id);
        pdc.set(typeKey, PersistentDataType.STRING, type);
        ItemInstance value = null;
        if (instance) {
            value = new ItemInstance();
            value.setDefinitionId(id);
            EquipmentDefinition equipment = definitions.snapshot().equipment().get(id);
            if (equipment != null) generateSubstats(value, equipment);
            pdc.set(instanceKey, PersistentDataType.STRING, gson.toJson(value));
        }
        stack.setItemMeta(meta);
        if (value != null) writeInstance(stack, value);
        return stack;
    }

    public Optional<String> id(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING));
    }

    public Optional<ItemInstance> instance(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        String raw = item.getItemMeta().getPersistentDataContainer().get(instanceKey, PersistentDataType.STRING);
        return raw == null ? Optional.empty() : Optional.of(gson.fromJson(raw, ItemInstance.class));
    }

    public NamespacedKey idKey() { return idKey; }

    public void writeInstance(ItemStack item, ItemInstance instance) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, gson.toJson(instance));
        List<Component> lore = new ArrayList<>();
        WeaponDefinition weapon = definitions.snapshot().weapons().get(instance.getDefinitionId());
        EquipmentDefinition equipment = definitions.snapshot().equipment().get(instance.getDefinitionId());
        if (weapon != null) weapon.lore().forEach(line -> lore.add(mini.deserialize(line)));
        if (equipment != null) equipment.lore().forEach(line -> lore.add(mini.deserialize(line)));
        lore.add(mini.deserialize("<gray>Lv." + instance.getLevel() + "</gray>"));
        if (weapon != null) lore.add(mini.deserialize("<gray>限界突破 " + instance.getLimitBreak() + "/5</gray>"));
        if (equipment != null) {
            int index = 0;
            for (var entry : instance.getSubstats().entrySet()) {
                boolean open = index++ < instance.getUnlockedSubstats();
                lore.add(mini.deserialize((open ? "<white>" : "<dark_gray>[未開放] ") + entry.getKey() + " +" + entry.getValue() + (open ? "</white>" : "</dark_gray>")));
            }
        }
        meta.lore(lore); item.setItemMeta(meta);
    }

    private void generateSubstats(ItemInstance instance, EquipmentDefinition definition) {
        List<StatKey> candidates = new ArrayList<>(definition.substatCandidates());
        if (candidates.isEmpty()) candidates.addAll(List.of(StatKey.ATK_FLAT, StatKey.ATK_PERCENT, StatKey.HP_FLAT,
                StatKey.HP_PERCENT, StatKey.DEF_FLAT, StatKey.DEF_PERCENT, StatKey.CRIT_RATE, StatKey.CRIT_DAMAGE));
        java.util.Collections.shuffle(candidates, ThreadLocalRandom.current());
        int count = Math.min(definition.rarity() - 1, candidates.size());
        for (int i = 0; i < count; i++) {
            StatKey key = candidates.get(i);
            instance.getSubstats().put(key.name(), key.name().endsWith("_FLAT") ? 25.0 : 0.05);
            instance.getSubstatUpgrades().put(key.name(), 0);
        }
        writeInitialNoop(instance);
    }

    private void writeInitialNoop(ItemInstance instance) {
        // Marker method keeps substat generation isolated from item rendering.
    }
}
