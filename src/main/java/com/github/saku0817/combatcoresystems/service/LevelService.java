package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.PlayerStats;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public final class LevelService {
    private final DefinitionRegistry definitions;
    private final StatService stats;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public LevelService(DefinitionRegistry definitions, StatService stats) {
        this.definitions = definitions;
        this.stats = stats;
    }

    public long requiredExp(int level) {
        int vanilla = level <= 15 ? 2 * level + 7 : level <= 30 ? 5 * level - 38 : 9 * level - 158;
        return Math.max(1L, Math.round(vanilla * definitions.snapshot().config("levels.yml").getDouble("player.vanilla-exp-multiplier", 5)));
    }

    public void addExp(Player player, PlayerData data, long amount) {
        if (amount <= 0 || data.getLevel() >= 100) return;
        long exp = data.getExp() + amount;
        while (data.getLevel() < 100 && exp >= requiredExp(data.getLevel())) {
            exp -= requiredExp(data.getLevel());
            data.setLevel(data.getLevel() + 1);
            if (data.getGrantedLevelPoints().add(data.getLevel())) data.setSkillPoints(data.getSkillPoints() + 1);
            player.sendMessage(miniMessage.deserialize("<green>Lv." + data.getLevel() + "になりました。</green>"));
        }
        if (data.getLevel() >= 100) exp = 0;
        data.setExp(exp);
        apply(player, data, false);
    }

    public boolean rebirth(Player player, PlayerData data) {
        if (data.getLevel() != 100) return false;
        data.setRebirthCount(data.getRebirthCount() + 1);
        data.setLevel(1);
        data.setExp(0);
        apply(player, data, true);
        return true;
    }

    public void apply(Player player, PlayerData data, boolean fullHeal) {
        PlayerStats value = stats.recalculate(player, data);
        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(value.maxHp());
        var attackDamage = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.setBaseValue(value.atk());
        var attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.setBaseValue(value.value(com.github.saku0817.combatcoresystems.model.StatKey.ATTACK_SPEED));
        player.setLevel(data.getLevel());
        long required = data.getLevel() >= 100 ? 1 : requiredExp(data.getLevel());
        player.setExp(data.getLevel() >= 100 ? 0 : Math.min(0.999999f, (float) data.getExp() / required));
        double desired = fullHeal ? value.maxHp() : Math.min(Math.max(0.1, data.getHealth()), value.maxHp());
        player.setHealth(desired);
    }
}
