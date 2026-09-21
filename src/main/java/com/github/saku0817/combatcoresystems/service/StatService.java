package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StatService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final CombatStateService combat;
    private final ItemService items;
    private final NamespacedKey itemIdKey;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> cacheTicks = new HashMap<>();
    private DefinitionRegistry.Snapshot cachedDefinitions;
    private DefinitionRegistry.Snapshot talentDefinitions;
    private Set<WeaponOptions.Hand> talentHands = Set.of();
    private final Map<UUID, Set<String>> activeTalents = new HashMap<>();
    private BuffService buffs;
    private DynamicEffectService dynamic;
    public void bindEffects(BuffService buffs,DynamicEffectService dynamic) { this.buffs=buffs; this.dynamic=dynamic; }

    public StatService(JavaPlugin plugin, DefinitionRegistry definitions, CombatStateService combat, ItemService items) {
        this.plugin = plugin;
        this.definitions = definitions;
        this.combat = combat;
        this.items = items;
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    public PlayerStats get(Player player, PlayerData data) {
        if (cachedDefinitions != definitions.snapshot()) { cache.clear(); cacheTicks.clear(); cachedDefinitions = definitions.snapshot(); }
        // Share repeated HUD/damage queries within one tick, never cache external changes indefinitely.
        PlayerStats value = cache.get(player.getUniqueId());
        return value != null && Objects.equals(cacheTicks.get(player.getUniqueId()), org.bukkit.Bukkit.getCurrentTick())
                ? value : recalculate(player, data);
    }

    public PlayerStats recalculate(Player player, PlayerData data) {
        PlayerStats stats = calculate(player, data, false);
        synchronizeAttackAttribute(player, stats.atk());
        cache.put(player.getUniqueId(), stats);
        cacheTicks.put(player.getUniqueId(), org.bukkit.Bukkit.getCurrentTick());
        return stats;
    }

    public void invalidate(UUID uuid) { cache.remove(uuid); }

    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
        cacheTicks.remove(event.getPlayer().getUniqueId());
        activeTalents.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler public void onHeldItem(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> invalidate(event.getPlayer().getUniqueId()));
    }

    @EventHandler public void onSwapHand(PlayerSwapHandItemsEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> invalidate(event.getPlayer().getUniqueId()));
    }

    public PlayerStats describe(Player player, PlayerData data) { return calculate(player, data, true); }

    private PlayerStats calculate(Player player, PlayerData data, boolean detailed) {
        if (talentDefinitions != definitions.snapshot()) {
            talentDefinitions = definitions.snapshot();
            talentHands = java.util.stream.Stream.concat(talentDefinitions.weapons().values().stream(),
                            talentDefinitions.weaponStages().values().stream().flatMap(List::stream))
                    .map(weapon -> weapon.options().talent()).filter(Objects::nonNull)
                    .map(WeaponOptions.Talent::hand).collect(java.util.stream.Collectors.toSet());
        }
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
        Map<String, Map<StatKey, Double>> sources = new LinkedHashMap<>();
        if (detailed) sources.put("基礎値", Map.of(StatKey.HP_FLAT, baseHp, StatKey.ATK_FLAT, playerBaseAtk, StatKey.DEF_FLAT, baseDef,
                StatKey.CRIT_RATE, modifiers.get(StatKey.CRIT_RATE), StatKey.CRIT_DAMAGE, modifiers.get(StatKey.CRIT_DAMAGE), StatKey.ATTACK_SPEED, modifiers.get(StatKey.ATTACK_SPEED)));
        double weaponAtk = vanillaWeaponAttack(player, levels);
        if (detailed && weaponAtk != 0) sources.put("バニラ武器から", Map.of(StatKey.ATK_FLAT, weaponAtk));
        Set<String> currentTalents = new HashSet<>();
        Set<String> previousTalents = activeTalents.getOrDefault(player.getUniqueId(), Set.of());
        Set<String> counted = new HashSet<>();
        int selected = player.getInventory().getHeldItemSlot();
        for (int slot = 0; slot <= 40; slot++) {
            if (slot >= 36 && slot <= 39) continue;
            if (slot != selected && slot != 40 && !talentHands.contains(WeaponOptions.Hand.INVENTORY)
                    && !(slot <= 8 && talentHands.contains(WeaponOptions.Hand.HOT_BAR))) continue;
            ItemStack stack = player.getInventory().getItem(slot);
            if (!definitions.snapshot().weapons().containsKey(items.id(stack).orElse(""))) continue;
            ItemInstance instance = items.instance(stack).orElse(null);
            WeaponDefinition weapon = instance == null ? null : definitions.snapshot().weapon(instance);
            if (weapon == null) continue;
            if (!counted.add(instance.getInstanceId())) continue;
            if (!weapon.canEquip(level)) continue;
            if (slot == selected || slot == 40) {
                weaponAtk += weapon.attackFor(level, instance.getLevel());
                if (detailed) sources.computeIfAbsent("武器：" + weapon.name(), ignored -> new EnumMap<>(StatKey.class)).merge(StatKey.ATK_FLAT, weapon.attackFor(level, instance.getLevel()), Double::sum);
                if (weapon.bonusElement() != Element.PHYSICAL) add(modifiers, damageKey(weapon.bonusElement()), weapon.elementBonus());
                if (detailed && weapon.bonusElement() != Element.PHYSICAL) sources.get("武器：" + weapon.name()).merge(damageKey(weapon.bonusElement()), weapon.elementBonus(), Double::sum);
            }
            WeaponOptions.Talent talent = weapon.options().talent();
            if (talent != null && talent.hand().includes(slot, selected)) {
                EnumMap<StatKey, Double> beforeTalent = detailed ? new EnumMap<>(modifiers) : null;
                talent.modifiers().forEach((key, value) -> add(modifiers, key, value * talent.multiplier()));
                recordDelta(sources, "天賦：" + talent.name(), beforeTalent, modifiers);
                String activation = instance.getInstanceId() + ":" + talent.name();
                currentTalents.add(activation);
                if (!previousTalents.contains(activation)) {
                    String message = definitions.snapshot().config("messages.yml").getString("ability-announcement.talent", "<green>天賦発動：<name></green>");
                    if (!message.isBlank()) player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message.replace("<name>", talent.name())));
                }
            }
        }
        activeTalents.put(player.getUniqueId(), currentTalents);
        EnumMap<StatKey, Double> beforeEquipment = detailed ? new EnumMap<>(modifiers) : null;
        for (Map.Entry<EquipmentSlot, ItemInstance> equipped : data.getEquipment().entrySet()) {
            ItemInstance item = equipped.getValue();
            EquipmentDefinition equipment = definitions.snapshot().equipment().get(item.getDefinitionId());
            if (equipment != null) {
                add(modifiers, item.mainStat(equipment), item.mainValue(equipment));
                item.getSubstats().entrySet().stream().limit(item.getUnlockedSubstats()).forEach(entry -> {
                    String key = entry.getKey(); double value = entry.getValue();
                    try { add(modifiers, StatKey.valueOf(key), value); } catch (IllegalArgumentException ignored) {}
                });
            }
        }
        recordDelta(sources, "装備から", beforeEquipment, modifiers);
        List<TimedEffect> activeEffects=buffs==null ? java.util.stream.Stream.concat(data.getBuffs().stream(), data.getDebuffs().stream()).toList() : buffs.effects(player);
        for (TimedEffect effect : activeEffects) {
            EnumMap<StatKey, Double> before = detailed ? new EnumMap<>(modifiers) : null;
            applyEffect(effect, modifiers);
            recordDelta(sources, "バフ・デバフ：" + definitions.snapshot().config("buffs.yml").getString("buffs." + effect.getId() + ".name", effect.getId()), before, modifiers);
        }
        EnumMap<StatKey, Double> beforeTree = detailed ? new EnumMap<>(modifiers) : null;
        applySkillTree(data, modifiers); recordDelta(sources, "スキルツリーから", beforeTree, modifiers);
        for (var set : SetEffectService.counts(definitions.snapshot(), data).entrySet()) {
            EnumMap<StatKey, Double> before = detailed ? new EnumMap<>(modifiers) : null;
            var config = definitions.snapshot().config("sets.yml");
            if (set.getValue() >= 2) applyModifierSection(config.getConfigurationSection("sets." + set.getKey() + ".two-piece.modifiers"), modifiers);
            if (set.getValue() >= 4) applyModifierSection(config.getConfigurationSection("sets." + set.getKey() + ".four-piece.modifiers"), modifiers);
            recordDelta(sources, "セット効果：" + config.getString("sets." + set.getKey() + ".name", set.getKey()), before, modifiers);
        }
        EnumMap<StatKey, Double> beforeHeart = detailed ? new EnumMap<>(modifiers) : null;
        applyDivineHeart(data, modifiers); recordDelta(sources, "神心から", beforeHeart, modifiers);

        double vanillaArmor = 0;
        if (dynamic!=null) {
            var before=detailed ? new EnumMap<>(modifiers) : null;
            dynamic.modifiers(player.getUniqueId()).forEach((key,value) -> add(modifiers,key,value));
            recordDelta(sources,"動的効果から",before,modifiers);
        }
        Map<StatKey,Double> finalOverrides=new EnumMap<>(StatKey.class);
        for (TimedEffect effect : activeEffects) {
            if (!effect.isPermanent() && effect.getRemainingMillis()<=0) continue;
            var overrides=definitions.snapshot().config("buffs.yml").getConfigurationSection("buffs."+effect.getId()+".modifiers.override");
            if (overrides==null) continue;
            for (String key : overrides.getKeys(false)) {
                StatKey stat=StatKey.valueOf(key); double value=overrides.getDouble(key);
                finalOverrides.put(stat,value);
            }
        }
        var beforeOverrides=detailed ? new EnumMap<>(modifiers) : null;
        finalOverrides.forEach((key,value) -> { if (!Set.of(StatKey.HP_FLAT,StatKey.ATK_FLAT,StatKey.DEF_FLAT).contains(key)) modifiers.put(key,value); });
        recordDelta(sources,"固定値補正（バフ・デバフ）",beforeOverrides,modifiers);
        AttributeInstance armor = player.getAttribute(Attribute.ARMOR);
        if (armor != null) vanillaArmor = armor.getValue();
        if (detailed && vanillaArmor != 0) sources.put("バニラ防具から", Map.of(StatKey.DEF_FLAT, vanillaArmor));
        double hp = finalOverrides.getOrDefault(StatKey.HP_FLAT,baseHp * (1 + modifiers.get(StatKey.HP_PERCENT)) + modifiers.get(StatKey.HP_FLAT));
        double atk = finalOverrides.getOrDefault(StatKey.ATK_FLAT,CoreMath.attack(playerBaseAtk, weaponAtk, modifiers.get(StatKey.ATK_PERCENT), modifiers.get(StatKey.ATK_FLAT)));
        double def = finalOverrides.getOrDefault(StatKey.DEF_FLAT,(baseDef + vanillaArmor) * (1 + modifiers.get(StatKey.DEF_PERCENT)) + modifiers.get(StatKey.DEF_FLAT));
        if (detailed) {
            var original=Map.of(StatKey.HP_FLAT,baseHp*(1+modifiers.get(StatKey.HP_PERCENT))+modifiers.get(StatKey.HP_FLAT),
                    StatKey.ATK_FLAT,CoreMath.attack(playerBaseAtk,weaponAtk,modifiers.get(StatKey.ATK_PERCENT),modifiers.get(StatKey.ATK_FLAT)),
                    StatKey.DEF_FLAT,(baseDef+vanillaArmor)*(1+modifiers.get(StatKey.DEF_PERCENT))+modifiers.get(StatKey.DEF_FLAT));
            for (var key : original.keySet()) if (finalOverrides.containsKey(key))
                sources.computeIfAbsent("固定値補正（バフ・デバフ）",ignored -> new EnumMap<>(StatKey.class)).put(key,finalOverrides.get(key)-original.get(key));
        }
        return new PlayerStats(level, hp, atk, def, modifiers).withSources(sources);
    }

    private void recordDelta(Map<String, Map<StatKey, Double>> sources, String label, Map<StatKey, Double> before, Map<StatKey, Double> after) {
        if (before == null) return;
        for (StatKey key : StatKey.values()) {
            double delta = after.getOrDefault(key, 0.0) - before.getOrDefault(key, 0.0);
            if (Math.abs(delta) > 1e-12) sources.computeIfAbsent(label, ignored -> new EnumMap<>(StatKey.class)).merge(key, delta, Double::sum);
        }
    }

    private double vanillaWeaponAttack(Player player, YamlConfiguration levels) {
        if (!levels.getBoolean("player.vanilla-weapons.enabled", true)) return 0;
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) return 0;
        if (held.hasItemMeta()) {
            String itemId = held.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
            if (itemId != null && definitions.snapshot().weapons().containsKey(itemId)) return 0;
        }
        double value = levels.getDouble("player.vanilla-weapons.attack-values." + held.getType().name(), 0);
        return Math.max(0, value * levels.getDouble("player.vanilla-weapons.conversion-multiplier", 1));
    }

    /** Native held-item modifiers must not be added a second time to CCS's final ATK. */
    static void synchronizeAttackAttribute(Player player, double finalAttack) {
        AttributeInstance attribute = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attribute == null) return;
        double add = 0, scalar = 0, product = 1;
        for (org.bukkit.attribute.AttributeModifier modifier : attribute.getModifiers()) {
            switch (modifier.getOperation()) {
                case ADD_NUMBER -> add += modifier.getAmount();
                case ADD_SCALAR -> scalar += modifier.getAmount();
                case MULTIPLY_SCALAR_1 -> product *= 1 + modifier.getAmount();
            }
        }
        double factor = (1 + scalar) * product;
        if (factor > 0 && Double.isFinite(factor)) {
            double base = CoreMath.nativeAttackBase(finalAttack, add, scalar, product);
            if (Double.compare(attribute.getBaseValue(), base) != 0) attribute.setBaseValue(base);
        }
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
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.RESONANCE)) {
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
