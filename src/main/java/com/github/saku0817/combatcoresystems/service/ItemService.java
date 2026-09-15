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
    private DefinitionRegistry.Snapshot renderedSnapshot;
    private record Rendered(Component name, List<Component> lore, boolean glint) {}
    private final java.util.Map<String, Rendered> renderedItems = new java.util.LinkedHashMap<>(128, 0.75f, true) {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, Rendered> eldest) { return size() > 2048; }
    };

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
        if (type.equals("MATERIAL")) {
            String target = definitions.snapshot().config("levels.yml").getString("materials." + id + ".type", "PLAYER");
            String label = definitions.snapshot().config("messages.yml").getString("material-tooltip.targets." + target,
                    switch (target) { case "WEAPON" -> "武器"; case "EQUIPMENT" -> "装備"; default -> "プレイヤー"; });
            lines.add(mini.deserialize(definitions.snapshot().config("messages.yml").getString("material-tooltip.usage", "<gray>用途：<yellow><target>強化用</yellow></gray>").replace("<target>", label)));
        }
        meta.lore(lines);
        meta.setEnchantmentGlintOverride(glint(id));
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
            if (equipment != null) {
                value.setLevel(Math.clamp(definitions.snapshot().config("equipment.yml").getInt("equipment." + id + ".initial-level", 1), 1, equipment.maxLevel()));
                generateSubstats(value, equipment);
            }
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

    public boolean glint(String id) {
        for (String file : List.of("weapons.yml", "equipments.yml", "equipment.yml", "divine_hearts.yml", "levels.yml")) {
            String root = switch (file) { case "weapons.yml" -> "weapons"; case "divine_hearts.yml" -> "divine-hearts"; case "levels.yml" -> "materials"; default -> "equipment"; };
            var config = definitions.snapshot().config(file);
            if (config != null && config.contains(root + "." + id + ".enchantment-glint"))
                return config.getBoolean(root + "." + id + ".enchantment-glint");
        }
        return false;
    }

    public String rarityLine(int rarity) {
        String fallback = switch (rarity) { case 1 -> "#aaaaaa"; case 2 -> "#55ff55"; case 3 -> "#55aaff"; case 4 -> "#cc88ff"; default -> "#ffaa00"; };
        String color = definitions.snapshot().config("config.yml").getString("rarity-colors." + rarity, fallback);
        return "<" + color + ">" + "★".repeat(Math.max(1, Math.min(10, rarity))) + "</" + color + ">";
    }

    public String displayName(String key) {
        return definitions.snapshot().config("messages.yml").getString("display-names." + key,
                com.github.saku0817.combatcoresystems.util.DisplayNames.japanese(key));
    }

    public String statValue(String key, double value) {
        boolean percent = !key.endsWith("_FLAT") && !key.equals("ATTACK_SPEED");
        return String.format(java.util.Locale.ROOT, "%.1f", percent ? value * 100 : value).replaceFirst("\\.0$", "") + (percent ? "%" : "");
    }

    public void writeInstance(ItemStack item, ItemInstance instance) {
        writeInstance(item, instance, 0);
    }

    public void writeInstance(ItemStack item, ItemInstance instance, int activeSetPieces) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (renderedSnapshot != definitions.snapshot()) {
            renderedSnapshot = definitions.snapshot();
            renderedItems.clear();
        }
        String serialized = gson.toJson(instance);
        String renderKey = serialized + ":" + activeSetPieces;
        Rendered cached = renderedItems.get(renderKey);
        if (cached != null) {
            meta.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, serialized);
            meta.itemName(cached.name()); meta.lore(cached.lore()); meta.setEnchantmentGlintOverride(cached.glint());
            if (!meta.equals(item.getItemMeta())) item.setItemMeta(meta);
            return;
        }
        meta.setEnchantmentGlintOverride(glint(instance.getDefinitionId()));
        meta.getPersistentDataContainer().set(instanceKey, PersistentDataType.STRING, serialized);
        List<Component> lore = new ArrayList<>();
        WeaponDefinition weapon = definitions.snapshot().weapons().get(instance.getDefinitionId());
        EquipmentDefinition equipment = definitions.snapshot().equipment().get(instance.getDefinitionId());
        if (weapon != null) {
            meta.itemName(mini.deserialize(weapon.name()));
            appendWeapon(lore, weapon, instance);
            meta.lore(lore);
            renderedItems.put(renderKey, new Rendered(meta.itemName(), List.copyOf(lore), glint(instance.getDefinitionId())));
            if (!meta.equals(item.getItemMeta())) item.setItemMeta(meta);
            return;
        }
        ConfigurationSection heart = definitions.snapshot().config("divine_hearts.yml").getConfigurationSection("divine-hearts." + instance.getDefinitionId());
        if (heart != null) appendDivineHeart(lore, heart);
        if (equipment != null) meta.itemName(mini.deserialize(equipment.name()));
        if (heart != null) meta.itemName(mini.deserialize(heart.getString("name", instance.getDefinitionId())));
        if (equipment != null) {
            appendEquipment(lore, equipment, instance, activeSetPieces);
        }
        meta.lore(lore);
        renderedItems.put(renderKey, new Rendered(meta.itemName(), List.copyOf(lore), glint(instance.getDefinitionId())));
        if (!meta.equals(item.getItemMeta())) item.setItemMeta(meta);
    }

    private void appendEquipment(List<Component> lore, EquipmentDefinition definition, ItemInstance instance, int activeSetPieces) {
        lore.add(mini.deserialize(rarityLine(definition.rarity())));
        var messages = definitions.snapshot().config("messages.yml");
        String slot = messages.getString("equipment-tooltip.slots." + definition.slot().name(), displayName(definition.slot().name()));
        lore.add(mini.deserialize(messages.getString("equipment-tooltip.slot", "<white>装備部位：<u><slot></u></white>").replace("<slot>", slot)));
        lore.add(mini.deserialize(messages.getString("equipment-tooltip.level", "<white>Lv.<yellow><level></yellow> / <yellow><max_level></yellow></white>")
                .replace("<level>", Integer.toString(instance.getLevel())).replace("<max_level>", Integer.toString(definition.maxLevel()))));
        lore.add(Component.empty());
        double main = com.github.saku0817.combatcoresystems.util.CoreMath.linear(definition.mainAtLevel1(), definition.mainAtMaxLevel(), instance.getLevel(), definition.maxLevel());
        lore.add(mini.deserialize(messages.getString("equipment-tooltip.main-stat", "<yellow>➽ <stat> +<value></yellow>")
                .replace("<stat>", displayName(definition.mainStat().name())).replace("<value>", statValue(definition.mainStat().name(), main))));
        int index = 0;
        for (var entry : instance.getSubstats().entrySet()) {
            boolean open = index++ < instance.getUnlockedSubstats();
            int upgrades = instance.getSubstatUpgrades().getOrDefault(entry.getKey(), 0);
            String template = messages.getString(open ? "equipment-tooltip.substat-open" : "equipment-tooltip.substat-locked",
                    open ? "<white>・<stat> <yellow>+<value></yellow> <aqua>[+<upgrades>]</aqua></white>" : "<gray>・<stat> +<value> [未開放]</gray>");
            lore.add(mini.deserialize(template.replace("<stat>", displayName(entry.getKey())).replace("<value>", statValue(entry.getKey(), entry.getValue()))
                    .replace("<upgrades>", Integer.toString(upgrades))));
        }
        if (!definition.setId().isBlank()) {
            ConfigurationSection set = definitions.snapshot().config("sets.yml").getConfigurationSection("sets." + definition.setId());
            if (set != null) {
                lore.add(Component.empty());
                lore.add(mini.deserialize(messages.getString("equipment-tooltip.set-title", "<yellow><u>「<set>」</u></yellow> <white>シリーズ</white>")
                        .replace("<set>", set.getString("name", definition.setId()))));
                appendSetLine(lore, set, "two-piece", 2, activeSetPieces >= 2);
                appendSetLine(lore, set, "four-piece", 4, activeSetPieces >= 4);
            }
        }
        if (!definition.lore().isEmpty()) { lore.add(Component.empty()); definition.lore().forEach(line -> lore.add(mini.deserialize(line))); }
    }

    private void appendSetLine(List<Component> lore, ConfigurationSection set, String key, int pieces, boolean active) {
        String description = set.getString(key + ".description", summarizeModifiers(set.getConfigurationSection(key + ".modifiers")));
        String template = definitions.snapshot().config("messages.yml").getString(active ? "equipment-tooltip.set-active" : "equipment-tooltip.set-inactive",
                active ? "<green>・<pieces>セット <description> [発動中]</green>" : "<gray>・<pieces>セット <description> [未発動]</gray>");
        lore.add(mini.deserialize(template.replace("<pieces>", Integer.toString(pieces)).replace("<description>", description)));
    }

    private String summarizeModifiers(ConfigurationSection section) {
        if (section == null) return "効果なし";
        return section.getKeys(false).stream().map(key -> displayName(key) + (section.getDouble(key) >= 0 ? "+" : "") + statValue(key, section.getDouble(key))).collect(java.util.stream.Collectors.joining("、"));
    }

    private void appendDivineHeart(List<Component> lore, ConfigurationSection heart) {
        lore.add(mini.deserialize(rarityLine(heart.getInt("rarity", 5))));
        var messages = definitions.snapshot().config("messages.yml");
        lore.add(mini.deserialize(messages.getString("divine-heart-tooltip.slot", "<white>装備部位：<u>神心</u></white>")));
        ConfigurationSection talents = heart.getConfigurationSection("talents");
        if (talents != null) for (String id : talents.getKeys(false)) {
            ConfigurationSection talent = talents.getConfigurationSection(id); if (talent == null) continue;
            lore.add(Component.empty());
            String color = talent.getString("color", "yellow");
            lore.add(mini.deserialize(messages.getString("divine-heart-tooltip.talent-title", "<yellow>➽ <talent_color><u><b>「<name>」</b></u></talent_color></yellow>")
                    .replace("<talent_color>", "<" + color + ">").replace("</talent_color>", "</" + color + ">").replace("<name>", talent.getString("name", id))));
            talent.getStringList("description").forEach(line -> lore.add(mini.deserialize("<white>" + line + "</white>")));
        }
        if (!heart.getStringList("lore").isEmpty()) { lore.add(Component.empty()); heart.getStringList("lore").forEach(line -> lore.add(mini.deserialize(line))); }
    }

    private void appendAbility(List<Component> lore, WeaponDefinition.SkillDefinition ability, String label) {
        if (ability == null) return;
        lore.add(mini.deserialize("<gold>" + label + " — " + ability.name() + "</gold>"));
        lore.add(mini.deserialize("<gray>" + displayName(ability.referenceStat().name()) + " × " + ability.multiplier() + " / " + ability.element().japaneseName()
                + " / CT " + ability.cooldownSeconds() + "秒 / " + ability.charges() + "回 / 範囲 " + ability.radius() + "m</gray>"));
        ability.conditions().forEach((key, value) -> lore.add(Component.text(displayName(key) + ": " + displayName(String.valueOf(value)))));
    }

    private void appendWeapon(List<Component> lore, WeaponDefinition weapon, ItemInstance instance) {
        lore.add(mini.deserialize(rarityLine(weapon.rarity())));
        var config = definitions.snapshot().config("messages.yml");
        var exp = definitions.snapshot().config("levels.yml");
        long required = exp.getString("weapon-exp.mode", "QUADRATIC").equalsIgnoreCase("TABLE")
                ? exp.getLong("weapon-exp.table." + instance.getLevel(), Long.MAX_VALUE)
                : com.github.saku0817.combatcoresystems.util.CoreMath.quadraticExp(exp.getLong("weapon-exp.base", 100), exp.getLong("weapon-exp.growth", 25), instance.getLevel());
        long remaining = instance.getLevel() >= 100 ? 0 : Math.max(0, required - instance.getExp());
        String category = config.getString("weapon-tooltip.categories." + weapon.category().name(), switch (weapon.category()) {
            case MELEE -> "近接"; case RANGED -> "遠距離"; case UNCATEGORIZED -> "未指定";
        });
        String color = config.getString("weapon-tooltip.colors." + weapon.bonusElement().name(), switch (weapon.bonusElement()) {
            case FIRE -> "#ff0000"; case WATER -> "#55aaff"; case WIND -> "#55ffaa"; case THUNDER -> "#cc88ff"; case MOON -> "#ddddff"; case PHYSICAL -> "#ffffff";
        });
        color = definitions.snapshot().config("config.yml").getString("attribute-colors." + weapon.bonusElement().name(), color);
        List<String> defaults = List.of("<white>カテゴリ：<u><category></u></white>",
                "<hover:show_text:'<white>次のレベルまであと <yellow><exp_remaining></yellow></white>'><white>Lv.<yellow><level></yellow> / <yellow>100</yellow></white></hover>",
                "<white>限界突破段階：<yellow><break></yellow> / <yellow>5</yellow></white>",
                "<white>武器攻撃力: <yellow><attack></yellow></white>",
                "<attribute_color><attribute></attribute_color><white>ダメージ <yellow><bonus>%</yellow></white>",
                "<white>装備可能レベル: <yellow><min_level></yellow> ~ <yellow><max_level></yellow></white>");
        List<String> header = config.isList("weapon-tooltip.header") ? config.getStringList("weapon-tooltip.header") : defaults;
        for (String line : header) lore.add(mini.deserialize(line.replace("<category>", category)
                .replace("<exp_remaining>", Long.toString(remaining)).replace("<level>", Integer.toString(instance.getLevel()))
                .replace("<break>", Integer.toString(instance.getLimitBreak())).replace("<attack>", Long.toString(Math.round(weapon.attackAt(instance.getLevel()))))
                .replace("<attribute_color>", "<" + color + ">").replace("</attribute_color>", "</" + color + ">")
                .replace("<attribute>", weapon.bonusElement() == Element.PHYSICAL ? "無属性" : weapon.bonusElement().japaneseName() + "属性")
                .replace("<bonus>", String.format(java.util.Locale.ROOT, "%+.0f", weapon.elementBonus() * 100))
                .replace("<min_level>", Integer.toString(weapon.minimumEquipLevel())).replace("<max_level>", Integer.toString(weapon.maximumEquipLevel()))));
        var talent = weapon.options().talent();
        if (talent != null) appendDescription(lore, "talent", "天賦", talent.name(), talent.description());
        if (weapon.skill() != null) appendDescription(lore, "skill", "スキル", weapon.skill().name(), abilityDescription(weapon.skill()));
        if (weapon.ultimate() != null) appendDescription(lore, "ultimate", "必殺技", weapon.ultimate().name(), abilityDescription(weapon.ultimate()));
        if (!weapon.lore().isEmpty()) { lore.add(Component.empty()); weapon.lore().forEach(line -> lore.add(mini.deserialize(line))); }
    }

    public List<String> abilityDescription(WeaponDefinition.SkillDefinition ability) {
        if (!ability.options().description().isEmpty()) return ability.options().description();
        return List.of(definitions.snapshot().config("messages.yml").getString("weapon-tooltip.ability-summary", "<white><reference> × <multiplier> / <attribute> / CT <cooldown>秒 / <charges>スタック</white>")
                .replace("<reference>", displayName(ability.referenceStat().name())).replace("<multiplier>", Double.toString(ability.multiplier()))
                .replace("<attribute>", ability.element().japaneseName()).replace("<cooldown>", Double.toString(ability.cooldownSeconds()))
                .replace("<charges>", Integer.toString(ability.charges())));
    }

    private void appendDescription(List<Component> lore, String key, String label, String name, List<String> description) {
        var config = definitions.snapshot().config("messages.yml");
        lore.add(mini.deserialize(config.getString("weapon-tooltip." + key + "-title", "<#adff2f>➽ " + label + " <u><b><name></b></u></#adff2f>").replace("<name>", name)));
        description.forEach(line -> lore.add(mini.deserialize("<white>" + line + "</white>")));
    }

    private void appendEffects(List<Component> lore, ConfigurationSection section, String label) {
        if (section == null) return;
        String template = definitions.snapshot().config("messages.yml").getString("item-lore.effect", "<gray><label>: <key> = <value></gray>");
        section.getValues(true).forEach((key, value) -> {
            if (!(value instanceof ConfigurationSection)) lore.add(mini.deserialize(template
                    .replace("<label>", label).replace("<key>", displayName(key)).replace("<value>", value instanceof Number number && key.matches("[A-Z_]+") ? statValue(key, number.doubleValue()) : displayName(String.valueOf(value)))));
        });
    }

    private void generateSubstats(ItemInstance instance, EquipmentDefinition definition) {
        ConfigurationSection fixed = definitions.snapshot().config("equipment.yml").getConfigurationSection("equipment." + definition.id() + ".initial-substats");
        if (fixed != null) {
            fixed.getKeys(false).stream().limit(4).forEach(key -> {
                instance.getSubstats().put(key.toUpperCase(java.util.Locale.ROOT), fixed.getDouble(key));
                instance.getSubstatUpgrades().put(key.toUpperCase(java.util.Locale.ROOT), definitions.snapshot().config("equipment.yml")
                        .getInt("equipment." + definition.id() + ".initial-upgrades." + key, 0));
            });
            instance.setUnlockedSubstats(definitions.snapshot().config("equipment.yml").getInt("equipment." + definition.id() + ".initial-unlocked-substats", 0));
            return;
        }
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
