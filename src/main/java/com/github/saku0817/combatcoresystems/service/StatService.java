package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StatService {
    private final DefinitionRegistry definitions;
    private final CombatStateService combat;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();

    public StatService(DefinitionRegistry definitions, CombatStateService combat) {
        this.definitions = definitions;
        this.combat = combat;
    }

    public PlayerStats get(Player player, PlayerData data) {
        return cache.computeIfAbsent(player.getUniqueId(), ignored -> calculate(player, data));
    }

    public PlayerStats recalculate(Player player, PlayerData data) {
        PlayerStats stats = calculate(player, data);
        cache.put(player.getUniqueId(), stats);
        return stats;
    }

    public void invalidate(UUID uuid) { cache.remove(uuid); }

    private PlayerStats calculate(Player player, PlayerData data) {
        YamlConfiguration levels = definitions.snapshot().config("levels.yml");
        int level = data.getLevel();
        int rebirth = data.getRebirthCount();
        double baseHp = CoreMath.linear(levels.getDouble("player.hp.start", 20), levels.getDouble("player.hp.end", 3000), level, 100)
                + levels.getDouble("player.rebirth.hp", 100) * rebirth;
        double playerBaseAtk = CoreMath.linear(levels.getDouble("player.atk.start", 2), levels.getDouble("player.atk.end", 200), level, 100)
                + levels.getDouble("player.rebirth.atk", 20) * rebirth;
        double baseDef = CoreMath.linear(levels.getDouble("player.def.start", 0), levels.getDouble("player.def.end", 100), level, 100)
                + levels.getDouble("player.rebirth.def", 10) * rebirth;

        EnumMap<StatKey, Double> modifiers = defaults();
        double weaponAtk = 0;
        String activeWeapon = combat.state(player.getUniqueId()).activeWeapon();
        if (activeWeapon.isBlank()) {
            ItemInstance preferred = data.getEquipment().get(EquipmentSlot.MELEE_WEAPON);
            if (preferred == null) preferred = data.getEquipment().get(EquipmentSlot.RANGED_WEAPON);
            if (preferred != null) activeWeapon = preferred.getDefinitionId();
        }
        for (Map.Entry<EquipmentSlot, ItemInstance> equipped : data.getEquipment().entrySet()) {
            ItemInstance item = equipped.getValue();
            WeaponDefinition weapon = definitions.snapshot().weapons().get(item.getDefinitionId());
            if (weapon != null && item.getDefinitionId().equals(activeWeapon)
                    && (equipped.getKey() == EquipmentSlot.MELEE_WEAPON || equipped.getKey() == EquipmentSlot.RANGED_WEAPON)) {
                weaponAtk += CoreMath.linear(weapon.attackAtLevel1(), weapon.attackAtLevel100(), item.getLevel(), 100);
                if (weapon.bonusElement() != Element.PHYSICAL) add(modifiers, damageKey(weapon.bonusElement()), weapon.elementBonus());
            }
            EquipmentDefinition equipment = definitions.snapshot().equipment().get(item.getDefinitionId());
            if (equipment != null) {
                add(modifiers, equipment.mainStat(), CoreMath.linear(equipment.mainAtLevel1(), equipment.mainAtMaxLevel(), item.getLevel(), equipment.maxLevel()));
                item.getSubstats().forEach((key, value) -> {
                    try { add(modifiers, StatKey.valueOf(key), value); } catch (IllegalArgumentException ignored) {}
                });
            }
        }
        applyEffects(data, modifiers);
        applySkillTree(data, modifiers);
        applySetBonuses(data, modifiers);
        applyDivineHeart(data, modifiers);

        double vanillaArmor = 0;
        AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
        if (armor != null) vanillaArmor = armor.getValue();
        double hp = baseHp * (1 + modifiers.get(StatKey.HP_PERCENT)) + modifiers.get(StatKey.HP_FLAT);
        double atk = (playerBaseAtk + weaponAtk) * (1 + modifiers.get(StatKey.ATK_PERCENT)) + modifiers.get(StatKey.ATK_FLAT);
        double def = (baseDef + vanillaArmor) * (1 + modifiers.get(StatKey.DEF_PERCENT)) + modifiers.get(StatKey.DEF_FLAT);
        return new PlayerStats(level, hp, atk, def, modifiers);
    }

    private EnumMap<StatKey, Double> defaults() {
        EnumMap<StatKey, Double> values = new EnumMap<>(StatKey.class);
        for (StatKey key : StatKey.values()) values.put(key, 0.0);
        values.put(StatKey.CRIT_RATE, 0.05);
        values.put(StatKey.CRIT_DAMAGE, 0.50);
        values.put(StatKey.ATTACK_SPEED, 4.0);
        return values;
    }

    private void applyEffects(PlayerData data, EnumMap<StatKey, Double> modifiers) {
        for (TimedEffect effect : data.getBuffs()) applyEffect(effect, modifiers);
        for (TimedEffect effect : data.getDebuffs()) applyEffect(effect, modifiers);
    }

    private void applyEffect(TimedEffect effect, EnumMap<StatKey, Double> modifiers) {
        BuffDefinition definition = definitions.snapshot().buffs().get(effect.getId());
        if (definition == null) return;
        definition.flatModifiers().forEach((key, value) -> add(modifiers, key, value * effect.getStacks()));
        definition.percentModifiers().forEach((key, value) -> add(modifiers, key, value * effect.getStacks()));
    }

    private void applySkillTree(PlayerData data, EnumMap<StatKey, Double> modifiers) {
        var trees = definitions.snapshot().config("skill_trees.yml").getConfigurationSection("trees");
        if (trees == null) return;
        for (String tree : trees.getKeys(false)) {
            for (var node : data.getSkillNodes().entrySet()) {
                var section = trees.getConfigurationSection(tree + ".nodes." + node.getKey());
                if (section == null) continue;
                for (int rank = 1; rank <= node.getValue(); rank++) applyModifierSection(section.getConfigurationSection("ranks." + rank + ".modifiers"), modifiers);
            }
        }
    }

    private void applySetBonuses(PlayerData data, EnumMap<StatKey, Double> modifiers) {
        Map<String, Integer> counts = new HashMap<>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemInstance item = data.getEquipment().get(slot);
            EquipmentDefinition definition = item == null ? null : definitions.snapshot().equipment().get(item.getDefinitionId());
            if (definition != null && !definition.setId().isBlank()) counts.merge(definition.setId(), 1, Integer::sum);
        }
        var sets = definitions.snapshot().config("sets.yml");
        counts.forEach((id, count) -> {
            if (count >= 2) applyModifierSection(sets.getConfigurationSection("sets." + id + ".two-piece.modifiers"), modifiers);
            if (count >= 4) applyModifierSection(sets.getConfigurationSection("sets." + id + ".four-piece.modifiers"), modifiers);
        });
    }

    private void applyDivineHeart(PlayerData data, EnumMap<StatKey, Double> modifiers) {
        ItemInstance heart = data.getEquipment().get(EquipmentSlot.DIVINE_HEART);
        if (heart != null) applyModifierSection(definitions.snapshot().config("divine_hearts.yml")
                .getConfigurationSection("divine-hearts." + heart.getDefinitionId() + ".modifiers"), modifiers);
    }

    private void applyModifierSection(org.bukkit.configuration.ConfigurationSection section, EnumMap<StatKey, Double> modifiers) {
        if (section == null) return;
        for (String raw : section.getKeys(false)) {
            try { add(modifiers, StatKey.valueOf(raw.toUpperCase(java.util.Locale.ROOT)), section.getDouble(raw)); }
            catch (IllegalArgumentException ignored) {}
        }
    }

    private void add(EnumMap<StatKey, Double> values, StatKey key, double value) { values.merge(key, value, Double::sum); }
    private StatKey damageKey(Element element) { return StatKey.valueOf(element.name() + "_DAMAGE"); }
}
