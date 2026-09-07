package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.*;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class GuiService implements Listener {
    private final JavaPlugin plugin;
    private final DefinitionRegistry definitions;
    private final PlayerDataService players;
    private final StatService stats;
    private final LevelService levels;
    private final CombatStateService combat;
    private final EquipmentService equipment;
    private final SkillTreeService skillTrees;
    private final PartyService parties;
    private final EnhancementService enhancement;
    private final ItemService items;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Map<UUID, SearchState> awaitingSearch = new HashMap<>();

    public GuiService(JavaPlugin plugin, DefinitionRegistry definitions, PlayerDataService players, StatService stats,
                      LevelService levels, CombatStateService combat, EquipmentService equipment, SkillTreeService skillTrees,
                      PartyService parties, EnhancementService enhancement, ItemService items) {
        this.plugin = plugin; this.definitions = definitions; this.players = players; this.stats = stats; this.levels = levels;
        this.combat = combat; this.equipment = equipment; this.skillTrees = skillTrees; this.parties = parties;
        this.enhancement = enhancement; this.items = items;
    }

    public void openMain(Player player) {
        var config = definitions.snapshot().config("gui.yml");
        Inventory inventory = inventory(player, Screen.MAIN, config.getString("main-menu.title", "<dark_gray>CombatCoreSystems</dark_gray>"), config.getInt("main-menu.size", 54), "", 0, "");
        ConfigurationSection entries = config.getConfigurationSection("main-menu.entries");
        if (entries != null) for (String id : entries.getKeys(false)) {
            ConfigurationSection entry = entries.getConfigurationSection(id);
            if (entry != null) inventory.setItem(entry.getInt("slot"), item(entry.getString("icon", "STONE"), entry.getString("name", id), List.of()));
        }
        player.openInventory(inventory);
    }

    public void openStats(Player player) {
        PlayerData data = players.require(player); PlayerStats value = stats.get(player, data);
        Inventory inv = inventory(player, Screen.STATS, "<aqua>ステータス</aqua>", 54, "", 0, "");
        inv.setItem(10, item("PLAYER_HEAD", "<yellow>Lv." + data.getLevel() + "</yellow>", List.of(
                "<gray>EXP: " + data.getExp() + " / " + (data.getLevel() >= 100 ? 0 : levels.requiredExp(data.getLevel())) + "</gray>",
                "<gray>新生回帰: " + data.getRebirthCount() + "</gray>", "<gray>スキルポイント: " + data.getSkillPoints() + "</gray>")));
        inv.setItem(12, item("REDSTONE", "<red>HP " + round(value.maxHp()) + "</red>", List.of()));
        inv.setItem(13, item("IRON_SWORD", "<white>ATK " + round(value.atk()) + "</white>", List.of()));
        inv.setItem(14, item("SHIELD", "<gray>DEF " + round(value.def()) + "</gray>", List.of()));
        inv.setItem(20, item("AMETHYST_SHARD", "<light_purple>会心</light_purple>", List.of("<gray>率: " + percent(value.value(StatKey.CRIT_RATE)), "<gray>ダメージ: " + percent(value.value(StatKey.CRIT_DAMAGE)))));
        int slot = 28;
        for (Element element : List.of(Element.FIRE, Element.WATER, Element.WIND, Element.THUNDER, Element.MOON)) {
            inv.setItem(slot++, item("PRISMARINE_SHARD", "<white>" + element.japaneseName() + "</white>", List.of(
                    "<gray>ダメージ: " + percent(value.elementDamage(element)), "<gray>耐性: " + percent(value.resistance(element)))));
        }
        back(inv); player.openInventory(inv);
    }

    public void openEquipment(Player player) {
        PlayerData data = players.require(player);
        Inventory inv = inventory(player, Screen.EQUIPMENT, "<gold>装備</gold>", 54, "", 0, "");
        Map<EquipmentSlot, Integer> slots = equipmentSlots();
        slots.forEach((slot, index) -> {
            ItemInstance instance = data.getEquipment().get(slot);
            ItemStack icon = instance == null ? item("GRAY_STAINED_GLASS_PANE", "<gray>" + slot + "</gray>", List.of("<dark_gray>未装備</dark_gray>"))
                    : items.create(instance.getDefinitionId(), 1).orElse(item("BARRIER", instance.getDefinitionId(), List.of()));
            inv.setItem(index, icon);
        });
        inv.setItem(49, item("BOOK", "<yellow>武器登録</yellow>", List.of("<gray>メインハンドのCCS武器を対応カテゴリへ登録</gray>")));
        back(inv); player.openInventory(inv);
    }

    public void openSkillTree(Player player, String requestedTree) {
        ConfigurationSection trees = definitions.snapshot().config("skill_trees.yml").getConfigurationSection("trees");
        String tree = requestedTree;
        if ((tree == null || tree.isBlank()) && trees != null) tree = trees.getKeys(false).stream().findFirst().orElse("");
        Inventory inv = inventory(player, Screen.SKILL_TREE, "<green>スキルツリー</green>", 54, tree, 0, "");
        if (trees != null && !tree.isBlank()) {
            ConfigurationSection nodes = trees.getConfigurationSection(tree + ".nodes");
            if (nodes != null) {
                int fallback = 0;
                for (String id : nodes.getKeys(false)) {
                    ConfigurationSection node = nodes.getConfigurationSection(id); if (node == null) continue;
                    int slot = node.getInt("slot", fallback++);
                    int rank = players.require(player).getSkillNodes().getOrDefault(id, 0);
                    inv.setItem(slot, item(node.getString("icon", "PAPER"), node.getString("name", id), List.of("<gray>Rank " + rank + "</gray>", "<yellow>クリックで取得</yellow>")));
                }
            }
        }
        inv.setItem(47, item("REDSTONE", "<red>リセット</red>", List.of("<gray>24時間CT / 全ポイント返却</gray>")));
        for (int preset = 1; preset <= 5; preset++) inv.setItem(47 + preset, item("PAPER", "<aqua>Preset " + preset + "</aqua>", List.of()));
        back(inv); player.openInventory(inv);
    }

    public void openRebirth(Player player, boolean confirm) {
        PlayerData data = players.require(player);
        Inventory inv = inventory(player, confirm ? Screen.REBIRTH_CONFIRM : Screen.REBIRTH, confirm ? "<red>新生回帰の確認</red>" : "<light_purple>新生回帰</light_purple>", 27, "", 0, "");
        inv.setItem(13, item(confirm ? "LIME_CONCRETE" : "NETHER_STAR", confirm ? "<green>新生回帰を実行</green>" : "<light_purple>新生回帰</light_purple>", List.of(
                "<gray>現在Lv: " + data.getLevel() + "</gray>", "<gray>回数: " + data.getRebirthCount() + "</gray>",
                "<gray>現在補正: HP+" + data.getRebirthCount() * 100 + " ATK+" + data.getRebirthCount() * 20 + " DEF+" + data.getRebirthCount() * 10 + "</gray>",
                "<gray>次回: HP+100 ATK+20 DEF+10</gray>")));
        back(inv); player.openInventory(inv);
    }

    public void openParty(Player player) {
        Inventory inv = inventory(player, Screen.PARTY, "<yellow>Party</yellow>", 54, "", 0, "");
        PartyData party = parties.findByPlayer(player.getUniqueId()).orElse(null);
        if (party == null) inv.setItem(22, item("TOTEM_OF_UNDYING", "<green>Partyを作成</green>", List.of()));
        else {
            int slot = 10;
            for (PartyData.Member member : party.getMembers()) {
                var offline = Bukkit.getOfflinePlayer(member.asUuid());
                inv.setItem(slot++, item("PLAYER_HEAD", "<white>" + Optional.ofNullable(offline.getName()).orElse(member.uuid()) + "</white>", List.of(
                        party.leader().equals(member.asUuid()) ? "<gold>Leader</gold>" : "<gray>Member</gray>", offline.isOnline() ? "<green>Online</green>" : "<red>Offline</red>",
                        "<gray>Join: " + member.joinOrder() + "</gray>")));
            }
        }
        back(inv); player.openInventory(inv);
    }

    public void openEnhancement(Player player) {
        Inventory inv = inventory(player, Screen.ENHANCEMENT, "<blue>強化</blue>", 27, "", 0, "");
        inv.setItem(11, item("EXPERIENCE_BOTTLE", "<green>プレイヤーを一括強化</green>", List.of("<gray>修練書をInventoryから自動選択します</gray>")));
        inv.setItem(13, item("ANVIL", "<aqua>メインハンドを一括強化</aqua>", List.of("<gray>対応する強化素材をInventoryから自動選択します</gray>")));
        inv.setItem(15, item("NETHER_STAR", "<gold>武器限界突破</gold>", List.of("<gray>同じ武器を1本消費します</gray>")));
        back(inv); player.openInventory(inv);
    }

    public void openSettings(Player player) {
        PlayerData data = players.require(player);
        Inventory inv = inventory(player, Screen.SETTINGS, "<gray>設定</gray>", 27, "", 0, "");
        inv.setItem(11, item(data.isPvpEnabled() ? "LIME_DYE" : "GRAY_DYE", "<red>PvP: " + onOff(data.isPvpEnabled()) + "</red>", List.of()));
        inv.setItem(15, item(data.isHudEnabled() ? "LIME_DYE" : "GRAY_DYE", "<aqua>HUD: " + onOff(data.isHudEnabled()) + "</aqua>", List.of()));
        back(inv); player.openInventory(inv);
    }

    public void openEncyclopedia(Player player, String category, int page, String query) {
        Inventory inv = inventory(player, Screen.ENCYCLOPEDIA, "<aqua>図鑑</aqua>", 54, category == null ? "" : category, page, query == null ? "" : query);
        if (category == null || category.isBlank()) {
            ConfigurationSection categories = definitions.snapshot().config("encyclopedia.yml").getConfigurationSection("categories");
            if (categories != null) { int slot = 10; for (String id : categories.getKeys(false)) {
                ConfigurationSection c = categories.getConfigurationSection(id); if (c != null) inv.setItem(slot++, item(c.getString("icon", "KNOWLEDGE_BOOK"), c.getString("name", id), List.of("<gray>" + id + "</gray>")));
            }}
        } else fillEncyclopediaEntries(player, inv, category, page, query == null ? "" : query);
        inv.setItem(45, item("ARROW", "<yellow>Previous</yellow>", List.of()));
        inv.setItem(48, item("ARROW", "<yellow>Back</yellow>", List.of()));
        inv.setItem(49, item("OAK_SIGN", "<aqua>Search</aqua>", List.of("<gray>Chatへ名前を入力</gray>")));
        inv.setItem(53, item("ARROW", "<yellow>Next</yellow>", List.of()));
        player.openInventory(inv);
    }

    private void fillEncyclopediaEntries(Player player, Inventory inv, String category, int page, String query) {
        PlayerData data = players.require(player);
        List<Entry> entries = new ArrayList<>();
        if (category.equals("mobs") || category.equals("bosses")) {
            Map<String, MobDefinition> source;
            if (category.equals("bosses")) source = definitions.snapshot().bosses();
            else {
                source = new LinkedHashMap<>(definitions.snapshot().mobs());
                source.putAll(definitions.snapshot().vanillaMobs());
            }
            Set<String> discovered = category.equals("bosses") ? data.getDiscoveredBosses() : data.getDiscoveredMobs();
            source.values().forEach(value -> {
                boolean known = discovered.contains(value.id());
                if (query.isBlank() || (known && value.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))))
                    entries.add(new Entry(value.id(), known ? value.name() : "？？？", known ? value.entityType() : "BARRIER", known ? List.of("<gray>ID: " + value.id() + "</gray>", "<gray>Lv." + value.minLevel() + "-" + value.maxLevel() + "</gray>") : List.of()));
            });
        } else if (category.equals("weapons")) definitions.snapshot().weapons().values().forEach(value -> entries.add(new Entry(value.id(), value.name(), value.material(), List.of("<gray>★" + value.rarity() + "</gray>"))));
        else if (category.equals("equipment")) definitions.snapshot().equipment().values().forEach(value -> entries.add(new Entry(value.id(), value.name(), value.material(), List.of("<gray>★" + value.rarity() + " " + value.slot() + "</gray>"))));
        else if (category.equals("materials")) {
            ConfigurationSection root = definitions.snapshot().config("levels.yml").getConfigurationSection("materials");
            if (root != null) root.getKeys(false).forEach(id -> { ConfigurationSection s = root.getConfigurationSection(id); entries.add(new Entry(id, s.getString("name", id), s.getString("material", "PAPER"), List.of("<gray>EXP: " + s.getLong("exp") + "</gray>"))); });
        } else if (category.equals("divine_hearts")) {
            ConfigurationSection root = definitions.snapshot().config("divine_hearts.yml").getConfigurationSection("divine-hearts");
            if (root != null) root.getKeys(false).forEach(id -> { ConfigurationSection s = root.getConfigurationSection(id); entries.add(new Entry(id, s.getString("name", id), s.getString("material", "NETHER_STAR"), s.getStringList("lore"))); });
        }
        entries.sort(Comparator.comparing(Entry::id));
        int start = Math.max(0, page) * 45;
        for (int i = start; i < Math.min(start + 45, entries.size()); i++) { Entry entry = entries.get(i); inv.setItem(i - start, item(entry.material, entry.name, entry.lore)); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true); int slot = event.getRawSlot(); if (slot < 0 || slot >= event.getInventory().getSize()) return;
        switch (holder.screen) {
            case MAIN -> handleMain(player, slot);
            case STATS -> { if (slot == event.getInventory().getSize() - 1) openMain(player); }
            case EQUIPMENT -> { if (slot == 49) registerHeldWeapon(player); else if (slot == 53) openMain(player); }
            case SKILL_TREE -> handleSkillTree(player, holder.context, slot, event.getCurrentItem());
            case REBIRTH -> { if (slot == 13 && !combat.inCombat(player.getUniqueId())) openRebirth(player, true); else if (slot == 26) openMain(player); }
            case REBIRTH_CONFIRM -> { if (slot == 13 && levels.rebirth(player, players.require(player))) player.closeInventory(); else if (slot == 26) openRebirth(player, false); }
            case PARTY -> { if (slot == 22 && parties.findByPlayer(player.getUniqueId()).isEmpty()) { parties.create(player.getUniqueId()); openParty(player); } else if (slot == 53) openMain(player); }
            case ENHANCEMENT -> { if (slot == 11 || slot == 13 || slot == 15) { EnhancementService.Result result = slot == 11 ? enhancement.enhancePlayer(player, levels) : slot == 13 ? enhancement.enhanceHeld(player) : enhancement.limitBreakHeld(player); player.sendMessage(mini.deserialize(result.success() ? "<green>強化しました。Lv." + result.resultingLevel() + "</green>" : "<red>強化できません: " + result.failure() + "</red>")); } else if (slot == 26) openMain(player); }
            case SETTINGS -> { PlayerData data = players.require(player); if (slot == 11) data.setPvpEnabled(!data.isPvpEnabled()); if (slot == 15) data.setHudEnabled(!data.isHudEnabled()); openSettings(player); }
            case ENCYCLOPEDIA -> handleEncyclopedia(player, holder, slot, event.getCurrentItem());
        }
    }

    private void handleMain(Player player, int slot) {
        ConfigurationSection entries = definitions.snapshot().config("gui.yml").getConfigurationSection("main-menu.entries");
        if (entries == null) return;
        for (String id : entries.getKeys(false)) if (entries.getInt(id + ".slot") == slot) switch (id) {
            case "stats" -> openStats(player); case "equipment" -> openEquipment(player); case "skilltree" -> openSkillTree(player, "");
            case "rebirth" -> openRebirth(player, false); case "party" -> openParty(player); case "enhancement" -> openEnhancement(player);
            case "encyclopedia" -> openEncyclopedia(player, "", 0, ""); case "settings" -> openSettings(player);
        }
    }

    private void registerHeldWeapon(Player player) {
        String id = items.id(player.getInventory().getItemInMainHand()).orElse(""); WeaponDefinition definition = definitions.snapshot().weapons().get(id);
        if (definition == null) return;
        equipment.registerWeapon(player, definition.category() == WeaponDefinition.Category.MELEE ? EquipmentSlot.MELEE_WEAPON : EquipmentSlot.RANGED_WEAPON, player.getInventory().getItemInMainHand());
        openEquipment(player);
    }

    private void handleSkillTree(Player player, String tree, int slot, ItemStack clicked) {
        if (slot == 47) { skillTrees.reset(player); openSkillTree(player, tree); return; }
        if (slot >= 48 && slot <= 52) { skillTrees.switchPreset(player, slot - 47); openSkillTree(player, tree); return; }
        if (slot == 53) { openMain(player); return; }
        ConfigurationSection nodes = definitions.snapshot().config("skill_trees.yml").getConfigurationSection("trees." + tree + ".nodes");
        if (nodes == null) return;
        for (String id : nodes.getKeys(false)) if (nodes.getInt(id + ".slot", -1) == slot) { skillTrees.acquire(player, tree, id); openSkillTree(player, tree); return; }
    }

    private void handleEncyclopedia(Player player, Holder holder, int slot, ItemStack clicked) {
        if (slot == 48) { if (holder.context.isBlank()) openMain(player); else openEncyclopedia(player, "", 0, ""); return; }
        if (slot == 45) { openEncyclopedia(player, holder.context, Math.max(0, holder.page - 1), holder.query); return; }
        if (slot == 53) { openEncyclopedia(player, holder.context, holder.page + 1, holder.query); return; }
        if (slot == 49 && !holder.context.isBlank()) { awaitingSearch.put(player.getUniqueId(), new SearchState(holder.context, holder.page)); player.closeInventory(); player.sendMessage(mini.deserialize("<aqua>検索する名前をChatへ入力してください。</aqua>")); return; }
        if (holder.context.isBlank() && clicked != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().itemName());
            ConfigurationSection categories = definitions.snapshot().config("encyclopedia.yml").getConfigurationSection("categories");
            if (categories != null) for (String id : categories.getKeys(false)) if (PlainTextComponentSerializer.plainText().serialize(mini.deserialize(categories.getString(id + ".name", id))).equals(plain)) { openEncyclopedia(player, id, 0, ""); return; }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        SearchState state = awaitingSearch.remove(event.getPlayer().getUniqueId()); if (state == null) return;
        event.setCancelled(true); String query = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> openEncyclopedia(event.getPlayer(), state.category, 0, query));
    }

    @EventHandler public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Holder && !awaitingSearch.containsKey(event.getPlayer().getUniqueId())) awaitingSearch.remove(event.getPlayer().getUniqueId());
    }

    private Inventory inventory(Player player, Screen screen, String title, int size, String context, int page, String query) {
        Holder holder = new Holder(screen, player.getUniqueId(), context, page, query); Inventory inv = Bukkit.createInventory(holder, size, mini.deserialize(title)); holder.inventory = inv; return inv;
    }
    private void back(Inventory inv) { inv.setItem(inv.getSize() - 1, item("ARROW", "<yellow>Back</yellow>", List.of())); }
    private ItemStack item(String materialName, String name, List<String> lore) {
        Material material = Material.matchMaterial(materialName); ItemStack item = new ItemStack(material == null ? Material.BARRIER : material); ItemMeta meta = item.getItemMeta();
        meta.itemName(mini.deserialize(name)); meta.lore(lore.stream().map(mini::deserialize).toList()); item.setItemMeta(meta); return item;
    }
    private Map<EquipmentSlot, Integer> equipmentSlots() { return Map.of(EquipmentSlot.MELEE_WEAPON, 10, EquipmentSlot.RANGED_WEAPON, 12, EquipmentSlot.HEAD, 19, EquipmentSlot.CHEST, 21, EquipmentSlot.LEGS, 23, EquipmentSlot.FEET, 25, EquipmentSlot.RESONANCE, 30, EquipmentSlot.DIVINE_HEART, 32); }
    private long round(double value) { return Math.round(value); } private String percent(double value) { return String.format(Locale.ROOT, "%.1f%%", value * 100); } private String onOff(boolean value) { return value ? "ON" : "OFF"; }
    private enum Screen { MAIN, STATS, EQUIPMENT, SKILL_TREE, REBIRTH, REBIRTH_CONFIRM, PARTY, ENHANCEMENT, SETTINGS, ENCYCLOPEDIA }
    private static final class Holder implements InventoryHolder {
        final Screen screen; final UUID player; final String context; final int page; final String query; Inventory inventory;
        Holder(Screen screen, UUID player, String context, int page, String query) { this.screen = screen; this.player = player; this.context = context; this.page = page; this.query = query; }
        @Override public Inventory getInventory() { return inventory; }
    }
    private record SearchState(String category, int page) {}
    private record Entry(String id, String name, String material, List<String> lore) {}
}
