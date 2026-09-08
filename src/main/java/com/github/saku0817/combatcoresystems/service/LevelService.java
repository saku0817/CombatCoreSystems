package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.PlayerStats;
import com.github.saku0817.combatcoresystems.util.CoreMath;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class LevelService {
    private final DefinitionRegistry definitions;
    private final StatService stats;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public LevelService(DefinitionRegistry definitions, StatService stats) {
        this.definitions = definitions;
        this.stats = stats;
    }

    public long requiredExp(int level) {
        ConfigurationSection config = definitions.snapshot().config("levels.yml").getConfigurationSection("player.required-exp");
        if (config == null) return CoreMath.quadraticExp(100, 25, level);
        String mode = config.getString("mode", "QUADRATIC").toUpperCase(Locale.ROOT);
        long base = Math.max(1, config.getLong("base", 100));
        long growth = Math.max(0, config.getLong("growth", 25));
        return switch (mode) {
            case "FIXED" -> base;
            case "LINEAR" -> Math.max(1, base + growth * Math.max(0, level - 1L));
            case "TABLE" -> Math.max(1, config.getLong("values." + level, base));
            default -> Math.max(1, CoreMath.quadraticExp(base, growth, level));
        };
    }

    public void addExp(Player player, PlayerData data, long amount) {
        int maxLevel = Math.min(100, Math.max(1, definitions.snapshot().config("levels.yml").getInt("player.max-level", 100)));
        if (amount <= 0 || data.getLevel() >= maxLevel) return;
        PlayerStats before = stats.recalculate(player, data);
        int oldLevel = data.getLevel();
        long exp = data.getExp() + amount;
        while (data.getLevel() < maxLevel && exp >= requiredExp(data.getLevel())) {
            exp -= requiredExp(data.getLevel());
            data.setLevel(data.getLevel() + 1);
            if (data.getGrantedLevelPoints().add(data.getLevel())) data.setSkillPoints(data.getSkillPoints() + 1);
        }
        if (data.getLevel() >= maxLevel) exp = 0;
        data.setExp(exp);
        apply(player, data, false);
        PlayerStats after = stats.get(player, data);
        player.sendMessage(miniMessage.deserialize(message("exp-gained", "<aqua>独自EXP +<amount>（<current>/<required>）</aqua>")
                .replace("<amount>", Long.toString(amount)).replace("<current>", Long.toString(data.getExp()))
                .replace("<required>", Long.toString(data.getLevel() >= maxLevel ? 0 : requiredExp(data.getLevel())))));
        if (data.getLevel() > oldLevel) {
            player.sendMessage(miniMessage.deserialize(message("level-up", "<green>Lv.<level>になりました。 HP +<hp_gain> / ATK +<atk_gain> / DEF +<def_gain></green>")
                    .replace("<old_level>", Integer.toString(oldLevel)).replace("<level>", Integer.toString(data.getLevel()))
                    .replace("<hp_gain>", format(after.maxHp() - before.maxHp()))
                    .replace("<atk_gain>", format(after.atk() - before.atk()))
                    .replace("<def_gain>", format(after.def() - before.def()))));
        }
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
        applyInternal(player, data, fullHeal ? HealthMode.FULL : HealthMode.CURRENT);
    }

    public void restore(Player player, PlayerData data) {
        applyInternal(player, data, HealthMode.STORED);
    }

    private void applyInternal(Player player, PlayerData data, HealthMode healthMode) {
        PlayerStats value = stats.recalculate(player, data);
        var maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double physicalMaximum = Math.min(value.maxHp(), Math.max(20, definitions.snapshot().config("levels.yml").getDouble("player.minecraft-max-health", 1024)));
        if (maxHealth != null) maxHealth.setBaseValue(physicalMaximum);
        var attackDamage = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.setBaseValue(value.atk());
        var attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed != null) attackSpeed.setBaseValue(value.value(com.github.saku0817.combatcoresystems.model.StatKey.ATTACK_SPEED));
        player.setHealthScaled(true);
        player.setHealthScale(Math.max(1, definitions.snapshot().config("levels.yml").getDouble("player.minecraft-health-scale", 20)));
        double source = switch (healthMode) { case FULL -> value.maxHp(); case STORED, CURRENT -> data.getHealth(); };
        double desired = CoreMath.preservedHealth(source, value.maxHp());
        data.setHealth(desired);
        player.setHealth(CoreMath.toPhysicalHealth(desired, value.maxHp(), physicalMaximum));
    }

    public void setVirtualHealth(Player player, PlayerData data, double health) {
        PlayerStats value = stats.get(player, data); double desired = Math.max(0, Math.min(value.maxHp(), health)); data.setHealth(desired);
        var attribute = player.getAttribute(Attribute.MAX_HEALTH); double physicalMaximum = attribute == null ? Math.min(1024, value.maxHp()) : attribute.getValue();
        player.setHealth(CoreMath.toPhysicalHealth(desired, value.maxHp(), physicalMaximum));
    }

    public void capturePhysicalHealth(Player player, PlayerData data) {
        PlayerStats value = stats.get(player, data); var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        double physicalMaximum = attribute == null ? Math.max(1, player.getHealth()) : attribute.getValue();
        data.setHealth(CoreMath.toVirtualHealth(player.getHealth(), physicalMaximum, value.maxHp()));
    }

    private String message(String path, String fallback) { return definitions.snapshot().config("messages.yml").getString(path, fallback); }
    private String format(double value) { return String.format(Locale.ROOT, "%.1f", Math.max(0, value)); }
    private enum HealthMode { CURRENT, STORED, FULL }
}
