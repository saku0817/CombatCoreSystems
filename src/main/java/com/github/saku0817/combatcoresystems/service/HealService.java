package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.PlayerStats;
import com.github.saku0817.combatcoresystems.model.ReferenceStat;
import com.github.saku0817.combatcoresystems.model.StatKey;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class HealService {
    private final PlayerDataService players;
    private final StatService stats;
    private final DamageDisplayService displays;

    public HealService(PlayerDataService players, StatService stats, DamageDisplayService displays) {
        this.players = players; this.stats = stats; this.displays = displays;
    }

    public long heal(LivingEntity source, LivingEntity target, ReferenceStat reference, double multiplier, boolean small) {
        double hp = value(source, ReferenceStat.HP), atk = value(source, ReferenceStat.ATK), def = value(source, ReferenceStat.DEF);
        double referenceValue = switch (reference) { case HP -> hp; case ATK -> atk; case DEF -> def; };
        double healingPower = source instanceof Player p ? stats.get(p, players.require(p)).value(StatKey.HEALING_POWER) : 0;
        long amount = Math.round(CoreMath.heal(referenceValue, multiplier, healingPower));
        double max = target.getAttribute(Attribute.MAX_HEALTH) == null ? target.getHealth() : target.getAttribute(Attribute.MAX_HEALTH).getValue();
        target.setHealth(Math.min(max, target.getHealth() + amount));
        displays.heal(source == null ? target.getUniqueId() : source.getUniqueId(), target, amount, small);
        return amount;
    }

    private double value(LivingEntity source, ReferenceStat stat) {
        if (source instanceof Player player) {
            PlayerStats value = stats.get(player, players.require(player));
            return switch (stat) { case HP -> value.maxHp(); case ATK -> value.atk(); case DEF -> value.def(); };
        }
        Attribute attribute = switch (stat) { case HP -> Attribute.MAX_HEALTH; case ATK -> Attribute.ATTACK_DAMAGE; case DEF -> Attribute.ARMOR; };
        return source.getAttribute(attribute) == null ? 0 : source.getAttribute(attribute).getValue();
    }
}
