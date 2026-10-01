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
        button(menu, 10, "CHEST", text("items", "<green>独自アイテムを取り出す</green>"), List.of(), () -> catalogCategories(player));
        button(menu, 12, "ANVIL", text("item-stats", "<gold>利き手の装備ステータスを編集</gold>"), List.of(text("item-stats-hint", "<white>メイン・サブステを個体単位で編集。YAML定義は変更しません。</white>")), () -> editHeld(player));
        button(menu, 14, "COMMAND_BLOCK", text("commands", "<aqua>管理コマンド操作</aqua>"), List.of(), () -> commands(player));
        button(menu, 16, "BARRIER", text("close", "<yellow>閉じる</yellow>"), List.of(), player::closeInventory);
        player.openInventory(menu.inventory);
    }
    private String target(Player player) { return targets.getOrDefault(player.getUniqueId(), "@s"); }
    private void catalogCategories(Player player) {
        if (!allowed(player, "give")) return;
        Menu menu=menu(player,text("items","<green>独自アイテムを取り出す</green>"));
        button(menu,10,"IRON_SWORD",text("categories.weapons","<white>武器</white>"),List.of(),()->catalog(player,"weapons","",0));
        button(menu,12,"IRON_CHESTPLATE",text("categories.equipment","<white>装備（セット別）</white>"),List.of(),()->equipmentSets(player,0));
        button(menu,14,"NETHER_STAR",text("categories.memories","<white>追憶</white>"),List.of(),()->catalog(player,"memories","",0));
        button(menu,16,"EXPERIENCE_BOTTLE",text("categories.materials","<white>強化素材</white>"),List.of(),()->catalog(player,"materials","",0));
        back(menu,player); player.openInventory(menu.inventory);
    }
    private void equipmentSets(Player player,int page) {
        if (!allowed(player,"give")) return;
        List<String> sets=definitions.snapshot().equipment().values().stream().map(d->Objects.requireNonNullElse(d.setId(),"")).distinct().sorted().toList();
        Menu menu=menu(player,text("categories.equipment","<white>装備（セット別）</white>"));
        for(int index=page*45;index<Math.min(sets.size(),(page+1)*45);index++) {
            String id=sets.get(index);
            String label=id.isBlank()?text("categories.no-set","<white>セットなし</white>"):
                    definitions.snapshot().config("sets.yml").getString("sets."+id+".name",id);
            button(menu,index%45,"IRON_CHESTPLATE",label,List.of(),()->catalog(player,"equipment",id,0));
        }
        if(page>0) button(menu,45,"ARROW",text("previous","<yellow>前のページ</yellow>"),List.of(),()->equipmentSets(player,page-1));
        if((page+1)*45<sets.size()) button(menu,52,"ARROW",text("next","<yellow>次のページ</yellow>"),List.of(),()->equipmentSets(player,page+1));
        button(menu,49,"ARROW",text("back","<yellow>戻る</yellow>"),List.of(),()->catalogCategories(player));
        player.openInventory(menu.inventory);
    }
    private void catalog(Player player, String category, String setId, int page) {
        if (!allowed(player, "give")) return;
        Set<String> ids = new TreeSet<>();
        switch(category) {
            case "weapons" -> ids.addAll(definitions.snapshot().weapons().keySet());
            case "equipment" -> definitions.snapshot().equipment().values().stream()
                    .filter(d->Objects.requireNonNullElse(d.setId(),"").equals(setId)).forEach(d->ids.add(d.id()));
            case "memories", "materials" -> {
                String file=category.equals("memories")?"divine_hearts.yml":"levels.yml";
                String root=category.equals("memories")?"divine-hearts":"materials";
                var section=definitions.snapshot().config(file).getConfigurationSection(root);
                if(section!=null) ids.addAll(section.getKeys(false));
            }
            default -> throw new IllegalArgumentException("Unknown item category");
        }
        List<String> list = new ArrayList<>(ids); Menu menu = menu(player, text("items", "<green>独自アイテムを取り出す</green>"));
        for (int index = page * 45; index < Math.min(list.size(), (page + 1) * 45); index++) {
            String id = list.get(index); ItemStack icon = items.create(id, 1).orElse(null); if (icon == null) continue;
            menu.inventory.setItem(index % 45, icon); menu.actions.put(index % 45, () -> command(player, "give item " + target(player) + " " + id + " 1"));
        }
        if (page > 0) button(menu, 45, "ARROW", text("previous", "<yellow>前のページ</yellow>"), List.of(), () -> catalog(player, category, setId, page - 1));
        if ((page + 1) * 45 < list.size()) button(menu, 52, "ARROW", text("next", "<yellow>次のページ</yellow>"), List.of(), () -> catalog(player, category, setId, page + 1));
        button(menu,49,"ARROW",text("back","<yellow>戻る</yellow>"),List.of(),()->{
            if(category.equals("equipment")) equipmentSets(player,0); else catalogCategories(player);
        });
        player.openInventory(menu.inventory);
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
        button(menu,10,"NETHER_STAR",text("main-stat","<yellow>メインステを変更</yellow>"),List.of(),()->chooseStat(player,definition,true,key->
            confirm(player,items.displayName(key.name()),()->updateHeld(player,target,instance.getInstanceId(),current->
                current.setMainStat(key,EquipmentGrowthTable.mainValue(definition.slot(),key,1),
                    EquipmentGrowthTable.mainValue(definition.slot(),key,definition.maxLevel()))))));
        int slot=19;
        for(String key:instance.getSubstats().keySet()) {
            button(menu,slot++,"PAPER","<white>"+items.displayName(key)+" "+items.statValue(key,instance.getSubstats().get(key)),
                List.of(text("sub-select-hint","<white>種類と強化回数を一覧から変更します。</white>")),
                ()->editSubstat(player,target,instance.getInstanceId(),definition,key));
        }
        back(menu, player); player.openInventory(menu.inventory);
    }
    private void editSubstat(Player player,Player target,String identity,EquipmentDefinition definition,String key) {
        Menu menu=menu(player,text("sub-editor-title","<gold>サブステの編集</gold>"));
        button(menu,4,"PAPER","<white>"+items.displayName(key),List.of(),()->{});
        for(int count=0;count<=5;count++) {
            int upgrades=count;
            String label=text("sub-upgrade-choice","<yellow>強化 +<count>：<value></yellow>")
                .replace("<count>",Integer.toString(count)).replace("<value>",items.statValue(key,EquipmentGrowthTable.subValue(StatKey.valueOf(key),count)));
            button(menu,10+count,"EXPERIENCE_BOTTLE",label,List.of(),()->confirm(player,label,()->updateHeld(player,target,identity,current->{
                if(!current.getSubstats().containsKey(key)) throw new IllegalArgumentException("サブステが変更されています。");
                current.getSubstatUpgrades().put(key,upgrades); EquipmentGrowth.recalculate(current);
            })));
        }
        button(menu,22,"EMERALD",text("sub-replace","<green>ステータス種類を選択</green>"),List.of(),()->chooseStat(player,definition,false,replacement->
            confirm(player,items.displayName(replacement.name()),()->updateHeld(player,target,identity,current->{
                if(!current.getSubstats().containsKey(key)) throw new IllegalArgumentException("サブステが変更されています。");
                String name=replacement.name();
                if(!name.equals(key)&&current.getSubstats().containsKey(name)) throw new IllegalArgumentException("同じサブステは重複できません。");
                LinkedHashMap<String,Double> ordered=new LinkedHashMap<>();
                current.getSubstats().forEach((oldKey,value)->ordered.put(oldKey.equals(key)?name:oldKey,value));
                int upgrades=current.getSubstatUpgrades().getOrDefault(key,0);
                current.getSubstats().clear();current.getSubstats().putAll(ordered);
                current.getSubstatUpgrades().remove(key);current.getSubstatUpgrades().put(name,upgrades);
                current.getEquipmentUpgradeRolls().replaceAll(oldKey->oldKey.equals(key)?name:oldKey);
                EquipmentGrowth.recalculate(current);
            }))));
        button(menu,49,"ARROW",text("back","<yellow>戻る</yellow>"),List.of(),()->editHeld(player));
        player.openInventory(menu.inventory);
    }
    private void chooseStat(Player player, EquipmentDefinition definition, boolean main, Consumer<StatKey> action) {
        Menu menu = menu(player, text("choose-stat", "<gold>ステータスを選択</gold>")); int slot = 0;
        for (StatKey key : StatKey.values()) {
            if (main ? !DefinitionRegistry.mainAllowed(definition.slot(), key) : !EquipmentGrowthTable.subCandidates().contains(key)) continue;
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
