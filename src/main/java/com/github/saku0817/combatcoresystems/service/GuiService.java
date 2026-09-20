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
    private final Map<UUID, SearchState> awaitingSearch = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> enhancementSelection = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> rarityFilters = new HashMap<>();

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
            if (entry != null) {
                ItemStack icon = item(entry.getString("icon", "STONE"), entry.getString("name", id), List.of());
                if (entry.contains("enchantment-glint")) {
                    ItemMeta meta = icon.getItemMeta(); meta.setEnchantmentGlintOverride(entry.getBoolean("enchantment-glint")); icon.setItemMeta(meta);
                }
                inventory.setItem(entry.getInt("slot"), icon);
            }
        }
        player.openInventory(inventory);
    }

    public void openStats(Player player) {
        PlayerData data = players.require(player); PlayerStats value = stats.describe(player, data);
        Inventory inv = inventory(player, Screen.STATS, gui("stats.title", "<dark_aqua>✦ ステータス ✦</dark_aqua>"), 54, "", 0, "");
        for (int border : new int[]{0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52})
            inv.setItem(border, item(gui("stats.border-material", "GRAY_STAINED_GLASS_PANE"), " ", List.of()));
        inv.setItem(4, item("PLAYER_HEAD", "<yellow>" + player.getName() + " · Lv." + data.getLevel() + "</yellow>", List.of(
                "<gray>EXP: " + data.getExp() + " / " + (data.getLevel() >= 100 ? 0 : levels.requiredExp(data.getLevel())) + "</gray>",
                "<gray>新生回帰: " + data.getRebirthCount() + "</gray>", "<gray>スキルポイント: " + data.getSkillPoints() + "</gray>")));
        List<String> healthLore = new ArrayList<>(List.of("<white>" + round(data.getHealth()) + " <gray>/</gray> " + round(value.maxHp()) + "</white>"));
        healthLore.addAll(sourceLore(value, StatKey.HP_FLAT, StatKey.HP_PERCENT));
        inv.setItem(20, item("REDSTONE", gui("stats.health-name", "<red>♥ 体力</red>"), healthLore));
        List<String> attackLore = new ArrayList<>();
        for (int i : new int[]{player.getInventory().getHeldItemSlot(), 40}) {
            ItemInstance instance = items.instance(player.getInventory().getItem(i)).orElse(null);
            WeaponDefinition weapon = instance == null ? null : definitions.snapshot().weapon(instance);
            if (weapon == null) continue;
            if (!weapon.canEquip(data.getLevel())) attackLore.add("<red>必要プレイヤーLv." + weapon.minimumEquipLevel() + "～" + weapon.maximumEquipLevel() + "（現在は無効）</red>");
        }
        attackLore.addAll(sourceLore(value, StatKey.ATK_FLAT, StatKey.ATK_PERCENT));
        inv.setItem(22, item("IRON_SWORD", gui("stats.attack-name", "<gold>⚔ 攻撃力</gold>") + " <white>" + round(value.atk()) + "</white>", attackLore));
        inv.setItem(24, item("SHIELD", gui("stats.defense-name", "<aqua>◆ 防御力</aqua>") + " <white>" + round(value.def()) + "</white>", sourceLore(value, StatKey.DEF_FLAT, StatKey.DEF_PERCENT, StatKey.DEF_DOWN, StatKey.DEF_IGNORE, StatKey.DEF_IGNORED_WHEN_HIT)));
        List<String> critLore = new ArrayList<>(List.of("<white>率: " + percent(value.value(StatKey.CRIT_RATE)), "<white>ダメージ: " + percent(value.value(StatKey.CRIT_DAMAGE))));
        critLore.addAll(sourceLore(value, StatKey.CRIT_RATE, StatKey.CRIT_DAMAGE));
        inv.setItem(31, item("AMETHYST_SHARD", gui("stats.critical-name", "<light_purple>✧ 会心</light_purple>"), critLore));
        inv.setItem(13, item("BOOK", gui("stats.other-name", "<white>その他の補正</white>"), sourceLore(value, StatKey.ATTACK_SPEED, StatKey.HEALING_POWER, StatKey.COOLDOWN)));
        int slot = 38;
        for (Element element : List.of(Element.FIRE, Element.WATER, Element.WIND, Element.THUNDER, Element.MOON)) {
            List<String> lore = new ArrayList<>(List.of("<white>ダメージ: " + percent(value.elementDamage(element)), "<white>耐性: " + percent(value.resistance(element))));
            lore.addAll(sourceLore(value, StatKey.valueOf(element.name() + "_DAMAGE"), StatKey.valueOf(element.name() + "_RESISTANCE"), StatKey.valueOf(element.name() + "_RESISTANCE_DOWN")));
            String dye = switch (element) { case FIRE -> "RED_DYE"; case WATER -> "CYAN_DYE"; case WIND -> "LIME_DYE"; case THUNDER -> "PURPLE_DYE"; default -> "LIGHT_BLUE_DYE"; };
            inv.setItem(slot++, item(dye, "<white>" + element.japaneseName() + "</white>", lore));
        }
        back(inv); player.openInventory(inv);
    }

    private List<String> sourceLore(PlayerStats value, StatKey... keys) {
        List<String> result = new ArrayList<>();
        for (var source : value.sources().entrySet()) for (StatKey key : keys) {
            double amount = source.getValue().getOrDefault(key, 0.0);
            if (amount != 0) result.add(gui("stats.source-line", "<white><source>：<stat> <yellow><value></yellow></white>")
                    .replace("<source>", source.getKey()).replace("<stat>", items.displayName(key.name()))
                    .replace("<value>", (amount > 0 ? "+" : "") + items.statValue(key.name(), amount)));
        }
        return result;
    }
    public void sendStatus(Player player) {
        PlayerData data = players.require(player); PlayerStats value = stats.get(player, data);
        player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse(gui("stats.chat-title", "<gold>自分のステータス</gold>")));
        player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse("<white>Lv." + value.level() + " / HP " + round(data.getHealth()) + "/" + round(value.maxHp()) + " / 攻撃力 " + round(value.atk()) + " / 防御力 " + round(value.def())));
        for (StatKey key : StatKey.values()) if (value.value(key) != 0)
            player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse("<white>" + items.displayName(key.name()) + "：" + items.statValue(key.name(), value.value(key))));
    }

    public void openEquipment(Player player) {
        equipment.syncArmor(player);
        PlayerData data = players.require(player);
        Inventory inv = inventory(player, Screen.EQUIPMENT, "<gold>装備</gold>", 54, "", 0, "");
        int weaponIcon = 10;
        for (int i : new int[]{player.getInventory().getHeldItemSlot(), 40}) {
            ItemStack stack = player.getInventory().getItem(i);
            if (definitions.snapshot().weapons().containsKey(items.id(stack).orElse("")) && weaponIcon < 18) inv.setItem(weaponIcon++, stack.clone());
        }
        Map<EquipmentSlot, Integer> slots = equipmentSlots();
        slots.forEach((slot, index) -> {
            ItemInstance instance = data.getEquipment().get(slot);
            ItemStack physical = switch (slot) { case HEAD -> player.getInventory().getHelmet(); case CHEST -> player.getInventory().getChestplate(); case LEGS -> player.getInventory().getLeggings(); case FEET -> player.getInventory().getBoots(); default -> null; };
            ItemStack icon = instance == null ? physical != null && !physical.getType().isAir() ? physical.clone() : item("GRAY_STAINED_GLASS_PANE", "<gray>" + items.displayName(slot.name()) + "</gray>", List.of("<dark_gray>未装備</dark_gray>"))
                    : Optional.ofNullable(findInstance(player, instance.getInstanceId())).map(ItemStack::clone).orElse(item("BARRIER", instance.getDefinitionId(), List.of()));
            if (instance != null || physical != null && !physical.getType().isAir()) {
                ItemMeta meta = icon.getItemMeta();
                List<Component> lore = new ArrayList<>(Optional.ofNullable(meta.lore()).orElse(List.of()));
                lore.add(com.github.saku0817.combatcoresystems.util.ItemText.parse(gui("equipment.manage-equipped", "<green>装備中 — タップで交換・解除</green>")));
                meta.lore(lore); icon.setItemMeta(meta);
            }
            inv.setItem(index, icon);
        });
        inv.setItem(45, item("CHEST", gui("equipment.select", "<green>所持品から装備を選ぶ</green>"), List.of()));
        back(inv); player.openInventory(inv);
    }

    private void openEquipmentList(Player player) {
        openEquipmentList(player, null);
    }

    private void openEquipmentList(Player player, EquipmentSlot filter) {
        equipment.syncArmor(player);
        Inventory inv = inventory(player, Screen.EQUIPMENT_LIST, gui("equipment.list-title", "<gold>タップして装備</gold>"), 54, filter == null ? "" : filter.name(), 0, "");
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.getType().isAir()) continue;
            String id = items.id(stack).orElse("");
            if (equipment.slotOf(id) != null && (filter == null || equipment.slotOf(id) == filter)) {
                inv.setItem(i, stack.clone());
                ItemInstance instance = items.instance(stack).orElse(null);
                if (instance != null) ((Holder) inv.getHolder()).actions.put(i, instance.getInstanceId());
            }
        }
        if (filter != null) inv.setItem(45, item("BARRIER", gui("equipment.unequip", "<yellow>この部位を外す</yellow>"), List.of()));
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
                ((Holder) inv.getHolder()).actions.put(slot, member.uuid());
                inv.setItem(slot++, item("PLAYER_HEAD", "<white>" + Optional.ofNullable(offline.getName()).orElse(member.uuid()) + "</white>", List.of(
                        party.leader().equals(member.asUuid()) ? "<gold>Leader</gold>" : "<gray>Member</gray>", offline.isOnline() ? "<green>Online</green>" : "<red>Offline</red>",
                        "<gray>Join: " + member.joinOrder() + "</gray>")));
            }
            inv.setItem(45, item("OAK_DOOR", gui("party.leave", "<red>退出</red>"), List.of()));
            if (party.leader().equals(player.getUniqueId())) {
                inv.setItem(47, item("PAPER", gui("party.invite", "<green>プレイヤーを招待</green>"), List.of()));
                inv.setItem(49, item("BARRIER", gui("party.disband", "<red>パーティを解散</red>"), List.of()));
            }
        }
        if (party == null) inv.setItem(47, item("PAPER", gui("party.invitations", "<yellow>届いた招待</yellow>"), List.of()));
        back(inv); player.openInventory(inv);
    }

    private void openPartyAction(Player player, String action, String target, int page) {
        Inventory inv = inventory(player, Screen.PARTY_ACTION, gui("party.actions-title", "<yellow>パーティ操作</yellow>"), 54, action, page, target);
        Holder holder = (Holder) inv.getHolder();
        if (action.equals("invite") || action.equals("invitations")) {
            List<UUID> candidates = action.equals("invite") ? Bukkit.getOnlinePlayers().stream().filter(p -> !p.equals(player) && parties.findByPlayer(p.getUniqueId()).isEmpty()).map(Player::getUniqueId).toList()
                    : parties.invitations(player.getUniqueId()).stream().map(PartyService.Invite::inviter).distinct().toList();
            for (int i = Math.max(0, page) * 45; i < Math.min(candidates.size(), (Math.max(0, page) + 1) * 45); i++) {
                UUID uuid = candidates.get(i); int slot = i % 45;
                holder.actions.put(slot, uuid.toString());
                inv.setItem(slot, item("PLAYER_HEAD", Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString()), List.of()));
            }
            inv.setItem(45, item("ARROW", "<yellow>前へ</yellow>", List.of()));
            inv.setItem(52, item("ARROW", "<yellow>次へ</yellow>", List.of()));
        } else if (action.equals("member")) {
            inv.setItem(11, item("BARRIER", gui("party.kick", "<red>メンバーを除名</red>"), List.of()));
            inv.setItem(15, item("GOLDEN_HELMET", gui("party.transfer", "<yellow>リーダーを移譲</yellow>"), List.of()));
        } else if (action.equals("respond")) {
            inv.setItem(11, item("LIME_DYE", gui("party.accept", "<green>招待を承認</green>"), List.of()));
            inv.setItem(15, item("RED_DYE", gui("party.decline", "<red>招待を辞退</red>"), List.of()));
        } else inv.setItem(13, item("LIME_CONCRETE", gui("party.confirm", "<green>実行する</green>"), List.of()));
        back(inv); player.openInventory(inv);
    }

    private void handleParty(Player player, Holder holder, int slot) {
        PartyData party = parties.findByPlayer(player.getUniqueId()).orElse(null);
        if (slot == 53) { openMain(player); return; }
        if (party == null) {
            if (slot == 22) { parties.create(player.getUniqueId()); openParty(player); }
            if (slot == 47) openPartyAction(player, "invitations", "", 0);
            return;
        }
        if (slot == 45) openPartyAction(player, "leave", "", 0);
        else if (party.leader().equals(player.getUniqueId())) {
            if (slot == 47) openPartyAction(player, "invite", "", 0);
            else if (slot == 49) openPartyAction(player, "disband", "", 0);
            else if (holder.actions.containsKey(slot)) openPartyAction(player, "member", holder.actions.get(slot), 0);
        }
    }

    private void handlePartyAction(Player player, Holder holder, int slot) {
        if (slot == 53) { openParty(player); return; }
        if (holder.context.equals("invite") || holder.context.equals("invitations")) {
            if (slot == 45 || slot == 52) { openPartyAction(player, holder.context, "", Math.max(0, holder.page + (slot == 45 ? -1 : 1))); return; }
            String id = holder.actions.get(slot); if (id == null) return;
            if (holder.context.equals("invitations")) { openPartyAction(player, "respond", id, 0); return; }
            if (parties.invite(player.getUniqueId(), UUID.fromString(id))) {
                Player target = Bukkit.getPlayer(UUID.fromString(id));
                if (target != null) target.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse(gui("party.invited", "<yellow>招待が届きました。/ccs open party で確認できます。</yellow>")));
            }
        } else if (holder.context.equals("respond")) {
            if (slot == 11) parties.accept(player.getUniqueId(), UUID.fromString(holder.query));
            else if (slot == 15) parties.decline(player.getUniqueId(), UUID.fromString(holder.query));
        } else if (holder.context.equals("member")) {
            if (slot == 11) parties.kick(player.getUniqueId(), UUID.fromString(holder.query));
            else if (slot == 15) parties.transfer(player.getUniqueId(), UUID.fromString(holder.query));
        } else if (slot == 13) {
            if (holder.context.equals("leave")) parties.leave(player.getUniqueId());
            else if (holder.context.equals("disband")) parties.disband(player.getUniqueId());
        }
        openParty(player);
    }

    public void openEnhancement(Player player) {
        Inventory inv = inventory(player, Screen.ENHANCEMENT, gui("enhancement.title", "<blue>強化</blue>"), 27, "", 0, "");
        inv.setItem(10, item("EXPERIENCE_BOTTLE", gui("enhancement.player.name", "<green>プレイヤーレベルアップ</green>"), List.of(gui("enhancement.player.lore", "<gray>3種類の素材を選んでレベルアップ</gray>"))));
        inv.setItem(12, item("DIAMOND_SWORD", gui("enhancement.weapon.name", "<aqua>武器レベルアップ＆限界突破</aqua>"), List.of(gui("enhancement.weapon.lore", "<gray>所持武器から選択</gray>"))));
        inv.setItem(14, item("DIAMOND_CHESTPLATE", gui("enhancement.equipment.name", "<gold>装備レベルアップ</gold>"), List.of(gui("enhancement.equipment.lore", "<gray>所持・装備中の装備から選択</gray>"))));
        inv.setItem(16, item("HOPPER", gui("enhancement.conversion.name", "<light_purple>変換</light_purple>"), List.of(gui("enhancement.conversion.lore", "<gray>実物素材・数値素材・等級を相互変換</gray>"))));
        back(inv); player.openInventory(inv);
    }

    private void openEnhancePlayer(Player player) {
        enhancementSelection.put(player.getUniqueId(), new LinkedHashMap<>());
        openEnhanceDetail(player, Screen.ENHANCE_PLAYER, "PLAYER", "");
    }

    private void openEnhanceItems(Player player, String type) {
        Screen screen = type.equals("WEAPON") ? Screen.ENHANCE_WEAPON_LIST : Screen.ENHANCE_EQUIPMENT_LIST;
        Inventory inv = inventory(player, screen, type.equals("WEAPON") ? gui("enhancement.weapon-list-title", "<aqua>武器を選択</aqua>") : gui("enhancement.equipment-list-title", "<gold>装備を選択</gold>"), 54, type, 0, "");
        int index = 0;
        Set<String> seen = new HashSet<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            ItemInstance instance = items.instance(stack).orElse(null); if (instance == null || !seen.add(instance.getInstanceId())) continue;
            boolean matches = type.equals("WEAPON") ? definitions.snapshot().weapons().containsKey(instance.getDefinitionId()) : definitions.snapshot().equipment().containsKey(instance.getDefinitionId());
            if (!matches || index >= 45) continue;
            ItemStack icon = stack.clone(); icon.setAmount(1); inv.setItem(index++, icon);
        }
        ItemStack resonance = equipment.storedResonance(players.require(player));
        if (type.equals("EQUIPMENT") && resonance != null && index < 45) inv.setItem(index, resonance);
        back(inv); player.openInventory(inv);
    }

    private void openEnhanceDetail(Player player, Screen screen, String type, String instanceId) {
        Inventory inv = inventory(player, screen, gui("enhancement.detail-title", "<blue>新強化GUI</blue>"), 54, instanceId, 0, type);
        Map<String, Long> selected = enhancementSelection.computeIfAbsent(player.getUniqueId(), ignored -> new LinkedHashMap<>());
        EnhancementService.Preview preview;
        if (type.equals("PLAYER")) {
            preview = enhancement.previewPlayer(player, levels, selected);
            inv.setItem(13, item("PLAYER_HEAD", gui("enhancement.player-target", "<yellow>プレイヤー Lv.<level></yellow>").replace("<level>", Integer.toString(preview.beforeLevel())), List.of()));
        } else {
            ItemStack target = findInstance(player, instanceId); ItemInstance instance = items.instance(target).orElse(null);
            if (instance == null) { openEnhancement(player); return; }
            preview = enhancement.previewItem(instance, selected); ItemStack icon = target.clone(); icon.setAmount(1); inv.setItem(13, icon);
        }
        inv.setItem(4, item("NETHER_STAR", gui("enhancement.preview-name", "<white>強化プレビュー</white>"), List.of(
                gui("enhancement.preview-before", "<gray>強化前: Lv.<level> EXP <exp></gray>").replace("<level>", Integer.toString(preview.beforeLevel())).replace("<exp>", Long.toString(preview.beforeExp())),
                gui("enhancement.preview-after", "<green>強化後: Lv.<level> EXP <exp></green>").replace("<level>", Integer.toString(preview.afterLevel())).replace("<exp>", Long.toString(preview.afterExp())),
                gui("enhancement.preview-gain", "<yellow>上昇幅: Lv. +<levels> / EXP +<exp></yellow>").replace("<levels>", Integer.toString(preview.afterLevel() - preview.beforeLevel())).replace("<exp>", Long.toString(preview.gainedExp())))));
        List<EnhancementService.MaterialInfo> materials = enhancement.materials(type);
        int[] slots = {29, 31, 33};
        for (int i = 0; i < Math.min(3, materials.size()); i++) {
            var material = materials.get(i); long count = selected.getOrDefault(material.id(), 0L);
            long available = enhancement.available(player, material.id());
            inv.setItem(slots[i], materialIcon(material, List.of(
                    gui("enhancement.material-count", "<gray>選択: <selected> / 所持: <available></gray>").replace("<selected>", Long.toString(count)).replace("<available>", Long.toString(available)),
                    gui("enhancement.material-exp", "<gray>獲得EXP: <exp></gray>").replace("<exp>", Long.toString(count * material.exp())),
                    gui("enhancement.tap-controls", "<yellow>上下のボタンで使用数を変更</yellow>"))));
            inv.setItem(slots[i] - 9, item("LIME_DYE", gui("enhancement.add-one", "<green>1個増やす</green>"), List.of()));
            inv.setItem(slots[i] + 9, item("RED_DYE", gui("enhancement.remove-one", "<red>1個減らす</red>"), List.of()));
        }
        if (type.equals("WEAPON")) inv.setItem(47, item("AMETHYST_SHARD", gui("enhancement.limit-break", "<light_purple>限界突破</light_purple>"), List.of(gui("enhancement.limit-break-lore", "<gray>同じ武器を1本消費</gray>"))));
        inv.setItem(49, item("LIME_CONCRETE", gui("enhancement.confirm", "<green>この内容で強化</green>"), List.of()));
        inv.setItem(45, item("CHEST", gui("enhancement.select-all", "<green>全て選択</green>"), List.of(gui("enhancement.select-all-lore", "<gray>対応する実物・数値素材を全て選択します。確定するまで消費しません。</gray>"))));
        inv.setItem(46, item("BARRIER", gui("enhancement.clear-selection", "<yellow>選択を解除</yellow>"), List.of()));
        back(inv); player.openInventory(inv);
    }

    private void openConversion(Player player) {
        Inventory inv = inventory(player, Screen.ENHANCE_CONVERSION, gui("enhancement.conversion-title", "<light_purple>強化素材の変換</light_purple>"), 54, "", 0, "");
        int slot = 9;
        for (String type : List.of("PLAYER", "WEAPON", "EQUIPMENT")) for (var material : enhancement.materials(type)) {
            if (slot >= 45) break;
            long virtual = players.require(player).getEnhancementMaterials().getOrDefault(material.id(), 0L);
            long physical = physicalCount(player, material.id());
            inv.setItem(slot++, materialIcon(material, List.of(
                    gui("enhancement.conversion-count", "<gray>実物: <physical> / 数値: <virtual></gray>").replace("<physical>", Long.toString(physical)).replace("<virtual>", Long.toString(virtual)),
                    gui("enhancement.conversion-select", "<yellow>タップして変換方法を選択</yellow>"))));
        }
        back(inv); player.openInventory(inv);
    }

    private void openLimitBreak(Player player, String targetId) {
        ItemInstance target = items.instance(findInstance(player, targetId)).orElse(null);
        if (target == null) { openEnhancement(player); return; }
        Inventory inv = inventory(player, Screen.LIMIT_BREAK_SELECT, gui("enhancement.duplicate-title", "<light_purple>消費する同名武器を選択</light_purple>"), 54, targetId, 0, "");
        Holder holder = (Holder) inv.getHolder();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            ItemInstance candidate = items.instance(stack).orElse(null);
            if (EnhancementService.validDuplicate(target, candidate)) {
                inv.setItem(slot, stack.clone()); holder.actions.put(slot, slot + ":" + candidate.getInstanceId());
            }
        }
        if (holder.actions.isEmpty()) inv.setItem(22, item("BARRIER", gui("enhancement.no-duplicate", "<red>消費可能な同名武器がありません</red>"), List.of()));
        back(inv); player.openInventory(inv);
    }

    private void handleLimitBreak(Player player, Holder holder, int slot) {
        if (slot == 53) { openEnhanceDetail(player, Screen.ENHANCE_WEAPON, "WEAPON", holder.context); return; }
        if (holder.screen == Screen.LIMIT_BREAK_SELECT) {
            String candidate = holder.actions.get(slot); if (candidate == null) return;
            Inventory inv = inventory(player, Screen.LIMIT_BREAK_CONFIRM, gui("enhancement.duplicate-confirm-title", "<red>この武器を消費しますか？</red>"), 54, holder.context, 0, candidate);
            ItemStack target = findInstance(player, holder.context);
            if (target != null) inv.setItem(11, target.clone());
            ItemStack material = player.getInventory().getItem(Integer.parseInt(candidate.split(":", 2)[0]));
            if (material != null) inv.setItem(15, material.clone());
            inv.setItem(49, item("LIME_CONCRETE", gui("enhancement.duplicate-confirm", "<green>右の武器を1本消費して、左の武器を限界突破</green>"), List.of()));
            back(inv); player.openInventory(inv); return;
        }
        if (slot != 49) return;
        String[] material = holder.query.split(":", 2);
        var result = enhancement.limitBreak(player, holder.context, Integer.parseInt(material[0]), material[1]);
        player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse(result.success()
                ? gui("enhancement.limit-break-success", "<green>限界突破しました。</green>")
                : gui("enhancement.failure", "<red>強化できません: <reason></red>").replace("<reason>", gui("enhancement.failure-reasons." + result.failure(), result.failure()))));
        if (result.success()) { equipment.syncArmor(player); levels.apply(player, players.require(player), false); }
        openEnhanceDetail(player, Screen.ENHANCE_WEAPON, "WEAPON", holder.context);
    }

    private void openConversionDetail(Player player, String id) {
        Inventory inv = inventory(player, Screen.CONVERSION_DETAIL, gui("enhancement.conversion-title", "<light_purple>強化素材の変換</light_purple>"), 27, id, 0, "");
        ItemStack material = items.create(id, 1).orElse(null);
        if (material != null) {
            ItemMeta meta = material.getItemMeta();
            List<Component> lore = new ArrayList<>(Optional.ofNullable(meta.lore()).orElse(List.of()));
            lore.add(com.github.saku0817.combatcoresystems.util.ItemText.parse(gui("enhancement.conversion-count", "<gray>実物: <physical> / 数値: <virtual></gray>")
                    .replace("<physical>", Long.toString(physicalCount(player, id)))
                    .replace("<virtual>", Long.toString(players.require(player).getEnhancementMaterials().getOrDefault(id, 0L)))));
            meta.lore(lore); material.setItemMeta(meta); inv.setItem(4, material);
        }
        inv.setItem(10, item("HOPPER", gui("enhancement.deposit", "<green>実物1個 → 数値1個</green>"), List.of()));
        inv.setItem(12, item("CHEST", gui("enhancement.withdraw", "<green>数値1個 → 実物1個</green>"), List.of()));
        inv.setItem(14, item("GOLD_INGOT", gui("enhancement.upgrade-tier", "<yellow>数値10個 → 上級1個</yellow>"), List.of()));
        inv.setItem(16, item("IRON_NUGGET", gui("enhancement.downgrade-tier", "<yellow>数値1個 → 下級10個</yellow>"), List.of()));
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
        inv.setItem(45, item("ARROW", gui("navigation.previous", "<yellow>前のページ</yellow>"), List.of()));
        inv.setItem(48, item("ARROW", gui("navigation.back", "<yellow>戻る</yellow>"), List.of()));
        inv.setItem(49, item("OAK_SIGN", gui("navigation.search", "<aqua>検索</aqua>"), List.of(gui("navigation.search-hint", "<gray>チャットへ名前を入力</gray>"))));
        inv.setItem(53, item("ARROW", gui("navigation.next", "<yellow>次のページ</yellow>"), List.of()));
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
                    entries.add(new Entry(value.id(), known ? value.name() : "？？？", known ? mobIcon(category, value.id()) : "BARRIER", known ? List.of("<gray>ID: " + value.id() + "</gray>", "<gray>Lv." + value.minLevel() + "-" + value.maxLevel() + "</gray>") : List.of()));
            });
        } else if (category.equals("weapons")) definitions.snapshot().weapons().values().forEach(value -> entries.add(new Entry(value.id(), value.name(), value.material(), List.of(items.rarityLine(value.rarity())))));
        else if (category.equals("equipment")) definitions.snapshot().equipment().values().forEach(value -> entries.add(new Entry(value.id(), value.name(), value.material(), List.of("<gray>★" + value.rarity() + " " + items.displayName(value.slot().name()) + "</gray>"))));
        else if (category.equals("materials")) {
            ConfigurationSection root = definitions.snapshot().config("levels.yml").getConfigurationSection("materials");
            if (root != null) root.getKeys(false).forEach(id -> { ConfigurationSection s = root.getConfigurationSection(id); entries.add(new Entry(id, s.getString("name", id), s.getString("material", "PAPER"), List.of("<gray>EXP: " + s.getLong("exp") + "</gray>"))); });
        } else if (category.equals("divine_hearts")) {
            ConfigurationSection root = definitions.snapshot().config("divine_hearts.yml").getConfigurationSection("divine-hearts");
            if (root != null) root.getKeys(false).forEach(id -> { ConfigurationSection s = root.getConfigurationSection(id); entries.add(new Entry(id, s.getString("name", id), s.getString("material", "NETHER_STAR"), s.getStringList("lore"))); });
        }
        int rarity = rarityFilters.getOrDefault(player.getUniqueId(), Map.of()).getOrDefault(category, 0);
        entries.removeIf(entry -> (rarity > 0 && entryRarity(category, entry.id()) != rarity)
                || (!category.equals("mobs") && !category.equals("bosses") && !query.isBlank()
                && !PlainTextComponentSerializer.plainText().serialize(com.github.saku0817.combatcoresystems.util.ItemText.parse(entry.name())).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))));
        entries.sort(Comparator.comparing(Entry::id));
        int start = Math.max(0, page) * 45;
        for (int i = start; i < Math.min(start + 45, entries.size()); i++) {
            Entry entry = entries.get(i); inv.setItem(i - start, item(entry.material, entry.name, entry.lore));
            if (category.equals("weapons")) {
                ItemStack weaponIcon = inv.getItem(i - start);
                ItemMeta meta = weaponIcon.getItemMeta(); meta.setEnchantmentGlintOverride(items.glint(entry.id)); weaponIcon.setItemMeta(meta);
            }
            if (!entry.name.equals("？？？")) ((Holder) inv.getHolder()).actions.put(i - start, entry.id);
        }
    }

    private void openEncyclopediaDetail(Player player, Holder parent, String id) {
        Inventory inv = inventory(player, Screen.ENCYCLOPEDIA_DETAIL, gui("encyclopedia.detail-title", "<aqua>図鑑・詳細</aqua>"), 54, parent.context, parent.page, parent.query);
        ItemStack icon = items.create(id, 1).orElse(null);
        WeaponDefinition weapon = definitions.snapshot().weapons().get(id);
        if (weapon != null) {
            inv.setItem(4, item(weapon.material(), weapon.name(), List.of(items.rarityLine(weapon.rarity()))));
            var talent = weapon.options().talent();
            inv.setItem(20, item("NETHER_STAR", gui("encyclopedia.talent", "<green>天賦</green>") + " — " + (talent == null ? gui("encyclopedia.undefined", "未設定") : talent.name()), talent == null ? List.of() : talent.description()));
            inv.setItem(22, abilityIcon(weapon.skill(), "skill", "スキル"));
            inv.setItem(24, abilityIcon(weapon.ultimate(), "ultimate", "必殺技"));
            inv.setItem(40, item("WRITABLE_BOOK", gui("encyclopedia.description", "<gold>武器の説明</gold>"), weapon.lore()));
        } else if (icon != null) inv.setItem(13, icon);
        else {
            MobDefinition mob = definitions.snapshot().bosses().get(id);
            if (mob == null) mob = definitions.snapshot().mobs().get(id);
            if (mob == null) mob = definitions.snapshot().vanillaMobs().get(id);
            if (mob != null) inv.setItem(13, item(mobIcon(parent.context, id), mob.name(), List.of("<gray>ID: " + id, "<gray>Lv: " + mob.minLevel() + "–" + mob.maxLevel(),
                    "<gray>HP: " + mob.hpAtMin() + "–" + mob.hpAtMax(), "<gray>ATK: " + mob.atkAtMin() + "–" + mob.atkAtMax(),
                    "<gray>DEF: " + mob.defAtMin() + "–" + mob.defAtMax(), "<gray>EXP: " + mob.exp(), "<gray>属性: " + mob.nativeElement().japaneseName())));
        }
        back(inv); player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(event.getInventory().getHolder() instanceof Holder holder)) return;
        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (slot >= topSize) {
            // Keep ordinary rearrangement in the player's inventory; reject transfers into the menu.
            if (event.isShiftClick() || event.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR) event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        if (slot < 0) return;
        if (!event.getCursor().getType().isAir()) {
            ItemStack cursor = event.getCursor().clone();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.getItemOnCursor().equals(cursor)) return;
                player.setItemOnCursor(null);
                var remainder = player.getInventory().addItem(cursor);
                if (!remainder.isEmpty()) player.setItemOnCursor(remainder.values().iterator().next());
                equipment.syncArmor(player);
            });
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() == holder) handleClick(player, holder, event, slot);
        });
    }

    private void handleClick(Player player, Holder holder, InventoryClickEvent event, int slot) {
        switch (holder.screen) {
            case MAIN -> handleMain(player, slot);
            case STATS -> { if (slot == event.getInventory().getSize() - 1) openMain(player); }
            case EQUIPMENT -> {
                if (slot == 53) openMain(player);
                else if (slot == 45) openEquipmentList(player);
                else equipmentSlots().forEach((kind, index) -> { if (index == slot) openEquipmentList(player, kind); });
            }
            case EQUIPMENT_LIST -> {
                if (slot == 53) openEquipment(player);
                else if (slot == 45 && !holder.context.isBlank()) {
                    if (equipment.unequip(player, EquipmentSlot.valueOf(holder.context))) levels.apply(player, players.require(player), false);
                    else player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse(gui("equipment.failed", "<red>変更できません。戦闘状態と所持品の空きを確認してください。</red>")));
                    openEquipment(player);
                }
                else if (slot < 36) {
                    String selected = holder.actions.get(slot);
                    ItemInstance actual = items.instance(player.getInventory().getItem(slot)).orElse(null);
                    if (selected != null && actual != null && selected.equals(actual.getInstanceId()) && equipment.equip(player, slot)) levels.apply(player, players.require(player), false);
                    else player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse(gui("equipment.failed", "<red>装備できません。戦闘状態・所持品・設定を確認してください。</red>")));
                    openEquipment(player);
                }
            }
            case SKILL_TREE -> handleSkillTree(player, holder.context, slot, event.getCurrentItem());
            case REBIRTH -> { if (slot == 13) { if (rebirthAllowed(player)) openRebirth(player, true); } else if (slot == 26) openMain(player); }
            case REBIRTH_CONFIRM -> { if (slot == 13 && rebirthAllowed(player) && levels.rebirth(player, players.require(player))) player.closeInventory(); else if (slot == 26) openRebirth(player, false); }
            case LIMIT_BREAK_SELECT, LIMIT_BREAK_CONFIRM -> handleLimitBreak(player, holder, slot);
            case PARTY -> handleParty(player, holder, slot);
            case PARTY_ACTION -> handlePartyAction(player, holder, slot);
            case ENHANCEMENT -> { if (slot == 10) openEnhancePlayer(player); else if (slot == 12) openEnhanceItems(player, "WEAPON"); else if (slot == 14) openEnhanceItems(player, "EQUIPMENT"); else if (slot == 16) openConversion(player); else if (slot == 26) openMain(player); }
            case ENHANCE_WEAPON_LIST, ENHANCE_EQUIPMENT_LIST -> handleEnhanceList(player, holder, slot, event.getCurrentItem());
            case ENHANCE_PLAYER, ENHANCE_WEAPON, ENHANCE_EQUIPMENT -> handleEnhanceDetail(player, holder, event);
            case ENHANCE_CONVERSION -> handleConversion(player, slot, event);
            case CONVERSION_DETAIL -> {
                if (slot == 26) { openConversion(player); return; }
                boolean ok = switch (slot) {
                    case 10 -> enhancement.deposit(player, holder.context, 1);
                    case 12 -> enhancement.withdraw(player, holder.context, 1);
                    case 14 -> enhancement.exchangeTier(player, holder.context, true);
                    case 16 -> enhancement.exchangeTier(player, holder.context, false);
                    default -> true;
                };
                if (!ok) player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse(gui("enhancement.conversion-failed", "<red>素材または空きが不足しています。</red>")));
                openConversionDetail(player, holder.context);
            }
            case SETTINGS -> { if (slot == 26) { openMain(player); return; } PlayerData data = players.require(player); if (slot == 11) data.setPvpEnabled(!data.isPvpEnabled()); if (slot == 15) data.setHudEnabled(!data.isHudEnabled()); openSettings(player); }
            case ENCYCLOPEDIA -> handleEncyclopedia(player, holder, slot, event.getCurrentItem());
            case ENCYCLOPEDIA_RARITY -> {
                if (slot == 53) openEncyclopedia(player, "", 0, "");
                else if (holder.actions.containsKey(slot)) {
                    rarityFilters.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>()).put(holder.context, Integer.parseInt(holder.actions.get(slot)));
                    openEncyclopedia(player, holder.context, 0, "");
                }
            }
            case ENCYCLOPEDIA_DETAIL -> { if (slot == 53) openEncyclopedia(player, holder.context, holder.page, holder.query); }
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

    private void handleEnhanceList(Player player, Holder holder, int slot, ItemStack clicked) {
        if (slot == 53) { openEnhancement(player); return; }
        ItemInstance instance = items.instance(clicked).orElse(null); if (instance == null) return;
        enhancementSelection.put(player.getUniqueId(), new LinkedHashMap<>());
        openEnhanceDetail(player, holder.context.equals("WEAPON") ? Screen.ENHANCE_WEAPON : Screen.ENHANCE_EQUIPMENT, holder.context, instance.getInstanceId());
    }

    private void handleEnhanceDetail(Player player, Holder holder, InventoryClickEvent event) {
        int slot = event.getRawSlot(); String type = holder.query; Screen screen = holder.screen;
        if (slot == 53) { if (type.equals("PLAYER")) openEnhancement(player); else openEnhanceItems(player, type); return; }
        if (slot == 47 && type.equals("WEAPON")) {
            openLimitBreak(player, holder.context); return;
        }
        if (slot == 45 || slot == 46) {
            Map<String, Long> selection = new LinkedHashMap<>();
            if (slot == 45) for (var material : enhancement.materials(type)) selection.put(material.id(), enhancement.available(player, material.id()));
            enhancementSelection.put(player.getUniqueId(), selection);
            openEnhanceDetail(player, screen, type, holder.context); return;
        }
        if (slot == 49) {
            Map<String, Long> selected = enhancementSelection.getOrDefault(player.getUniqueId(), Map.of());
            EnhancementService.Result result = type.equals("PLAYER") ? enhancement.enhancePlayer(player, levels, selected) : enhancement.enhanceItem(player, holder.context, selected);
            player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse(result.success() ? gui("enhancement.success", "<green>強化しました。Lv.<level></green>").replace("<level>", Integer.toString(result.resultingLevel())) : gui("enhancement.failure", "<red>強化できません: <reason></red>").replace("<reason>", result.failure())));
            if (result.success()) enhancementSelection.put(player.getUniqueId(), new LinkedHashMap<>());
            openEnhanceDetail(player, screen, type, holder.context); return;
        }
        List<EnhancementService.MaterialInfo> valid = enhancement.materials(type);
        int index = switch (slot) { case 20, 38 -> 0; case 22, 40 -> 1; case 24, 42 -> 2; default -> -1; };
        if (index < 0 || index >= Math.min(3, valid.size())) return;
        String id = valid.get(index).id();
        long delta = slot < 29 ? 1 : -1;
        Map<String, Long> selected = enhancementSelection.computeIfAbsent(player.getUniqueId(), ignored -> new LinkedHashMap<>());
        long next = Math.max(0, Math.min(enhancement.available(player, id), selected.getOrDefault(id, 0L) + delta));
        if (next == 0) selected.remove(id); else selected.put(id, next);
        openEnhanceDetail(player, screen, type, holder.context);
    }

    private void handleConversion(Player player, int slot, InventoryClickEvent event) {
        if (slot == 53) { openEnhancement(player); return; }
        String id = items.id(event.getCurrentItem()).orElse(""); if (id.isBlank()) return;
        openConversionDetail(player, id);
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
        if (slot == 48) { if (holder.context.isBlank()) openMain(player); else if (rarityCategory(holder.context)) openRarity(player, holder.context); else openEncyclopedia(player, "", 0, ""); return; }
        if (slot == 45) { openEncyclopedia(player, holder.context, Math.max(0, holder.page - 1), holder.query); return; }
        if (slot == 53) { openEncyclopedia(player, holder.context, holder.page + 1, holder.query); return; }
        if (slot == 49 && !holder.context.isBlank()) { awaitingSearch.put(player.getUniqueId(), new SearchState(holder.context, holder.page)); player.closeInventory(); player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse("<aqua>検索する名前をChatへ入力してください。</aqua>")); return; }
        if (!holder.context.isBlank() && holder.actions.containsKey(slot)) { openEncyclopediaDetail(player, holder, holder.actions.get(slot)); return; }
        if (holder.context.isBlank() && clicked != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().itemName());
            ConfigurationSection categories = definitions.snapshot().config("encyclopedia.yml").getConfigurationSection("categories");
            if (categories != null) for (String id : categories.getKeys(false)) if (PlainTextComponentSerializer.plainText().serialize(com.github.saku0817.combatcoresystems.util.ItemText.parse(categories.getString(id + ".name", id))).equals(plain)) { if (rarityCategory(id)) openRarity(player, id); else openEncyclopedia(player, id, 0, ""); return; }
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) event.setCancelled(true);
    }

    private Inventory inventory(Player player, Screen screen, String title, int size, String context, int page, String query) {
        Holder holder = new Holder(screen, player.getUniqueId(), context, page, query); Inventory inv = Bukkit.createInventory(holder, size, com.github.saku0817.combatcoresystems.util.ItemText.parse(title)); holder.inventory = inv; return inv;
    }
    private boolean rebirthAllowed(Player player) {
        if (!combat.inCombat(player.getUniqueId())) return true;
        player.sendMessage(com.github.saku0817.combatcoresystems.util.ItemText.parse(definitions.snapshot().config("messages.yml")
                .getString("rebirth-failure.combat", "<red>戦闘中は新生回帰できません。</red>")));
        return false;
    }
    private void back(Inventory inv) { inv.setItem(inv.getSize() - 1, item("ARROW", gui("navigation.back", "<yellow>戻る</yellow>"), List.of())); }
    private boolean rarityCategory(String category) { return Set.of("weapons", "equipment", "divine_hearts").contains(category); }
    private int entryRarity(String category, String id) {
        return switch (category) {
            case "weapons" -> definitions.snapshot().weapons().get(id).rarity();
            case "equipment" -> definitions.snapshot().equipment().get(id).rarity();
            case "divine_hearts" -> definitions.snapshot().config("divine_hearts.yml").getInt("divine-hearts." + id + ".rarity", 5);
            default -> 0;
        };
    }
    private void openRarity(Player player, String category) {
        Inventory inv = inventory(player, Screen.ENCYCLOPEDIA_RARITY, gui("encyclopedia.rarity-title", "<gold>レア度を選択</gold>"), 54, category, 0, "");
        Set<Integer> rarities = new TreeSet<>();
        if (category.equals("weapons")) definitions.snapshot().weapons().values().forEach(value -> rarities.add(value.rarity()));
        else if (category.equals("equipment")) definitions.snapshot().equipment().values().forEach(value -> rarities.add(value.rarity()));
        else {
            var root = definitions.snapshot().config("divine_hearts.yml").getConfigurationSection("divine-hearts");
            if (root != null) root.getKeys(false).forEach(id -> rarities.add(root.getInt(id + ".rarity", 5)));
        }
        int index = 10;
        for (int rarity : rarities) {
            if (index >= 30) break;
            inv.setItem(index, item("NETHER_STAR", items.rarityLine(rarity), List.of()));
            ((Holder) inv.getHolder()).actions.put(index++, Integer.toString(rarity));
        }
        inv.setItem(31, item("CHEST", gui("encyclopedia.all-rarities", "<white>全レア度</white>"), List.of()));
        ((Holder) inv.getHolder()).actions.put(31, "0");
        back(inv); player.openInventory(inv);
    }
    private String mobIcon(String category, String id) {
        boolean boss = category.equals("bosses");
        String root = boss ? "bosses" : definitions.snapshot().vanillaMobs().containsKey(id) ? "vanilla-mobs" : "mobs";
        return definitions.snapshot().config(boss ? "bosses.yml" : "mobs.yml").getString(root + "." + id + ".material", boss ? "DRAGON_HEAD" : "ZOMBIE_HEAD");
    }
    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        rarityFilters.remove(event.getPlayer().getUniqueId()); awaitingSearch.remove(event.getPlayer().getUniqueId()); enhancementSelection.remove(event.getPlayer().getUniqueId());
    }
    private String gui(String path, String fallback) { return definitions.snapshot().config("gui.yml").getString(path, fallback); }
    private ItemStack materialIcon(EnhancementService.MaterialInfo material, List<String> extraLore) {
        ItemStack stack = items.create(material.id(), 1).orElse(item(material.material(), material.name(), List.of()));
        ItemMeta meta = stack.getItemMeta(); List<Component> lore = new ArrayList<>(Optional.ofNullable(meta.lore()).orElse(List.of())); lore.addAll(extraLore.stream().map(com.github.saku0817.combatcoresystems.util.ItemText::parse).toList()); meta.lore(lore); stack.setItemMeta(meta); return stack;
    }
    private ItemStack findInstance(Player player, String instanceId) {
        for (ItemStack stack : player.getInventory().getContents()) { ItemInstance value = items.instance(stack).orElse(null); if (value != null && value.getInstanceId().equals(instanceId)) return stack; }
        ItemStack stored = equipment.storedResonance(players.require(player));
        if (items.instance(stored).map(value -> value.getInstanceId().equals(instanceId)).orElse(false)) return stored;
        return null;
    }
    private long physicalCount(Player player, String id) { long count = 0; for (ItemStack stack : player.getInventory().getContents()) if (id.equals(items.id(stack).orElse(""))) count += stack.getAmount(); return count; }
    private ItemStack item(String materialName, String name, List<String> lore) {
        Material material = Material.matchMaterial(materialName); ItemStack item = new ItemStack(material == null ? Material.BARRIER : material); ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(definitions.snapshot().config("gui.yml").getBoolean("enchantment-glint", false));
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        meta.itemName(com.github.saku0817.combatcoresystems.util.ItemText.parse(name)); meta.lore(lore.stream().map(com.github.saku0817.combatcoresystems.util.ItemText::parse).toList()); item.setItemMeta(meta); return item;
    }

    private ItemStack abilityIcon(WeaponDefinition.SkillDefinition ability, String key, String label) {
        return item("ENCHANTED_BOOK", gui("encyclopedia." + key, "<aqua>" + label + "</aqua>") + " — "
                + (ability == null ? gui("encyclopedia.undefined", "未設定") : ability.name()),
                ability == null ? List.of() : items.abilityDescription(ability));
    }

    @EventHandler
    public void onOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
        for (int slot = 0; slot < event.getInventory().getSize(); slot++) {
            ItemStack stack = event.getInventory().getItem(slot);
            if (stack == null || !(stack.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta meta)) continue;
            UUID owner = event.getPlayer().getUniqueId();
            if (holder.screen == Screen.PARTY || holder.screen == Screen.PARTY_ACTION) {
                String member = holder.actions.get(slot);
                if (member != null) try { owner = UUID.fromString(member); } catch (IllegalArgumentException ignored) { }
            }
            Component name = meta.itemName();
            Player onlineOwner = Bukkit.getPlayer(owner);
            if (onlineOwner != null) meta.setPlayerProfile(onlineOwner.getPlayerProfile());
            else meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            if (name != null) { meta.itemName(name); meta.displayName(name); }
            stack.setItemMeta(meta);
        }
    }
    private Map<EquipmentSlot, Integer> equipmentSlots() { return Map.of(EquipmentSlot.HEAD, 19, EquipmentSlot.CHEST, 21, EquipmentSlot.LEGS, 23, EquipmentSlot.FEET, 25, EquipmentSlot.RESONANCE, 30, EquipmentSlot.DIVINE_HEART, 32); }
    private long round(double value) { return Math.round(value); } private String percent(double value) { return String.format(Locale.ROOT, "%.1f%%", value * 100); } private String onOff(boolean value) { return value ? "ON" : "OFF"; }
    private enum Screen { MAIN, STATS, EQUIPMENT, EQUIPMENT_LIST, SKILL_TREE, REBIRTH, REBIRTH_CONFIRM, PARTY, ENHANCEMENT,
        ENHANCE_PLAYER, ENHANCE_WEAPON_LIST, ENHANCE_WEAPON, ENHANCE_EQUIPMENT_LIST, ENHANCE_EQUIPMENT, ENHANCE_CONVERSION,
        PARTY_ACTION, CONVERSION_DETAIL, SETTINGS, ENCYCLOPEDIA, ENCYCLOPEDIA_RARITY, ENCYCLOPEDIA_DETAIL, LIMIT_BREAK_SELECT, LIMIT_BREAK_CONFIRM }
    private static final class Holder implements InventoryHolder {
        final Map<Integer, String> actions = new HashMap<>();
        final Screen screen; final UUID player; final String context; final int page; final String query; Inventory inventory;
        Holder(Screen screen, UUID player, String context, int page, String query) { this.screen = screen; this.player = player; this.context = context; this.page = page; this.query = query; }
        @Override public Inventory getInventory() { return inventory; }
    }
    private record SearchState(String category, int page) {}
    private record Entry(String id, String name, String material, List<String> lore) {}
}
