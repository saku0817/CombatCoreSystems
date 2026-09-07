package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public final class SkillTreeService {
    public static final long CHANGE_COOLDOWN_MILLIS = Duration.ofHours(24).toMillis();
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;

    public SkillTreeService(DefinitionRegistry definitions, PlayerDataService players, StatService stats) {
        this.definitions = definitions; this.players = players; this.stats = stats;
    }

    public Result acquire(Player player, String treeId, String nodeId) {
        PlayerData data = players.require(player);
        ConfigurationSection node = node(treeId, nodeId);
        if (node == null) return Result.MISSING;
        int current = data.getSkillNodes().getOrDefault(nodeId, 0);
        int max = Math.max(1, node.getInt("max-rank", node.getConfigurationSection("ranks") == null ? 1 : node.getConfigurationSection("ranks").getKeys(false).size()));
        if (current >= max) return Result.MAX_RANK;
        if (data.getLevel() < node.getInt("requirements.level", 1) || data.getRebirthCount() < node.getInt("requirements.rebirth", 0)) return Result.REQUIREMENTS;
        for (String prerequisite : node.getStringList("requirements.nodes")) if (data.getSkillNodes().getOrDefault(prerequisite, 0) <= 0) return Result.REQUIREMENTS;
        String exclusive = node.getString("exclusive-group", "");
        if (!exclusive.isBlank() && hasExclusive(data, treeId, nodeId, exclusive)) return Result.EXCLUSIVE;
        int cost = node.getInt("ranks." + (current + 1) + ".cost", node.getInt("cost", 1));
        if (data.getSkillPoints() < cost) return Result.NO_POINTS;
        data.setSkillPoints(data.getSkillPoints() - cost);
        data.getSkillNodes().put(nodeId, current + 1);
        stats.invalidate(player.getUniqueId());
        return Result.SUCCESS;
    }

    public Result reset(Player player) {
        PlayerData data = players.require(player);
        if (System.currentTimeMillis() < data.getPresetCooldownEnd()) return Result.COOLDOWN;
        int refund = spentPoints(data);
        data.setSkillPoints(data.getSkillPoints() + refund);
        data.getSkillNodes().clear();
        data.setPresetCooldownEnd(System.currentTimeMillis() + CHANGE_COOLDOWN_MILLIS);
        data.getSkillPresets().put(data.getActivePreset(), new LinkedHashMap<>());
        stats.invalidate(player.getUniqueId());
        return Result.SUCCESS;
    }

    public Result switchPreset(Player player, int preset) {
        if (preset < 1 || preset > 5) return Result.MISSING;
        PlayerData data = players.require(player);
        if (System.currentTimeMillis() < data.getPresetCooldownEnd()) return Result.COOLDOWN;
        data.getSkillPresets().put(data.getActivePreset(), new LinkedHashMap<>(data.getSkillNodes()));
        data.getSkillNodes().clear();
        data.getSkillNodes().putAll(data.getSkillPresets().getOrDefault(preset, Map.of()));
        data.setActivePreset(preset);
        data.setPresetCooldownEnd(System.currentTimeMillis() + CHANGE_COOLDOWN_MILLIS);
        stats.invalidate(player.getUniqueId());
        return Result.SUCCESS;
    }

    private int spentPoints(PlayerData data) {
        int total = 0;
        ConfigurationSection trees = definitions.snapshot().config("skill_trees.yml").getConfigurationSection("trees");
        if (trees == null) return 0;
        for (String tree : trees.getKeys(false)) for (var value : data.getSkillNodes().entrySet()) {
            ConfigurationSection node = trees.getConfigurationSection(tree + ".nodes." + value.getKey());
            if (node == null) continue;
            for (int rank = 1; rank <= value.getValue(); rank++) total += node.getInt("ranks." + rank + ".cost", node.getInt("cost", 1));
        }
        return total;
    }

    private boolean hasExclusive(PlayerData data, String treeId, String nodeId, String group) {
        ConfigurationSection nodes = definitions.snapshot().config("skill_trees.yml").getConfigurationSection("trees." + treeId + ".nodes");
        if (nodes == null) return false;
        return data.getSkillNodes().keySet().stream().filter(id -> !id.equals(nodeId))
                .map(nodes::getConfigurationSection).filter(Objects::nonNull).anyMatch(section -> group.equals(section.getString("exclusive-group")));
    }

    private ConfigurationSection node(String treeId, String nodeId) {
        return definitions.snapshot().config("skill_trees.yml").getConfigurationSection("trees." + treeId + ".nodes." + nodeId);
    }
    public enum Result { SUCCESS, MISSING, MAX_RANK, REQUIREMENTS, EXCLUSIVE, NO_POINTS, COOLDOWN }
}
