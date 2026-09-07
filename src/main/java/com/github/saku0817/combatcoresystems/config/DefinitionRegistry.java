package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public final class DefinitionRegistry {
    public static final List<String> FILES = List.of(
            "config.yml", "messages.yml", "storage.yml", "levels.yml", "reactions.yml", "buffs.yml",
            "sets.yml", "gui.yml", "weapons.yml", "equipment.yml", "divine_hearts.yml", "skill_trees.yml",
            "mobs.yml", "bosses.yml", "regions.yml", "spawns.yml", "encyclopedia.yml"
    );

    private final JavaPlugin plugin;
    private final AtomicReference<Snapshot> current = new AtomicReference<>();

    public DefinitionRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void createDefaults() {
        for (String file : FILES) {
            if (!new File(plugin.getDataFolder(), file).exists()) plugin.saveResource(file, false);
        }
    }

    public boolean loadInitial() {
        createDefaults();
        LoadResult result = loadCandidate();
        logIssues(result);
        if (!result.fatalErrors().isEmpty()) return false;
        current.set(result.snapshot());
        return true;
    }

    public boolean reloadSafely() {
        LoadResult result = loadCandidate();
        logIssues(result);
        if (!result.errors().isEmpty() || !result.fatalErrors().isEmpty()) return false;
        current.set(result.snapshot());
        return true;
    }

    public Snapshot snapshot() {
        Snapshot snapshot = current.get();
        if (snapshot == null) throw new IllegalStateException("Definitions are not loaded");
        return snapshot;
    }

    private LoadResult loadCandidate() {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> fatal = new ArrayList<>();
        Map<String, YamlConfiguration> yaml = new LinkedHashMap<>();
        for (String name : FILES) {
            File file = new File(plugin.getDataFolder(), name);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (config.getKeys(false).isEmpty() && file.length() > 0) {
                if (name.equals("config.yml") || name.equals("storage.yml")) fatal.add(name + " could not be parsed");
                else errors.add(name + " could not be parsed");
            }
            int version = config.getInt("data-version", -1);
            if (version > PlayerData.DATA_VERSION) fatal.add(name + " has unsupported data-version " + version);
            if (version < 1) errors.add(name + " is missing data-version");
            yaml.put(name, config);
        }

        Map<String, ReactionDefinition> reactions = parseReactions(yaml.get("reactions.yml"), errors);
        Map<String, WeaponDefinition> weapons = parseWeapons(yaml.get("weapons.yml"), errors, warnings);
        Map<String, EquipmentDefinition> equipment = parseEquipment(yaml.get("equipment.yml"), errors, warnings);
        Map<String, MobDefinition> mobs = parseMobs(yaml.get("mobs.yml"), "mobs", false, false, errors);
        Map<String, MobDefinition> vanillaMobs = parseMobs(yaml.get("mobs.yml"), "vanilla-mobs", false, true, errors);
        Map<String, MobDefinition> bosses = parseMobs(yaml.get("bosses.yml"), "bosses", true, false, errors);
        Map<String, BuffDefinition> buffs = parseBuffs(yaml.get("buffs.yml"), errors);
        Map<String, RegionDefinition> regions = parseRegions(yaml.get("regions.yml"), errors);

        for (EquipmentDefinition definition : equipment.values()) {
            if (!definition.setId().isBlank() && !yaml.get("sets.yml").contains("sets." + definition.setId())) {
                errors.add("equipment " + definition.id() + " references missing set " + definition.setId());
            }
        }

        Snapshot snapshot = new Snapshot(Map.copyOf(yaml), reactions, weapons, equipment, buffs, mobs, vanillaMobs, bosses, regions);
        return new LoadResult(snapshot, warnings, errors, fatal);
    }

    private Map<String, ReactionDefinition> parseReactions(YamlConfiguration yaml, List<String> errors) {
        Map<String, ReactionDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("reactions");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            List<String> rawElements = list(section, "attributes", "elements");
            if (rawElements.size() != 2) { errors.add("reaction " + id + " must have exactly two attributes"); continue; }
            Optional<Element> first = Element.parse(rawElements.get(0));
            Optional<Element> second = Element.parse(rawElements.get(1));
            if (first.isEmpty() || second.isEmpty() || first.get() == Element.PHYSICAL || second.get() == Element.PHYSICAL) {
                errors.add("reaction " + id + " has an invalid attribute"); continue;
            }
            EnumMap<Element, Double> components = new EnumMap<>(Element.class);
            ConfigurationSection componentSection = section(section, "damage-components", "components");
            if (componentSection != null) {
                for (String key : componentSection.getKeys(false)) Element.parse(key)
                        .ifPresent(element -> components.put(element, componentSection.getDouble(key)));
            }
            ConfigurationSection down = section.getConfigurationSection("resistance-down");
            Element downElement = down == null ? null : Element.parse(value(down, "attribute", "element")).orElse(null);
            result.put(id, new ReactionDefinition(id, section.getString("name", id), first.get(), second.get(),
                    Math.max(0, section.getDouble("radius", 0)), Math.max(0, section.getDouble("cooldown", 0)),
                    Math.max(1, section.getInt("hits", 1)), Element.parse(value(section, "multi-hit-attribute", "multi-hit-element")).orElse(null),
                    Math.max(0, section.getDouble("levitation", 0)), components,
                    downElement, down == null ? 0 : down.getDouble("amount"), down == null ? 0 : down.getDouble("duration")));
        }
        return Map.copyOf(result);
    }

    private Map<String, WeaponDefinition> parseWeapons(YamlConfiguration yaml, List<String> errors, List<String> warnings) {
        Map<String, WeaponDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("weapons");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Material material = Material.matchMaterial(s.getString("material", ""));
            if (material == null) { errors.add("weapon " + id + " has invalid material"); continue; }
            WeaponDefinition.Category category;
            try { category = WeaponDefinition.Category.valueOf(s.getString("category", "").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { errors.add("weapon " + id + " has invalid category"); continue; }
            int rarity = s.getInt("rarity", 1);
            if (rarity < 1 || rarity > 5) { errors.add("weapon " + id + " rarity must be 1..5"); continue; }
            Element element = Element.parse(value(s, "attribute-bonus.type", "element-bonus.type")).orElse(Element.PHYSICAL);
            int min = Math.max(1, s.getInt("equip-level.min", 1));
            int max = Math.min(100, s.getInt("equip-level.max", 100));
            if (min > max) { errors.add("weapon " + id + " equip level range is invalid"); continue; }
            result.put(id, new WeaponDefinition(id, s.getString("name", id), material.name(), category, rarity,
                    s.getDouble("base-atk.level-1"), s.getDouble("base-atk.level-100"), element,
                    decimal(s, "attribute-bonus.value", "element-bonus.value"), min, max,
                    s.contains("custom-model-data") ? s.getInt("custom-model-data") : null, s.getStringList("lore"),
                    parseSkill(id + ":skill", s.getConfigurationSection("skill"), warnings),
                    parseSkill(id + ":ultimate", s.getConfigurationSection("ultimate"), warnings), readLimitBreaks(s.getConfigurationSection("limit-breaks"))));
        }
        return Map.copyOf(result);
    }

    private WeaponDefinition.SkillDefinition parseSkill(String fallbackId, ConfigurationSection s, List<String> warnings) {
        if (s == null) return new WeaponDefinition.SkillDefinition(fallbackId, fallbackId, ReferenceStat.ATK, 1, Element.PHYSICAL, 0, 1, 0, "ENEMY", Map.of());
        ReferenceStat stat;
        try { stat = ReferenceStat.valueOf(s.getString("reference", "ATK").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { warnings.add(fallbackId + " uses ATK because reference is invalid"); stat = ReferenceStat.ATK; }
        return new WeaponDefinition.SkillDefinition(s.getString("id", fallbackId), s.getString("name", fallbackId), stat,
                s.getDouble("multiplier", 1), Element.parse(value(s, "attribute", "element")).orElse(Element.PHYSICAL),
                Math.max(0, s.getDouble("cooldown", 0)), Math.max(1, s.getInt("charges", 1)),
                Math.max(0, s.getDouble("radius", 0)), s.getString("target", "ENEMY"),
                s.getConfigurationSection("conditions") == null ? Map.of() : Map.copyOf(s.getConfigurationSection("conditions").getValues(false)));
    }

    private Map<Integer, Map<String, Object>> readLimitBreaks(ConfigurationSection root) {
        if (root == null) return Map.of();
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (String key : root.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                ConfigurationSection section = root.getConfigurationSection(key);
                if (level >= 0 && level <= 5 && section != null) result.put(level, Map.copyOf(section.getValues(true)));
            } catch (NumberFormatException ignored) {}
        }
        return Map.copyOf(result);
    }

    private Map<String, EquipmentDefinition> parseEquipment(YamlConfiguration yaml, List<String> errors, List<String> warnings) {
        Map<String, EquipmentDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("equipment");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            Material material = Material.matchMaterial(s.getString("material", ""));
            if (material == null) { errors.add("equipment " + id + " has invalid material"); continue; }
            EquipmentSlot slot;
            StatKey main;
            try {
                slot = EquipmentSlot.valueOf(s.getString("slot", "").toUpperCase(Locale.ROOT));
                main = StatKey.valueOf(s.getString("main-stat.type", "").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) { errors.add("equipment " + id + " has invalid slot or main stat"); continue; }
            if (!mainAllowed(slot, main)) { errors.add("equipment " + id + " main stat is not allowed for its slot"); continue; }
            int rarity = s.getInt("rarity");
            int expectedMax = rarity == 3 ? 9 : rarity == 4 ? 12 : rarity == 5 ? 15 : -1;
            if (expectedMax < 0) { errors.add("equipment " + id + " rarity must be 3..5"); continue; }
            int maxLevel = s.getInt("max-level", expectedMax);
            if (maxLevel != expectedMax) warnings.add("equipment " + id + " max-level differs from rarity standard");
            List<StatKey> candidates = new ArrayList<>();
            for (String raw : s.getStringList("substats")) {
                try { StatKey key = StatKey.valueOf(raw.toUpperCase(Locale.ROOT)); if (!candidates.contains(key)) candidates.add(key); }
                catch (IllegalArgumentException ex) { errors.add("equipment " + id + " contains invalid substat " + raw); }
            }
            result.put(id, new EquipmentDefinition(id, s.getString("name", id), material.name(), slot, rarity, maxLevel,
                    main, s.getDouble("main-stat.level-1"), s.getDouble("main-stat.max-level"), List.copyOf(candidates),
                    s.getString("set", ""), s.contains("custom-model-data") ? s.getInt("custom-model-data") : null,
                    s.getStringList("lore")));
        }
        return Map.copyOf(result);
    }

    private boolean mainAllowed(EquipmentSlot slot, StatKey key) {
        return switch (slot) {
            case HEAD -> Set.of(StatKey.HP_FLAT, StatKey.HP_PERCENT, StatKey.DEF_FLAT, StatKey.DEF_PERCENT).contains(key);
            case CHEST -> Set.of(StatKey.CRIT_RATE, StatKey.CRIT_DAMAGE).contains(key);
            case LEGS -> Set.of(StatKey.ATK_FLAT, StatKey.ATK_PERCENT).contains(key);
            case FEET -> Set.of(StatKey.ATK_FLAT, StatKey.ATK_PERCENT, StatKey.CRIT_RATE, StatKey.CRIT_DAMAGE,
                    StatKey.HP_FLAT, StatKey.HP_PERCENT, StatKey.DEF_FLAT, StatKey.DEF_PERCENT).contains(key);
            case RESONANCE -> key.name().endsWith("_DAMAGE") || key.name().endsWith("_RESISTANCE");
            default -> false;
        };
    }

    private Map<String, MobDefinition> parseMobs(YamlConfiguration yaml, String rootName, boolean boss, boolean vanilla, List<String> errors) {
        Map<String, MobDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection(rootName);
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try { EntityType.valueOf(s.getString("entity-type", "").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ex) { errors.add((boss ? "boss " : "mob ") + id + " has invalid entity type"); continue; }
            int min = Math.max(1, s.getInt("level.min", 1));
            int max = Math.max(min, s.getInt("level.max", min));
            Set<Element> immunity = EnumSet.noneOf(Element.class);
            for (String raw : s.getStringList("immunity")) Element.parse(raw).ifPresent(immunity::add);
            EnumMap<Element, Double> resistance = new EnumMap<>(Element.class);
            ConfigurationSection rs = s.getConfigurationSection("resistance");
            if (rs != null) for (String key : rs.getKeys(false)) Element.parse(key).ifPresent(e -> resistance.put(e, rs.getDouble(key)));
            result.put(id, new MobDefinition(id, s.getString("name", id), s.getString("entity-type"), boss, min, max,
                    s.getDouble("stats.hp.min", 20), s.getDouble("stats.hp.max", 20), s.getDouble("stats.atk.min", 2),
                    s.getDouble("stats.atk.max", 2), s.getDouble("stats.def.min", 0), s.getDouble("stats.def.max", 0),
                    Element.parse(value(s, "native-attribute", "native-element")).orElse(Element.PHYSICAL), Set.copyOf(immunity), Map.copyOf(resistance),
                    Math.max(0, s.contains("custom-exp") ? s.getLong("custom-exp") : s.getLong("exp", 0)),
                    s.getBoolean("drop-custom-exp", true), s.getBoolean("show-level", true), vanilla));
        }
        return Map.copyOf(result);
    }

    private Map<String, BuffDefinition> parseBuffs(YamlConfiguration yaml, List<String> errors) {
        Map<String, BuffDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("buffs");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) continue;
            try {
                BuffDefinition.Kind kind = BuffDefinition.Kind.valueOf(s.getString("kind", "BUFF").toUpperCase(Locale.ROOT));
                BuffDefinition.Target target = BuffDefinition.Target.valueOf(s.getString("target", kind == BuffDefinition.Kind.BUFF ? "SELF" : "ENEMY").toUpperCase(Locale.ROOT));
                BuffDefinition.Reapply reapply = BuffDefinition.Reapply.valueOf(s.getString("reapply", "REFRESH").toUpperCase(Locale.ROOT));
                EnumMap<StatKey, Double> flat = parseStatMap(s.getConfigurationSection("modifiers.flat"), errors, "buff " + id);
                EnumMap<StatKey, Double> percent = parseStatMap(s.getConfigurationSection("modifiers.percent"), errors, "buff " + id);
                BuffDefinition.TickEffect tick = parseTickEffect(s.getConfigurationSection("tick-effect"), errors, id);
                result.put(id, new BuffDefinition(id, kind, target, Math.max(0, s.getDouble("duration", 0)),
                        s.getBoolean("permanent"), Math.max(1, s.getInt("max-stacks", 1)), reapply, Map.copyOf(flat), Map.copyOf(percent), tick));
            } catch (IllegalArgumentException ex) { errors.add("buff " + id + " contains an invalid enum value"); }
        }
        return Map.copyOf(result);
    }

    private EnumMap<StatKey, Double> parseStatMap(ConfigurationSection section, List<String> errors, String owner) {
        EnumMap<StatKey, Double> result = new EnumMap<>(StatKey.class);
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            try { result.put(StatKey.valueOf(key.toUpperCase(Locale.ROOT)), section.getDouble(key)); }
            catch (IllegalArgumentException ex) { errors.add(owner + " has invalid stat " + key); }
        }
        return result;
    }

    private BuffDefinition.TickEffect parseTickEffect(ConfigurationSection section, List<String> errors, String id) {
        if (section == null) return null;
        try {
            return new BuffDefinition.TickEffect(section.getBoolean("healing"), section.getBoolean("fixed"),
                    ReferenceStat.valueOf(section.getString("reference", "ATK").toUpperCase(Locale.ROOT)),
                    section.getDouble("multiplier", 1), Element.parse(value(section, "attribute", "element")).orElse(Element.PHYSICAL),
                    Math.max(0.05, section.getDouble("interval", 1)), section.getBoolean("critical"));
        } catch (IllegalArgumentException ex) {
            errors.add("buff " + id + " has invalid tick-effect");
            return null;
        }
    }

    private Map<String, RegionDefinition> parseRegions(YamlConfiguration yaml, List<String> errors) {
        Map<String, RegionDefinition> result = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("regions");
        if (root == null) return Map.of();
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null || s.getString("world") == null) { errors.add("region " + id + " is missing world"); continue; }
            int x1 = s.getInt("pos1.x"), z1 = s.getInt("pos1.z"), x2 = s.getInt("pos2.x"), z2 = s.getInt("pos2.z");
            boolean full = s.getBoolean("full-height", false);
            Integer y1 = full ? null : Math.min(s.getInt("pos1.y"), s.getInt("pos2.y"));
            Integer y2 = full ? null : Math.max(s.getInt("pos1.y"), s.getInt("pos2.y"));
            Map<String, String> flags = new LinkedHashMap<>();
            ConfigurationSection fs = s.getConfigurationSection("flags");
            if (fs != null) for (String key : fs.getKeys(false)) flags.put(key, fs.getString(key, ""));
            result.put(id, new RegionDefinition(id, s.getString("world"), Math.min(x1, x2), y1, Math.min(z1, z2),
                    Math.max(x1, x2), y2, Math.max(z1, z2), Map.copyOf(flags)));
        }
        return Map.copyOf(result);
    }

    private List<String> list(ConfigurationSection section, String preferred, String legacy) {
        return section.contains(preferred) ? section.getStringList(preferred) : section.getStringList(legacy);
    }
    private ConfigurationSection section(ConfigurationSection section, String preferred, String legacy) {
        ConfigurationSection result = section.getConfigurationSection(preferred);
        return result == null ? section.getConfigurationSection(legacy) : result;
    }
    private String value(ConfigurationSection section, String preferred, String legacy) {
        return section.contains(preferred) ? section.getString(preferred) : section.getString(legacy);
    }
    private double decimal(ConfigurationSection section, String preferred, String legacy) {
        return section.contains(preferred) ? section.getDouble(preferred) : section.getDouble(legacy);
    }

    private void logIssues(LoadResult result) {
        result.warnings().forEach(message -> plugin.getLogger().warning(message));
        result.errors().forEach(message -> plugin.getLogger().severe(message));
        result.fatalErrors().forEach(message -> plugin.getLogger().log(Level.SEVERE, message));
    }

    public record Snapshot(Map<String, YamlConfiguration> yaml, Map<String, ReactionDefinition> reactions,
                           Map<String, WeaponDefinition> weapons, Map<String, EquipmentDefinition> equipment,
                           Map<String, BuffDefinition> buffs, Map<String, MobDefinition> mobs, Map<String, MobDefinition> vanillaMobs,
                           Map<String, MobDefinition> bosses, Map<String, RegionDefinition> regions) {
        public YamlConfiguration config(String file) { return yaml.get(file); }
    }

    private record LoadResult(Snapshot snapshot, List<String> warnings, List<String> errors, List<String> fatalErrors) {}
}
