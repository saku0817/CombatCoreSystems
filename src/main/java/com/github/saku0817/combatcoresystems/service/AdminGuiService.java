package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import com.github.saku0817.combatcoresystems.util.ItemText;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Menus carry server-side actions only; no command is ever run as console. */
public final class AdminGuiService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final ItemService items;
    private final EquipmentService equipment;
    private final StatService stats;
    private final LevelService levels;
    private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();
    private final Map<UUID, String> targets = new HashMap<>();
    public AdminGuiService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, ItemService items,
                           EquipmentService equipment, StatService stats, LevelService levels) {
        this.plugin = plugin; this.definitions = definitions; this.players = players; this.items = items;
        this.equipment = equipment; this.stats = stats; this.levels = levels;
    }
    private String text(String key, String fallback) { return definitions.snapshot().config("gui.yml").getString("admin." + key, fallback); }
    private boolean allowed(Player player, String permission) {
        if (player.hasPermission("combatcoresystems.admin." + permission) || player.hasPermission("combatcoresystems.admin.*")) return true;
        player.sendMessage(ItemText.parse(text("denied", "<red>権限がありません。</red>"))); return false;
    }
    public void open(Player player) {
        if (!allowed(player, "menu")) return;
        prompts.remove(player.getUniqueId());
        Menu menu = menu(player, text("title", "<dark_red>CCS 管理メニュー</dark_red>"));
        button(menu, 4, "PLAYER_HEAD", text("target", "<yellow>対象：<target></yellow>").replace("<target>", target(player)), List.of(text("target-hint", "<white>名前またはセレクターを入力（初期値 @s）</white>")),
                () -> prompt(player, text("target-prompt", "対象の名前／セレクターを入力してください。"), input -> {
                    if (input.isBlank() || input.contains(" ")) throw new IllegalArgumentException("対象は空白なしで指定してください。");
                    targets.put(player.getUniqueId(), input); open(player);
                }));
        button(menu, 10, "CHEST", text("items", "<green>独自アイテムを取り出す</green>"), List.of(), () -> catalog(player, 0));
        button(menu, 12, "ANVIL", text("item-stats", "<gold>利き手の装備ステータスを編集</gold>"), List.of(text("item-stats-hint", "<white>メイン・サブステを個体単位で編集。YAML定義は変更しません。</white>")), () -> editHeld(player));
        button(menu, 14, "COMMAND_BLOCK", text("commands", "<aqua>管理コマンド操作</aqua>"), List.of(), () -> commands(player));
        button(menu, 16, "BARRIER", text("close", "<yellow>閉じる</yellow>"), List.of(), player::closeInventory);
        player.openInventory(menu.inventory);
    }
    private String target(Player player) { return targets.getOrDefault(player.getUniqueId(), "@s"); }
    private void catalog(Player player, int page) {
        if (!allowed(player, "give")) return;
        Set<String> ids = new TreeSet<>(); ids.addAll(definitions.snapshot().weapons().keySet()); ids.addAll(definitions.snapshot().equipment().keySet());
        for (String[] root : new String[][]{{"divine_hearts.yml", "divine-hearts"}, {"levels.yml", "materials"}}) {
            var section = definitions.snapshot().config(root[0]).getConfigurationSection(root[1]); if (section != null) ids.addAll(section.getKeys(false));
        }
        List<String> list = new ArrayList<>(ids); Menu menu = menu(player, text("items", "<green>独自アイテムを取り出す</green>"));
        for (int index = page * 45; index < Math.min(list.size(), (page + 1) * 45); index++) {
            String id = list.get(index); ItemStack icon = items.create(id, 1).orElse(null); if (icon == null) continue;
            menu.inventory.setItem(index % 45, icon); menu.actions.put(index % 45, () -> command(player, "give item " + target(player) + " " + id + " 1"));
        }
        if (page > 0) button(menu, 45, "ARROW", text("previous", "<yellow>前のページ</yellow>"), List.of(), () -> catalog(player, page - 1));
        if ((page + 1) * 45 < list.size()) button(menu, 52, "ARROW", text("next", "<yellow>次のページ</yellow>"), List.of(), () -> catalog(player, page + 1));
        back(menu, player); player.openInventory(menu.inventory);
    }
    private void commands(Player player) {
        Menu menu = menu(player, text("commands", "<aqua>管理コマンド操作</aqua>"));
        // Prefix and arguments are separated so input cannot execute another root command.
        String[][] commands = {
            {"レベル変更", "edit level", "<対象> set|add|remove <数値>"}, {"経験値変更", "edit exp", "<対象> set|add|remove <数値>"},
            {"バフ・デバフ", "edit buff", "add|remove <対象> <buffs.ymlのID>"}, {"属性付着", "edit attribute", "add|remove <対象> FIRE|WATER|WIND|THUNDER|MOON"},
            {"戦闘状態", "edit force", "on|off [対象]"}, {"利き手アイテムLv", "itemlevel", "<レベル> [対象]"},
            {"アイテム付与", "give item", "<対象> <ID> <個数>"}, {"モブ召喚", "spawn mob", "<x> <y> <z> <ID>（現在地は ~ ~ ~）"},
            {"ボス召喚", "spawn boss", "<x> <y> <z> <ID>"}, {"領域選択ツール", "region wand", ""},
            {"領域の始点", "region pos1", ""}, {"領域の終点", "region pos2", ""}, {"領域作成", "region create", "<ID> [--full-height]"},
            {"領域削除", "region delete", "<ID>"}, {"領域情報", "region info", "<ID>"}, {"領域一覧", "region list", ""},
            {"領域フラグ", "region flag", "<ID> pvp allow|deny"}, {"バックアップ作成", "backup create", ""},
            {"バックアップ一覧", "backup list", ""}, {"バックアップ復元", "backup restore", "<バックアップID>"},
            {"図鑑解放", "encyclopedia unlock", "<対象> mob|boss <ID> または <対象> all"}, {"図鑑リセット", "encyclopedia reset", "<対象> mob|boss <ID> または <対象> all"},
            {"設定再読込", "reload", ""}, {"データ保存", "save", ""}, {"診断開始", "debug on", "[10m]"},
            {"診断停止", "debug off", ""}, {"診断状況", "debug status", ""}, {"診断予約", "debug schedule", "<待ち時間> [継続時間]（例：30s 10m）"},
            {"診断予約取消", "debug cancel", ""}
        };
        int slot = 0;
        for (String[] entry : commands) {
            String label = text("command-names." + entry[1].replace(' ', '-'), entry[0]);
            button(menu, slot++, "PAPER", "<white>" + label + "</white>", List.of("<gray>/ccsadmin " + entry[1] + "</gray>"), () -> {
                if (entry[2].isEmpty()) command(player, entry[1]);
                else prompt(player, entry[2] + " / " + text("input-cancel", "cancelで中止。対象に@s等を使用できます。"), input -> command(player, entry[1] + " " + input));
            });
        }
        back(menu, player); player.openInventory(menu.inventory);
    }
    private void command(Player player, String args) {
        confirm(player, "/ccsadmin " + args, () -> {
            player.closeInventory(); Bukkit.dispatchCommand(player, "ccsadmin " + args);
        });
    }
    private Player oneTarget(Player player) {
        String input = target(player);
        List<Player> matches = input.startsWith("@") ? Bukkit.selectEntities(player, input).stream().filter(Player.class::isInstance).map(Player.class::cast).toList()
                : Optional.ofNullable(Bukkit.getPlayerExact(input)).stream().toList();
        if (matches.size() != 1) throw new IllegalArgumentException("個体編集はオンラインプレイヤー1人を指定してください。");
        return matches.getFirst();
    }
    private void editHeld(Player player) {
        if (!allowed(player, "itemstats")) return;
        Player target = oneTarget(player); ItemStack stack = target.getInventory().getItemInMainHand();
        ItemInstance instance = items.instance(stack).orElseThrow(() -> new IllegalArgumentException("対象の利き手にCCS装備を持ってください。"));
        EquipmentDefinition definition = definitions.snapshot().equipment().get(instance.getDefinitionId());
        if (definition == null) throw new IllegalArgumentException("武器ではなく防具または残響を指定してください。");
        Menu menu = menu(player, text("edit-title", "<gold>装備個体の編集</gold>")); menu.inventory.setItem(4, stack.clone());
        button(menu, 10, "NETHER_STAR", text("main-stat", "<yellow>メインステを変更</yellow>"), List.of(), () -> chooseStat(player, definition, true, key -> {
            prompt(player, text("main-values", "Lv.1の値 最大Lvの値 を空白区切りで入力（例 0.05 0.60）"), input -> {
                String[] values = input.split("\\s+"); if (values.length != 2) throw new IllegalArgumentException("値を2つ指定してください。");
                double first = finite(values[0]), last = finite(values[1]);
                confirm(player, items.displayName(key.name()) + " " + first + " → " + last,
                        () -> updateHeld(player, target, instance.getInstanceId(), value -> value.setMainStat(key, first, last)));
            });
        }));
        button(menu, 12, "EMERALD", text("add-substat", "<green>サブステを追加</green>"), List.of(), () -> chooseStat(player, definition, false, key -> {
            prompt(player, text("sub-value", "値を入力（5%なら0.05）。既存のサブステとの重複はできません。"), input -> {
                double value = finite(input);
                confirm(player, items.displayName(key.name()) + " " + value, () -> updateHeld(player, target, instance.getInstanceId(), current -> {
                    if (current.getSubstats().containsKey(key.name()) || current.getSubstats().size() >= 4) throw new IllegalArgumentException("重複または4枠上限です。");
                    current.getSubstats().put(key.name(), value); current.getSubstatUpgrades().put(key.name(), 0);
                }));
            });
        }));
        button(menu, 14, "EXPERIENCE_BOTTLE", text("unlocked", "<aqua>開放済みサブステ数</aqua>"), List.of(), () -> prompt(player, "0～4を入力してください。", input -> {
            int count = Integer.parseInt(input); if (count < 0 || count > 4) throw new IllegalArgumentException("0～4で指定してください。");
            confirm(player, "開放数 " + count, () -> updateHeld(player, target, instance.getInstanceId(), value -> {
                if (count > value.getSubstats().size()) throw new IllegalArgumentException("サブステ数を超えています。"); value.setUnlockedSubstats(count);
            }));
        }));
        int slot = 28;
        for (var entry : instance.getSubstats().entrySet()) {
            String key = entry.getKey();
            button(menu, slot++, "PAPER", items.displayName(key) + " " + items.statValue(key, entry.getValue()), List.of(text("sub-edit-hint", "<white>値 強化回数 を入力。deleteでこのサブステを削除。</white>")), () -> prompt(player, "値 強化回数（例 0.10 2）、またはdelete", input -> {
                if (input.equalsIgnoreCase("delete")) confirm(player, "削除：" + items.displayName(key), () -> updateHeld(player, target, instance.getInstanceId(), value -> {
                    int position = new ArrayList<>(value.getSubstats().keySet()).indexOf(key);
                    value.getSubstats().remove(key); value.getSubstatUpgrades().remove(key);
                    if (position >= 0 && position < value.getUnlockedSubstats()) value.setUnlockedSubstats(value.getUnlockedSubstats() - 1);
                }));
                else {
                    String[] values = input.split("\\s+"); if (values.length != 2) throw new IllegalArgumentException("値と強化回数を指定してください。");
                    double amount = finite(values[0]); int upgrades = Integer.parseInt(values[1]);
                    if (upgrades < 0 || upgrades > 100) throw new IllegalArgumentException("強化回数は0～100です。");
                    confirm(player, items.displayName(key) + " " + amount + " [+" + upgrades + "]", () -> updateHeld(player, target, instance.getInstanceId(), value -> {
                        if (!value.getSubstats().containsKey(key)) throw new IllegalArgumentException("サブステが変更されています。");
                        value.getSubstats().put(key, amount); value.getSubstatUpgrades().put(key, upgrades);
                    }));
                }
            }));
        }
        back(menu, player); player.openInventory(menu.inventory);
    }
    private void chooseStat(Player player, EquipmentDefinition definition, boolean main, Consumer<StatKey> action) {
        Menu menu = menu(player, text("choose-stat", "<gold>ステータスを選択</gold>")); int slot = 0;
        for (StatKey key : StatKey.values()) {
            if (main && !DefinitionRegistry.mainAllowed(definition.slot(), key)) continue;
            button(menu, slot++, "PAPER", "<white>" + items.displayName(key.name()), List.of(), () -> action.accept(key));
        }
        back(menu, player); player.openInventory(menu.inventory);
    }
    private double finite(String raw) {
        double value = Double.parseDouble(raw);
        if (!Double.isFinite(value) || Math.abs(value) > 1e9) throw new IllegalArgumentException("有限の数値（絶対値10億以下）を指定してください。");
        return value;
    }
    private void updateHeld(Player actor, Player target, String id, Consumer<ItemInstance> change) {
        if (!allowed(actor, "itemstats")) return;
        if (!target.isOnline() || players.find(target.getUniqueId()).isEmpty()) throw new IllegalArgumentException("対象がオフラインまたは読込中です。");
        ItemStack stack = target.getInventory().getItemInMainHand().clone();
        ItemInstance current = items.instance(stack).orElseThrow(() -> new IllegalArgumentException("対象アイテムが移動しました。"));
        if (!id.equals(current.getInstanceId())) throw new IllegalArgumentException("対象アイテムが変更されています。再選択してください。");
        change.accept(current); items.writeInstance(stack, current); target.getInventory().setItemInMainHand(stack);
        equipment.syncArmor(target); stats.invalidate(target.getUniqueId()); levels.apply(target, players.require(target), false);
        actor.sendMessage(ItemText.parse(text("updated", "<green>装備個体を更新しました。</green>"))); open(actor);
    }
    private void prompt(Player player, String message, Consumer<String> action) {
        player.closeInventory(); prompts.put(player.getUniqueId(), action);
        player.sendMessage(net.kyori.adventure.text.Component.text(message + " / cancelで中止"));
    }
    private void confirm(Player player, String description, Runnable action) {
        Menu menu = menu(player, text("confirm-title", "<red>実行内容を確認</red>"));
        button(menu, 13, "PAPER", text("confirm-summary", "<white>実行する操作</white>"), List.of(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().escapeTags(description)), () -> {});
        button(menu, 29, "LIME_CONCRETE", text("confirm", "<green>確定して実行</green>"), List.of(), action);
        button(menu, 33, "RED_CONCRETE", text("cancel", "<yellow>中止</yellow>"), List.of(), () -> open(player)); player.openInventory(menu.inventory);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onChat(AsyncChatEvent event) {
        Consumer<String> action = prompts.remove(event.getPlayer().getUniqueId()); if (action == null) return;
        event.setCancelled(true); String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> safe(event.getPlayer(), () -> {
            if (!event.getPlayer().isOnline()) return;
            if (input.equalsIgnoreCase("cancel")) { open(event.getPlayer()); return; }
            if (input.length() > 256 || input.contains("\n") || input.contains("\r")) throw new IllegalArgumentException("入力が長すぎるか、改行を含みます。");
            action.accept(input);
        }));
    }
    private void safe(Player player, Runnable action) {
        if (!allowed(player, "menu")) return;
        try { action.run(); } catch (IllegalArgumentException ex) { player.sendMessage(net.kyori.adventure.text.Component.text("操作できません：" + ex.getMessage())); open(player); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        if (event.getRawSlot() >= menu.inventory.getSize()) {
            if (event.isShiftClick() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        if (!event.getCursor().getType().isAir()) return;
        Runnable action = menu.actions.get(event.getRawSlot()); if (action == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() != menu || !menu.owner.equals(player.getUniqueId())) return;
            menu.actions.clear(); safe(player, action); // consume confirmation once, including double clicks
        });
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu && event.getRawSlots().stream().anyMatch(slot -> slot < menu.inventory.getSize())) event.setCancelled(true);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { prompts.remove(event.getPlayer().getUniqueId()); targets.remove(event.getPlayer().getUniqueId()); }
    private Menu menu(Player player, String title) { Menu menu = new Menu(player.getUniqueId()); menu.inventory = Bukkit.createInventory(menu, 54, ItemText.parse(title)); return menu; }
    private void button(Menu menu, int slot, String material, String name, List<String> lore, Runnable action) {
        ItemStack stack = new ItemStack(Material.valueOf(material)); var meta = stack.getItemMeta(); meta.itemName(ItemText.parse(name)); meta.lore(lore.stream().map(ItemText::parse).toList());
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta skull) {
            Player owner = Bukkit.getPlayer(menu.owner); if (owner != null) skull.setPlayerProfile(owner.getPlayerProfile());
            meta.itemName(ItemText.parse(name)); meta.displayName(ItemText.parse(name));
        }
        stack.setItemMeta(meta); menu.inventory.setItem(slot, stack); menu.actions.put(slot, action);
    }
    private void back(Menu menu, Player player) { button(menu, 53, "ARROW", text("back", "<yellow>戻る</yellow>"), List.of(), () -> open(player)); }
    private static final class Menu implements InventoryHolder {
        final UUID owner; final Map<Integer, Runnable> actions = new HashMap<>(); Inventory inventory;
        Menu(UUID owner) { this.owner = owner; } @Override public Inventory getInventory() { return inventory; }
    }
}
