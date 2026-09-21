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
    private final LevelService levels;
    private final MobService mobs;
    private TriggerService triggers;
    public void bindTriggers(TriggerService triggers) { this.triggers=triggers; }

    public HealService(PlayerDataService players, StatService stats, DamageDisplayService displays, LevelService levels, MobService mobs) {
        this.players = players; this.stats = stats; this.displays = displays; this.levels = levels; this.mobs = mobs;
    }

    public long heal(LivingEntity source, LivingEntity target, ReferenceStat reference, double multiplier, boolean small) {
        double hp = value(source, ReferenceStat.HP), atk = value(source, ReferenceStat.ATK), def = value(source, ReferenceStat.DEF);
        double referenceValue = switch (reference) { case HP -> hp; case ATK -> atk; case DEF -> def; };
        return healAmount(source,target,referenceValue*multiplier,small);
    }

    public long healAmount(LivingEntity source,LivingEntity target,double baseAmount,boolean small) {
        if (target==null || target.isDead() || !Double.isFinite(baseAmount) || baseAmount<=0) return 0;
        if (source==null) source=target;
        double healingPower = source instanceof Player p ? stats.get(p, players.require(p)).value(StatKey.HEALING_POWER) : 0;
        long amount = Math.round(Math.max(0,baseAmount*(1+healingPower)));
        if (amount<=0) return 0;
        double current=target instanceof Player p ? players.require(p).getHealth() : mobs.health(target);
        double maximum=value(target,ReferenceStat.HP);
        double effective=Math.min(amount,Math.max(0,maximum-current));
        if (target instanceof Player player) {
            PlayerData data = players.require(player); levels.setVirtualHealth(player, data, data.getHealth() + amount);
        } else {
            mobs.setHealth(target, mobs.health(target) + amount);
        }
        displays.heal(source == null ? target.getUniqueId() : source.getUniqueId(), target, amount, small);
        if (triggers!=null) triggers.healed(source,target,amount,effective,amount-effective);
        return amount;
    }

    public double value(LivingEntity source, ReferenceStat stat) {
        if (source instanceof Player player) {
            PlayerStats value = stats.get(player, players.require(player));
            return switch (stat) { case HP -> value.maxHp(); case ATK -> value.atk(); case DEF -> value.def(); };
        }
        if (stat == ReferenceStat.HP) return mobs.maxHealth(source);
        if (stat == ReferenceStat.DEF) return mobs.definition(source).map(definition -> mobs.defense(source,definition,mobs.level(source))).orElse(0.0);
        Attribute attribute = switch (stat) { case HP -> Attribute.MAX_HEALTH; case ATK -> Attribute.ATTACK_DAMAGE; case DEF -> Attribute.ARMOR; };
        return source.getAttribute(attribute) == null ? 0 : source.getAttribute(attribute).getValue();
    }
}
