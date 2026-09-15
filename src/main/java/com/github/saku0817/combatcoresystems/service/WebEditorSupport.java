package com.github.saku0817.combatcoresystems.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** YAML-only operations: independent of live Bukkit entities and the server thread. */
final class WebEditorSupport {
    static final Map<String, String> ROOTS = Map.ofEntries(
            Map.entry("weapons.yml", "weapons"), Map.entry("equipment.yml", "equipment"),
            Map.entry("sets.yml", "sets"), Map.entry("mobs.yml", "mobs"), Map.entry("bosses.yml", "bosses"),
            Map.entry("buffs.yml", "buffs"), Map.entry("reactions.yml", "reactions"), Map.entry("divine_hearts.yml", "divine-hearts"));

    static Map<String, Object> tree(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) values.put(key,
                section.isConfigurationSection(key) ? tree(section.getConfigurationSection(key)) : section.get(key));
        return values;
    }

    static void addEntry(YamlConfiguration yaml, String file, String id, String requestedRoot) throws Exception {
        if (id == null || !id.matches("[a-zA-Z0-9_-]{1,64}")) throw new IllegalArgumentException("IDは英数字・_・-で1～64文字にしてください。");
        String root = ROOTS.get(file);
        if (file.equals("mobs.yml") && "vanilla-mobs".equals(requestedRoot)) root = requestedRoot;
        if (root == null) throw new IllegalArgumentException("この設定ファイルは新規定義の追加に対応していません。");
        if (yaml.contains(root + "." + id)) throw new IllegalArgumentException("同じIDが既に存在します。");
        String content = switch (file) {
            case "weapons.yml" -> "name: '<white>新しい武器</white>'\nmaterial: IRON_SWORD\ncategory: UNCATEGORIZED\nrarity: 3\nbase-atk: {level-1: 10, level-100: 100}\nnormal-attack: {attribute: PHYSICAL}\nlore: []\n";
            case "equipment.yml" -> "name: '<white>新しい装備</white>'\nmaterial: IRON_CHESTPLATE\nslot: CHEST\nrarity: 3\nmax-level: 9\nmain-stat: {type: CRIT_RATE, level-1: 0.05, max-level: 0.15}\nsubstats: [ATK_PERCENT, HP_FLAT]\nlore: []\n";
            case "sets.yml" -> "name: 新しいシリーズ\ntwo-piece:\n  description: 攻撃力+10%\n  modifiers: {ATK_PERCENT: 0.10}\nfour-piece:\n  description: 会心ダメージ+20%\n  modifiers: {CRIT_DAMAGE: 0.20}\n";
            case "mobs.yml", "bosses.yml" -> "name: '<white>新しいMob</white>'\nentity-type: ZOMBIE\nlevel: {min: 1, max: 1}\nstats:\n  hp: {min: 50000, max: 50000}\n  atk: {min: 10, max: 10}\n  def: {min: 0, max: 0}\ncustom-exp: 10\ndrop-custom-exp: true\nnative-attribute: PHYSICAL\n";
            case "buffs.yml" -> "name: 新しいバフ\nkind: BUFF\ntarget: SELF\nduration: 10\nmax-stacks: 1\nreapply: REFRESH\nmodifiers:\n  flat: {ATK_PERCENT: 0.10}\n";
            case "reactions.yml" -> "name: 新しい反応\nattributes: [FIRE, WATER]\ncooldown: 2.0\nradius: 0\ndamage-components: {FIRE: 0.5, WATER: 0.5}\n";
            case "divine_hearts.yml" -> "name: '<red>新しい神心</red>'\nmaterial: NETHER_STAR\nrarity: 5\nmodifiers: {FIRE_DAMAGE: 0.50}\ntalents:\n  blessing:\n    name: '<red>太陽の祝福</red>'\n    description: ['<white>炎属性ダメージ+50%</white>']\nlore: []\n";
            default -> throw new IllegalArgumentException("未対応のファイルです。");
        };
        YamlConfiguration entry = new YamlConfiguration(); entry.loadFromString(content);
        yaml.createSection(root + "." + id, tree(entry));
    }
}
