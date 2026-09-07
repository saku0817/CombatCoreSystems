package com.github.saku0817.combatcoresystems.command;

import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.service.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public final class CcsCommand implements CommandExecutor, TabCompleter {
    private final PlayerDataService players; private final CombatStateService combat; private final GuiService gui;
    private final PartyService parties; private final DebugService debug; private final MiniMessage mini = MiniMessage.miniMessage();

    public CcsCommand(PlayerDataService players, CombatStateService combat, GuiService gui, PartyService parties, DebugService debug) {
        this.players = players; this.combat = combat; this.gui = gui; this.parties = parties; this.debug = debug;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (combat.inCombat(player.getUniqueId())) { message(player, "<red>戦闘中は一般CCSコマンドを使用できません。</red>"); return true; }
        if (players.find(player.getUniqueId()).isEmpty()) { message(player, "<yellow>データを読み込み中です。</yellow>"); return true; }
        String permission = permissionFor(args);
        if (!permission.isBlank() && !player.hasPermission(permission) && !player.hasPermission("combatcoresystems.command.*")) {
            message(player, "<red>権限がありません。</red>"); return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(player); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "menu" -> gui.openMain(player);
            case "open" -> open(player, args);
            case "encyclopedia" -> gui.openEncyclopedia(player, "", 0, "");
            case "setting" -> setting(player, args);
            case "party" -> party(player, args);
            case "debug" -> debug(player, args);
            default -> help(player);
        }
        return true;
    }

    private void open(Player player, String[] args) {
        if (args.length < 2) { gui.openMain(player); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "stats" -> gui.openStats(player); case "equipments" -> gui.openEquipment(player); case "skilltree" -> gui.openSkillTree(player, "");
            case "rebirth" -> gui.openRebirth(player, false); case "party" -> gui.openParty(player); default -> gui.openMain(player);
        }
    }

    private void setting(Player player, String[] args) {
        PlayerData data = players.require(player); if (args.length < 2) { gui.openSettings(player); return; }
        if (args[1].equalsIgnoreCase("pvp") && args.length >= 3) data.setPvpEnabled(args[2].equalsIgnoreCase("on"));
        else if (args[1].equalsIgnoreCase("hud")) data.setHudEnabled(!data.isHudEnabled());
        else if (args[1].equalsIgnoreCase("controls")) data.setControls(args.length >= 3 ? args[2] : "DEFAULT");
        message(player, "<green>設定を更新しました。</green>");
    }

    private void party(Player player, String[] args) {
        if (args.length < 2) { gui.openParty(player); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> message(player, parties.create(player.getUniqueId()).isPresent() ? "<green>Partyを作成しました。</green>" : "<red>作成できません。</red>");
            case "invite" -> { Player target = argumentPlayer(args, 2); message(player, target != null && parties.invite(player.getUniqueId(), target.getUniqueId()) ? "<green>招待しました。</green>" : "<red>招待できません。</red>"); if (target != null) message(target, "<yellow>" + player.getName() + "からParty招待が届きました。</yellow>"); }
            case "accept" -> { Player inviter = argumentPlayer(args, 2); message(player, inviter != null && parties.accept(player.getUniqueId(), inviter.getUniqueId()) ? "<green>Partyへ参加しました。</green>" : "<red>参加できません。</red>"); }
            case "decline" -> { Player inviter = argumentPlayer(args, 2); if (inviter != null) parties.decline(player.getUniqueId(), inviter.getUniqueId()); }
            case "leave" -> message(player, parties.leave(player.getUniqueId()) ? "<green>Partyから離脱しました。</green>" : "<red>離脱できません。</red>");
            case "kick" -> { Player target = argumentPlayer(args, 2); message(player, target != null && parties.kick(player.getUniqueId(), target.getUniqueId()) ? "<green>Kickしました。</green>" : "<red>Kickできません。</red>"); }
            case "leader" -> { Player target = argumentPlayer(args, 2); message(player, target != null && parties.transfer(player.getUniqueId(), target.getUniqueId()) ? "<green>Leaderを移譲しました。</green>" : "<red>移譲できません。</red>"); }
            case "disband" -> message(player, parties.disband(player.getUniqueId()) ? "<green>Partyを解体しました。</green>" : "<red>解体できません。</red>");
            case "list" -> parties.findByPlayer(player.getUniqueId()).ifPresentOrElse(p -> message(player, "<yellow>Members: " + String.join(", ", p.getMembers().stream().map(m -> Optional.ofNullable(Bukkit.getOfflinePlayer(m.asUuid()).getName()).orElse(m.uuid())).toList()) + "</yellow>"), () -> message(player, "<red>Partyに参加していません。</red>"));
            case "chat" -> { if (args.length < 3) return; String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length)); parties.findByPlayer(player.getUniqueId()).ifPresent(p -> p.getMembers().forEach(m -> { Player target = Bukkit.getPlayer(m.asUuid()); if (target != null) message(target, "<aqua>[Party] " + player.getName() + ":</aqua> <white>" + text + "</white>"); })); }
            default -> gui.openParty(player);
        }
    }

    private void debug(Player player, String[] args) {
        if (!player.hasPermission("combatcoresystems.command.debug")) { message(player, "<red>権限がありません。</red>"); return; }
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) { message(player, "<gray>Debug: " + (debug.enabled(player.getUniqueId()) ? "ON" : "OFF") + "</gray>"); return; }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "on" -> { Duration duration = args.length >= 3 ? parseDuration(args[2]) : Duration.ofDays(36500); debug.enable(player.getUniqueId(), duration); }
            case "off", "cancel" -> debug.disable(player.getUniqueId());
            case "schedule" -> { if (args.length < 3) return; debug.schedule(player.getUniqueId(), parseDuration(args[2]), args.length >= 4 ? parseDuration(args[3]) : Duration.ofMinutes(10)); }
        }
        message(player, "<green>Debug設定を更新しました。</green>");
    }

    private Duration parseDuration(String value) {
        long number = Long.parseLong(value.substring(0, value.length() - 1));
        return switch (value.substring(value.length() - 1).toLowerCase(Locale.ROOT)) { case "s" -> Duration.ofSeconds(number); case "h" -> Duration.ofHours(number); case "d" -> Duration.ofDays(number); default -> Duration.ofMinutes(number); };
    }
    private String permissionFor(String[] args) {
        if (args.length == 0) return "combatcoresystems.command.help";
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> "combatcoresystems.command.help"; case "menu" -> "combatcoresystems.command.menu";
            case "encyclopedia" -> "combatcoresystems.command.encyclopedia"; case "party" -> "combatcoresystems.command.party";
            case "debug" -> "combatcoresystems.command.debug";
            case "open" -> "combatcoresystems.command.open." + (args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "stats");
            case "setting" -> "combatcoresystems.command.setting." + (args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "hud");
            default -> "combatcoresystems.command.help";
        };
    }
    private Player argumentPlayer(String[] args, int index) { return args.length > index ? Bukkit.getPlayerExact(args[index]) : null; }
    private void help(Player player) { message(player, "<gold>/ccs menu, open, encyclopedia, setting, party, debug</gold>"); }
    private void message(CommandSender sender, String value) { sender.sendMessage(mini.deserialize("<dark_gray>[<gold>CCS</gold>]</dark_gray> " + value)); }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return complete(args[0], List.of("help", "menu", "open", "encyclopedia", "setting", "party", "debug"));
        if (args.length == 2 && args[0].equalsIgnoreCase("open")) return complete(args[1], List.of("stats", "equipments", "skilltree", "rebirth", "party"));
        if (args.length == 2 && args[0].equalsIgnoreCase("party")) return complete(args[1], List.of("create", "invite", "accept", "decline", "leave", "kick", "leader", "disband", "list", "chat"));
        if (args.length == 2 && args[0].equalsIgnoreCase("setting")) return complete(args[1], List.of("pvp", "hud", "controls"));
        if (args.length == 3 && args[0].equalsIgnoreCase("setting") && args[1].equalsIgnoreCase("pvp")) return complete(args[2], List.of("on", "off"));
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) return complete(args[1], List.of("on", "off", "schedule", "status", "cancel"));
        if (args.length == 3 && args[0].equalsIgnoreCase("party") && Set.of("invite", "accept", "decline", "kick", "leader").contains(args[1].toLowerCase(Locale.ROOT))) return complete(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        return List.of();
    }
    private List<String> complete(String input, Collection<String> values) { String lower = input.toLowerCase(Locale.ROOT); return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower)).sorted().toList(); }
}
