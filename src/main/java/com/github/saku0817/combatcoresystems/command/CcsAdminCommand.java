package com.github.saku0817.combatcoresystems.command;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.Element;
import com.github.saku0817.combatcoresystems.model.PlayerData;
import com.github.saku0817.combatcoresystems.service.*;
import com.github.saku0817.combatcoresystems.storage.StorageService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;

public final class CcsAdminCommand implements CommandExecutor, TabCompleter {
    private final DebugService debug;
    private final JavaPlugin plugin; private final DefinitionRegistry definitions; private final StorageService storage;
    private final PlayerDataService players; private final LevelService levels; private final StatService stats;
    private final BuffService buffs; private final ElementService elements; private final ItemService items; private final MobService mobs;
    private final RegionService regions; private final BackupService backups; private final EncyclopediaService encyclopedia;
    private final PartyService parties; private final CombatStateService combat; private final MiniMessage mini = MiniMessage.miniMessage();

    public CcsAdminCommand(JavaPlugin plugin, DefinitionRegistry definitions, StorageService storage, PlayerDataService players,
                           LevelService levels, StatService stats, BuffService buffs, ElementService elements, ItemService items,
                           MobService mobs, RegionService regions, BackupService backups, EncyclopediaService encyclopedia,
                           PartyService parties, CombatStateService combat, DebugService debug) {
        this.debug = debug;
        this.plugin = plugin; this.definitions = definitions; this.storage = storage; this.players = players; this.levels = levels;
        this.stats = stats; this.buffs = buffs; this.elements = elements; this.items = items; this.mobs = mobs; this.regions = regions;
        this.backups = backups; this.encyclopedia = encyclopedia; this.parties = parties; this.combat = combat;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { help(sender); return true; }
        String root = args[0].toLowerCase(Locale.ROOT);
        String permission = switch (root) { case "edit" -> args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "*"; case "spawn" -> "spawn"; default -> root; };
        if (!sender.hasPermission("combatcoresystems.admin." + permission) && !sender.hasPermission("combatcoresystems.admin.*")) { message(sender, "<red>権限がありません。</red>"); return true; }
        try {
            switch (root) {
                case "debug" -> debug(sender, args);
                case "reload" -> message(sender, definitions.reloadSafely() ? "<green>設定を再読み込みしました。</green>" : "<red>検証に失敗したため現在の設定を維持しました。</red>");
                case "save" -> { parties.save(); players.saveAll().whenComplete((ok, error) -> main(() -> message(sender, error == null ? "<green>保存しました。</green>" : "<red>保存に失敗しました。</red>"))); }
                case "backup" -> backup(sender, args);
                case "give" -> give(sender, args);
                case "spawn" -> spawn(sender, args);
                case "region" -> region(sender, args);
                case "edit" -> edit(sender, args);
                case "encyclopedia" -> encyclopedia(sender, args);
                default -> help(sender);
            }
        } catch (IllegalArgumentException ex) { message(sender, "<red>引数が不正です: " + ex.getMessage() + "</red>"); }
        return true;
    }

    private void backup(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) { message(sender, "<gray>Backups: " + String.join(", ", backups.list()) + "</gray>"); return; }
        if (args[1].equalsIgnoreCase("create")) backups.create().whenComplete((id, error) -> main(() -> message(sender, error == null ? "<green>バックアップを作成しました: " + id + "</green>" : "<red>バックアップに失敗しました。</red>")));
        else if (args[1].equalsIgnoreCase("restore") && args.length >= 3) backups.restore(args[2]).whenComplete((ok, error) -> main(() -> message(sender, error == null ? "<green>復元しました。</green>" : "<red>復元に失敗しました: " + error.getMessage() + "</red>")));
    }

    private void give(CommandSender sender, String[] args) {
        if (args.length < 5 || !args[1].equalsIgnoreCase("item")) throw new IllegalArgumentException("/ccsadmin give item <player> <id> <amount>");
        Player player = Bukkit.getPlayerExact(args[2]); if (player == null) throw new IllegalArgumentException("player");
        int amount = Math.max(1, Integer.parseInt(args[4]));
        for (int i = 0; i < amount; i++) {
            ItemStack item = items.create(args[3], 1).orElseThrow(() -> new IllegalArgumentException("item id"));
            player.getInventory().addItem(item).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        message(sender, "<green>アイテムを付与しました。</green>");
    }

    private void spawn(CommandSender sender, String[] args) {
        if (args.length < 6 || !(sender instanceof Player player)) throw new IllegalArgumentException("/ccsadmin spawn mob|boss <x> <y> <z> <id>");
        boolean boss = args[1].equalsIgnoreCase("boss"); Location base = player.getLocation();
        Location location = new Location(player.getWorld(), coordinate(args[2], base.getX()), coordinate(args[3], base.getY()), coordinate(args[4], base.getZ()));
        message(sender, mobs.spawn(args[5], boss, location, false).isPresent() ? "<green>Spawnしました。</green>" : "<red>定義が見つかりません。</red>");
    }

    private double coordinate(String raw, double origin) { return raw.startsWith("~") ? origin + (raw.length() == 1 ? 0 : Double.parseDouble(raw.substring(1))) : Double.parseDouble(raw); }

    private void region(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 2) throw new IllegalArgumentException("region command");
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "wand" -> player.getInventory().addItem(regions.wand());
            case "pos1" -> regions.select(player, true, player.getLocation()); case "pos2" -> regions.select(player, false, player.getLocation());
            case "create" -> { if (args.length < 3) throw new IllegalArgumentException("region id"); message(sender, regions.create(player, args[2], Arrays.asList(args).contains("--full-height")) ? "<green>Regionを作成しました。</green>" : "<red>作成できません。</red>"); }
            case "delete" -> message(sender, args.length >= 3 && regions.delete(args[2]) ? "<green>削除しました。</green>" : "<red>削除できません。</red>");
            case "info" -> { if (args.length < 3) throw new IllegalArgumentException("region id"); var region = definitions.snapshot().regions().get(args[2]); message(sender, region == null ? "<red>見つかりません。</red>" : "<gray>" + region + "</gray>"); }
            case "list" -> message(sender, "<gray>Regions: " + String.join(", ", definitions.snapshot().regions().keySet()) + "</gray>");
            case "flag" -> { if (args.length < 5) throw new IllegalArgumentException("region flag <id> pvp allow|deny"); message(sender, regions.setFlag(args[2], args[3], args[4]) ? "<green>Flagを更新しました。</green>" : "<red>更新できません。</red>"); }
        }
    }

    private void edit(CommandSender sender, String[] args) {
        if (args.length < 3) throw new IllegalArgumentException("edit command");
        String type = args[1].toLowerCase(Locale.ROOT);
        if ((type.equals("level") || type.equals("exp")) && args.length >= 5) {
            editPlayerData(args[2], data -> {
                long current = type.equals("level") ? data.getLevel() : data.getExp(); long input = Long.parseLong(args[4]);
                long value = switch (args[3].toLowerCase(Locale.ROOT)) { case "set" -> input; case "add" -> current + input; case "remove" -> current - input; default -> throw new IllegalArgumentException("set/add/remove"); };
                if (type.equals("level")) data.setLevel((int) value); else data.setExp(value);
            }, sender); return;
        }
        if (type.equals("buff") && args.length >= 5) { Player target = Bukkit.getPlayerExact(args[3]); if (target == null) throw new IllegalArgumentException("online player"); boolean ok = args[2].equalsIgnoreCase("add") ? buffs.apply(target, args[4], null) : buffs.remove(target, args[4]); message(sender, ok ? "<green>更新しました。</green>" : "<red>更新できません。</red>"); return; }
        if ((type.equals("attribute") || type.equals("element")) && args.length >= 5) { Player target = Bukkit.getPlayerExact(args[3]); Element element = Element.parse(args[4]).orElseThrow(() -> new IllegalArgumentException("attribute")); if (target == null) throw new IllegalArgumentException("online player"); boolean ok; if (args[2].equalsIgnoreCase("add")) { elements.attach(target, element, 5); ok = true; } else ok = elements.remove(target.getUniqueId(), element); message(sender, ok ? "<green>更新しました。</green>" : "<red>更新できません。</red>"); return; }
        if (type.equals("force")) { forceCombat(sender, args); return; }
    }

    private void forceCombat(CommandSender sender, String[] args) {
        if (args.length < 3) throw new IllegalArgumentException("/ccsadmin edit force on|off [username]");
        boolean enable = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "on" -> true;
            case "off" -> false;
            default -> throw new IllegalArgumentException("on/off");
        };
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);
            if (target == null) throw new IllegalArgumentException("online player");
        } else if (sender instanceof Player player) target = player;
        else throw new IllegalArgumentException("コンソールから実行する場合はusernameを指定してください");

        if (enable) combat.forceOn(target); else combat.forceOff(target.getUniqueId());
        message(sender, configuredMessage("command.force", "<green><player> の戦闘状態を <state> にしました。</green>",
                Map.of("<player>", target.getName(), "<state>", enable ? "ON" : "OFF")));
    }

    private void encyclopedia(CommandSender sender, String[] args) {
        if (args.length < 4) throw new IllegalArgumentException("encyclopedia unlock|reset <player> mob|boss <id>|all");
        boolean unlock = args[1].equalsIgnoreCase("unlock");
        editPlayerData(args[2], data -> {
            if (args[3].equalsIgnoreCase("all")) encyclopedia.setAll(data, unlock);
            else { if (args.length < 5) throw new IllegalArgumentException("id"); encyclopedia.set(data, args[3].equalsIgnoreCase("boss"), args[4], unlock); }
        }, sender);
    }

    private void editPlayerData(String name, Consumer<PlayerData> action, CommandSender sender) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null && players.find(online.getUniqueId()).isPresent()) {
            PlayerData data = players.require(online); action.accept(data); stats.invalidate(online.getUniqueId()); levels.apply(online, data, false); message(sender, "<green>更新しました。</green>"); return;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        storage.loadPlayer(offline.getUniqueId(), Optional.ofNullable(offline.getName()).orElse(name)).thenCompose(data -> { action.accept(data); return storage.savePlayers(List.of(storage.snapshot(data))); })
                .whenComplete((ok, error) -> main(() -> message(sender, error == null ? "<green>Offline Playerを更新しました。</green>" : "<red>更新に失敗しました。</red>")));
    }

    private void debug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) throw new IllegalArgumentException("ゲーム内から実行してください。");
        String action = args.length < 2 ? "status" : args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on" -> debug.enable(player.getUniqueId(), args.length > 2 ? duration(args[2]) : java.time.Duration.ofMinutes(10));
            case "off", "cancel" -> debug.disable(player.getUniqueId());
            case "schedule" -> {
                if (args.length < 3) throw new IllegalArgumentException("/ccsadmin debug schedule <delay:30s|5m|1h> [duration:10m]");
                debug.schedule(player.getUniqueId(), duration(args[2]), args.length > 3 ? duration(args[3]) : java.time.Duration.ofMinutes(10));
            }
            case "status" -> { }
            default -> throw new IllegalArgumentException("/ccsadmin debug on [10m] | off | status | schedule <delay> [duration] | cancel");
        }
        message(sender, "<gray>Debug: " + (debug.enabled(player.getUniqueId()) ? "ON" : "OFF") + " / 予約: " + debug.scheduled(player.getUniqueId()).isPresent() + " / 保存先: plugins/CombatCoreSystems/logs/debug/</gray>");
    }

    private java.time.Duration duration(String input) {
        if (!input.matches("[1-9][0-9]*[smhd]")) throw new IllegalArgumentException("時間は30s・5m・1h・1dのように指定してください。");
        long amount = Long.parseLong(input.substring(0, input.length() - 1));
        try {
            return java.time.Duration.ofSeconds(Math.multiplyExact(amount, switch (input.charAt(input.length() - 1)) { case 's' -> 1L; case 'm' -> 60L; case 'h' -> 3600L; default -> 86400L; }));
        } catch (ArithmeticException ex) { throw new IllegalArgumentException("時間が大きすぎます。"); }
    }

    private void main(Runnable action) { Bukkit.getScheduler().runTask(plugin, action); }
    private String configuredMessage(String path, String fallback, Map<String, String> replacements) {
        String value = definitions.snapshot().config("messages.yml").getString(path, fallback);
        for (Map.Entry<String, String> replacement : replacements.entrySet()) value = value.replace(replacement.getKey(), replacement.getValue());
        return value;
    }
    private void help(CommandSender sender) {
        message(sender, "<gray>/ccsadmin debug on [10m] | off | status | schedule <delay> [duration] | cancel</gray>");
        message(sender, "<gold>/ccsadmin edit|give|spawn|region|backup|encyclopedia|reload|save</gold>");
        message(sender, "<gray>/ccsadmin edit attribute add|remove <player> <attribute></gray>");
        message(sender, "<gray>/ccsadmin edit force on|off [username]</gray>");
    }
    private void message(CommandSender sender, String text) { sender.sendMessage(mini.deserialize("<dark_gray>[<gold>CCS</gold>]</dark_gray> " + text)); }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return complete(args[0], List.of("edit", "give", "spawn", "region", "backup", "encyclopedia", "reload", "save", "debug"));
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) return complete(args[1], List.of("on", "off", "status", "schedule", "cancel"));
        if (args.length >= 3 && args[0].equalsIgnoreCase("debug")) return complete(args[args.length - 1], List.of("30s", "5m", "10m", "1h"));
        if (args.length == 2 && args[0].equalsIgnoreCase("edit")) return complete(args[1], List.of("level", "exp", "buff", "attribute", "force"));
        if (args.length == 2 && args[0].equalsIgnoreCase("backup")) return complete(args[1], List.of("create", "restore", "list"));
        if (args.length == 2 && args[0].equalsIgnoreCase("region")) return complete(args[1], List.of("wand", "pos1", "pos2", "create", "delete", "info", "list", "flag"));
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return complete(args[1], List.of("mob", "boss"));
        if (args.length == 3 && args[0].equalsIgnoreCase("edit") && args[1].equalsIgnoreCase("force")) return complete(args[2], List.of("on", "off"));
        if (args.length == 4 && args[0].equalsIgnoreCase("edit") && args[1].equalsIgnoreCase("force")) return complete(args[3], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("edit") && (args[1].equalsIgnoreCase("level") || args[1].equalsIgnoreCase("exp"))) return complete(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("edit") && (args[1].equalsIgnoreCase("buff") || args[1].equalsIgnoreCase("attribute"))) return complete(args[2], List.of("add", "remove"));
        if (args.length == 4 && args[0].equalsIgnoreCase("edit") && (args[1].equalsIgnoreCase("buff") || args[1].equalsIgnoreCase("attribute"))) return complete(args[3], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        if (args.length == 3 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("encyclopedia"))) return complete(args[2], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) return complete(args[3], itemIds());
        if (args.length == 6 && (args[1].equalsIgnoreCase("mob") || args[1].equalsIgnoreCase("boss"))) return complete(args[5], args[1].equalsIgnoreCase("boss") ? definitions.snapshot().bosses().keySet() : definitions.snapshot().mobs().keySet());
        return List.of();
    }
    private Collection<String> itemIds() { Set<String> ids = new LinkedHashSet<>(); ids.addAll(definitions.snapshot().weapons().keySet()); ids.addAll(definitions.snapshot().equipment().keySet()); var materials = definitions.snapshot().config("levels.yml").getConfigurationSection("materials"); if (materials != null) ids.addAll(materials.getKeys(false)); return ids; }
    private List<String> complete(String input, Collection<String> values) { String lower = input.toLowerCase(Locale.ROOT); return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower)).sorted().toList(); }
}
