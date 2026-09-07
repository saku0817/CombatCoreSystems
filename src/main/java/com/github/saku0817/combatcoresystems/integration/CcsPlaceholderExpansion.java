package com.github.saku0817.combatcoresystems.integration;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.service.*;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class CcsPlaceholderExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final LevelService levels;
    private final CombatStateService combat;
    private final ElementService elements;
    private final SkillService skills;
    private final PartyService parties;

    public CcsPlaceholderExpansion(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                                   LevelService levels, CombatStateService combat, ElementService elements,
                                   SkillService skills, PartyService parties) {
        this.plugin = plugin;
        this.definitions = definitions; this.players = players; this.stats = stats; this.levels = levels;
        this.combat = combat; this.elements = elements; this.skills = skills; this.parties = parties;
    }
    @Override public @NotNull String getIdentifier() { return "ccs"; }
    @Override public @NotNull String getAuthor() { return "s3_q3x"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override public @Nullable String onRequest(OfflinePlayer offline, @NotNull String parameter) {
        Player player = offline.getPlayer(); if (player == null) return "";
        PlayerData data = players.find(player.getUniqueId()).orElse(null); if (data == null) return "";
        PlayerStats value = stats.get(player, data);
        String key = parameter.toLowerCase(Locale.ROOT);
        if (key.startsWith("buff_") && key.endsWith("_active")) return bool(hasEffect(data, key.substring(5, key.length() - 7)));
        if (key.startsWith("buff_") && key.endsWith("_stacks")) return Integer.toString(effectStacks(data, key.substring(5, key.length() - 7)));
        if (key.startsWith("buff_") && key.endsWith("_remaining")) return format(effectRemaining(data, key.substring(5, key.length() - 10)) / 1000.0);
        return switch (key) {
            case "player_name" -> player.getName(); case "level" -> Integer.toString(data.getLevel()); case "exp" -> Long.toString(data.getExp());
            case "exp_required" -> Long.toString(data.getLevel() >= maxLevel() ? 0 : levels.requiredExp(data.getLevel()));
            case "exp_remaining" -> Long.toString(data.getLevel() >= maxLevel() ? 0 : levels.requiredExp(data.getLevel()) - data.getExp());
            case "rebirth_count" -> Integer.toString(data.getRebirthCount()); case "skill_points" -> Integer.toString(data.getSkillPoints());
            case "hp" -> Long.toString(Math.round(player.getHealth())); case "max_hp" -> Long.toString(Math.round(value.maxHp()));
            case "atk" -> format(value.atk()); case "def" -> format(value.def()); case "crit_rate" -> format(value.value(StatKey.CRIT_RATE));
            case "crit_damage" -> format(value.value(StatKey.CRIT_DAMAGE)); case "healing_power" -> format(value.value(StatKey.HEALING_POWER));
            case "cooltime" -> format(value.value(StatKey.COOLDOWN)); case "attack_speed" -> format(value.value(StatKey.ATTACK_SPEED));
            case "fire_damage" -> format(value.elementDamage(Element.FIRE)); case "fire_resistance" -> format(value.resistance(Element.FIRE));
            case "water_damage" -> format(value.elementDamage(Element.WATER)); case "water_resistance" -> format(value.resistance(Element.WATER));
            case "wind_damage" -> format(value.elementDamage(Element.WIND)); case "wind_resistance" -> format(value.resistance(Element.WIND));
            case "thunder_damage" -> format(value.elementDamage(Element.THUNDER)); case "thunder_resistance" -> format(value.resistance(Element.THUNDER));
            case "moon_damage" -> format(value.elementDamage(Element.MOON)); case "moon_resistance" -> format(value.resistance(Element.MOON));
            case "combat" -> bool(combat.inCombat(player.getUniqueId())); case "combat_display" -> combat.inCombat(player.getUniqueId()) ? "戦闘中" : "非戦闘";
            case "combat_remaining" -> combat.isForced(player.getUniqueId()) ? "∞" : format(combat.remainingMillis(player.getUniqueId()) / 1000.0);
            case "skill_ready" -> bool(skills.status(player.getUniqueId(), false).ready()); case "skill_status" -> skills.status(player.getUniqueId(), false).ready() ? "ready" : "cooldown";
            case "skill_status_display" -> skills.status(player.getUniqueId(), false).ready() ? "発動可能" : "あと" + format(skills.status(player.getUniqueId(), false).remainingSeconds()) + "秒";
            case "skill_cooldown" -> format(skills.status(player.getUniqueId(), false).remainingSeconds()); case "skill_charges" -> Integer.toString(skills.status(player.getUniqueId(), false).charges());
            case "ultimate_ready" -> bool(skills.status(player.getUniqueId(), true).ready()); case "ultimate_status" -> skills.status(player.getUniqueId(), true).ready() ? "ready" : "cooldown";
            case "ultimate_status_display" -> skills.status(player.getUniqueId(), true).ready() ? "発動可能" : "あと" + format(skills.status(player.getUniqueId(), true).remainingSeconds()) + "秒";
            case "ultimate_cooldown" -> format(skills.status(player.getUniqueId(), true).remainingSeconds()); case "ultimate_charges" -> Integer.toString(skills.status(player.getUniqueId(), true).charges());
            case "melee_weapon" -> equipment(data, EquipmentSlot.MELEE_WEAPON); case "melee_weapon_level" -> equipmentLevel(data, EquipmentSlot.MELEE_WEAPON);
            case "melee_weapon_limitbreak" -> equipmentLimit(data, EquipmentSlot.MELEE_WEAPON); case "ranged_weapon" -> equipment(data, EquipmentSlot.RANGED_WEAPON);
            case "ranged_weapon_level" -> equipmentLevel(data, EquipmentSlot.RANGED_WEAPON); case "ranged_weapon_limitbreak" -> equipmentLimit(data, EquipmentSlot.RANGED_WEAPON);
            case "head" -> equipment(data, EquipmentSlot.HEAD); case "chest" -> equipment(data, EquipmentSlot.CHEST); case "legs" -> equipment(data, EquipmentSlot.LEGS);
            case "feet" -> equipment(data, EquipmentSlot.FEET); case "resonance" -> equipment(data, EquipmentSlot.RESONANCE); case "divine_heart" -> equipment(data, EquipmentSlot.DIVINE_HEART);
            case "element_count" -> Integer.toString(elements.remaining(player.getUniqueId()).size()); case "elements", "elements_display" -> String.join(",", elements.remaining(player.getUniqueId()).keySet().stream().map(Element::japaneseName).toList());
            case "buff_count" -> Integer.toString(data.getBuffs().size()); case "debuff_count" -> Integer.toString(data.getDebuffs().size());
            case "buffs" -> String.join(",", data.getBuffs().stream().map(TimedEffect::getId).toList()); case "debuffs" -> String.join(",", data.getDebuffs().stream().map(TimedEffect::getId).toList());
            case "party" -> parties.partyId(player.getUniqueId()).isPresent() ? "true" : "false"; case "party_id" -> parties.partyId(player.getUniqueId()).orElse("");
            case "party_leader" -> parties.findByPlayer(player.getUniqueId()).map(p -> p.getLeader()).orElse(""); case "party_size" -> Integer.toString(parties.findByPlayer(player.getUniqueId()).map(p -> p.getMembers().size()).orElse(0));
            case "party_max_size" -> "4"; case "party_is_leader" -> bool(parties.findByPlayer(player.getUniqueId()).map(p -> p.leader().equals(player.getUniqueId())).orElse(false));
            case "pvp" -> bool(data.isPvpEnabled()); case "pvp_display" -> data.isPvpEnabled() ? "ON" : "OFF";
            case "encyclopedia_mob_discovered" -> Integer.toString(data.getDiscoveredMobs().size()); case "encyclopedia_mob_total" -> Integer.toString(mobTotal());
            case "encyclopedia_boss_discovered" -> Integer.toString(data.getDiscoveredBosses().size()); case "encyclopedia_boss_total" -> Integer.toString(definitions.snapshot().bosses().size());
            case "encyclopedia_discovered" -> Integer.toString(data.getDiscoveredMobs().size() + data.getDiscoveredBosses().size());
            case "encyclopedia_total" -> Integer.toString(mobTotal() + definitions.snapshot().bosses().size());
            case "encyclopedia_progress" -> progress(data.getDiscoveredMobs().size() + data.getDiscoveredBosses().size(), mobTotal() + definitions.snapshot().bosses().size());
            case "encyclopedia_mob_progress" -> progress(data.getDiscoveredMobs().size(), mobTotal()); case "encyclopedia_boss_progress" -> progress(data.getDiscoveredBosses().size(), definitions.snapshot().bosses().size());
            default -> null;
        };
    }
    private String equipment(PlayerData data, EquipmentSlot slot) { ItemInstance item = data.getEquipment().get(slot); return item == null ? "" : item.getDefinitionId(); }
    private String equipmentLevel(PlayerData data, EquipmentSlot slot) { ItemInstance item = data.getEquipment().get(slot); return item == null ? "0" : Integer.toString(item.getLevel()); }
    private String equipmentLimit(PlayerData data, EquipmentSlot slot) { ItemInstance item = data.getEquipment().get(slot); return item == null ? "0" : Integer.toString(item.getLimitBreak()); }
    private boolean hasEffect(PlayerData data, String id) { return data.getBuffs().stream().anyMatch(e -> e.getId().equals(id)); }
    private int effectStacks(PlayerData data, String id) { return data.getBuffs().stream().filter(e -> e.getId().equals(id)).mapToInt(TimedEffect::getStacks).findFirst().orElse(0); }
    private long effectRemaining(PlayerData data, String id) { return data.getBuffs().stream().filter(e -> e.getId().equals(id)).mapToLong(TimedEffect::getRemainingMillis).findFirst().orElse(0); }
    private String bool(boolean value) { return Boolean.toString(value); } private String format(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private String progress(int current, int total) { return total == 0 ? "0.0" : format(current * 100.0 / total); }
    private int maxLevel() { return Math.min(100, Math.max(1, definitions.snapshot().config("levels.yml").getInt("player.max-level", 100))); }
    private int mobTotal() { return definitions.snapshot().mobs().size() + definitions.snapshot().vanillaMobs().size(); }
}
