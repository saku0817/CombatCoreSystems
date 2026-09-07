package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.Element;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.model.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;

public final class HudService {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final LevelService levels;
    private final CombatStateService combat;
    private final ElementService elements;
    private final SkillService skills;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Map<UUID, Board> boards = new HashMap<>();

    public HudService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                      LevelService levels, CombatStateService combat, ElementService elements, SkillService skills) {
        this.plugin = plugin; this.definitions = definitions; this.players = players; this.stats = stats; this.levels = levels;
        this.combat = combat; this.elements = elements; this.skills = skills;
    }

    public void start() {
        long ticks = Math.max(1, definitions.snapshot().config("config.yml").getLong("hud.update-ticks", 5));
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, ticks, ticks);
    }

    public void remove(Player player) {
        Board board = boards.remove(player.getUniqueId());
        if (board != null && player.getScoreboard() == board.scoreboard) player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) players.find(player.getUniqueId()).ifPresent(data -> update(player, data));
    }

    private void update(Player player, PlayerData data) {
        if (!data.isHudEnabled()) { remove(player); return; }
        if (definitions.snapshot().config("config.yml").getBoolean("hud.sidebar-enabled", true)) updateSidebar(player, data);
        if (definitions.snapshot().config("config.yml").getBoolean("hud.actionbar-enabled", true)) updateActionBar(player);
    }

    private void updateSidebar(Player player, PlayerData data) {
        PlayerStats value = stats.get(player, data);
        List<String> templates = definitions.snapshot().config("gui.yml").getStringList("hud.sidebar-lines");
        List<Component> rendered = templates.stream().map(line -> mini.deserialize(replace(line, player, data, value))).toList();
        Component title = mini.deserialize(replace(definitions.snapshot().config("gui.yml").getString("hud.sidebar-title", "<gold>CCS</gold>"), player, data, value));
        Board board = boards.computeIfAbsent(player.getUniqueId(), ignored -> createBoard(player, title, rendered.size()));
        if (!board.title.equals(title)) { board.objective.displayName(title); board.title = title; }
        for (int i = 0; i < rendered.size(); i++) if (!rendered.get(i).equals(board.lines.get(i))) {
            board.teams.get(i).prefix(rendered.get(i)); board.lines.set(i, rendered.get(i));
        }
    }

    private Board createBoard(Player player, Component title, int count) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("ccs", Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        List<Team> teams = new ArrayList<>(); List<Component> lines = new ArrayList<>();
        ChatColor[] colors = ChatColor.values();
        for (int i = 0; i < count; i++) {
            Team team = scoreboard.registerNewTeam("line" + i);
            String entry = colors[i % colors.length].toString() + ChatColor.RESET;
            team.addEntry(entry); objective.getScore(entry).setScore(count - i); teams.add(team); lines.add(Component.empty());
        }
        player.setScoreboard(scoreboard);
        return new Board(scoreboard, objective, teams, lines, title);
    }

    private void updateActionBar(Player player) {
        long combatRemaining = combat.remainingMillis(player.getUniqueId());
        Map<Element, Long> attached = elements.remaining(player.getUniqueId());
        if (combatRemaining <= 0 && attached.isEmpty()) return;
        StringBuilder text = new StringBuilder();
        if (combatRemaining > 0) {
            SkillService.Status skill = skills.status(player.getUniqueId(), false), ultimate = skills.status(player.getUniqueId(), true);
            String remaining = combat.isForced(player.getUniqueId()) ? "∞" : format(combatRemaining / 1000.0) + "秒";
            String template = definitions.snapshot().config("gui.yml").getString("hud.combat-actionbar", "<red>⚔ 戦闘中 <combat_remaining>秒</red> <gray>|</gray> <yellow>スキル: <skill_status></yellow> <gray>|</gray> <gold>必殺技: <ultimate_status></gold>");
            text.append(template.replace("<combat_remaining>", remaining)
                    .replace("<skill_status>", skill.ready() ? "発動可能" : "あと" + format(skill.remainingSeconds()) + "秒")
                    .replace("<ultimate_status>", ultimate.ready() ? "発動可能" : "あと" + format(ultimate.remainingSeconds()) + "秒"));
        }
        if (!attached.isEmpty()) {
            if (!text.isEmpty()) text.append(" <gray>|</gray> ");
            String attributes = attached.entrySet().stream().map(entry -> entry.getKey().japaneseName() + " " + format(entry.getValue() / 1000.0) + "秒").collect(java.util.stream.Collectors.joining(" "));
            String template = definitions.snapshot().config("gui.yml").getString("hud.attribute-actionbar", "<attributes>");
            text.append(template.replace("<attributes>", attributes));
        }
        player.sendActionBar(mini.deserialize(text.toString()));
    }

    private String replace(String input, Player player, PlayerData data, PlayerStats value) {
        return input.replace("<server>", definitions.snapshot().config("config.yml").getString("server-name", "CCS"))
                .replace("<player>", player.getName()).replace("<level>", Integer.toString(data.getLevel()))
                .replace("<exp_remaining>", Long.toString(data.getLevel() >= 100 ? 0 : levels.requiredExp(data.getLevel()) - data.getExp()))
                .replace("<hp>", Long.toString(Math.round(player.getHealth()))).replace("<max_hp>", Long.toString(Math.round(value.maxHp())));
    }
    private String format(double value) { return String.format(Locale.ROOT, "%.1f", value); }

    private static final class Board {
        final Scoreboard scoreboard; final Objective objective; final List<Team> teams; final List<Component> lines; Component title;
        Board(Scoreboard scoreboard, Objective objective, List<Team> teams, List<Component> lines, Component title) {
            this.scoreboard = scoreboard; this.objective = objective; this.teams = teams; this.lines = lines; this.title = title;
        }
    }
}
