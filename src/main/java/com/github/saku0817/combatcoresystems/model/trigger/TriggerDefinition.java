package com.github.saku0817.combatcoresystems.model.trigger;

import com.github.saku0817.combatcoresystems.model.Element;
import com.github.saku0817.combatcoresystems.model.StatKey;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Immutable validated configuration. Bukkit sections never escape the compilation step. */
public record TriggerDefinition(String id, TriggerEvent event, String target, Map<String,Object> conditions,
                                List<Map<String,Object>> actions, long cooldown, int maxActivations,
                                String activationScope, double hpPercent) {
    public static final Set<String> SELECTORS = Set.of("SELF", "SOURCE", "OTHER", "EVENT_TARGET", "ATTACKER", "VICTIM",
            "NEAREST_ENEMY", "NEAREST_ALLY", "LOWEST_HP_PARTY_MEMBER", "LOWEST_HP_PARTY_MEMBER_OR_SELF",
            "ALL_PARTY_MEMBERS", "ALL_PARTY_MEMBERS_AND_SELF", "ENTITIES_IN_AREA", "ALLIES_IN_AREA", "ENEMIES_IN_AREA");
    public static final Set<String> SCOPES = Set.of("SELF", "TARGET", "EVENT_TARGET", "ATTACKER", "VICTIM", "ALL_TARGETS");
    private static final Set<String> ACTIONS = Set.of("APPLY_EFFECT", "REMOVE_EFFECT", "ADD_STACK", "SET_STACK", "CLEAR_STACK",
            "CONSUME_STACK", "DAMAGE", "HEAL", "MODIFY_EVENT_STATS", "CREATE_FIELD", "REMOVE_FIELD", "APPLY_DYNAMIC_MODIFIER");
    private static final Set<String> CONDITIONS = Set.of("min-hp-percent", "max-hp-percent", "requires-combat", "requires-target",
            "min-distance", "max-distance", "damage-positive", "heal-positive", "overheal-positive", "normal-attack-only",
            "skill-only", "ultimate-only", "element", "critical", "buff-present", "buff-absent", "party-required",
            "target-is-self", "target-is-ally", "target-is-enemy", "inside-field", "outside-field", "stack", "context-value");
    public static Map<String,Object> map(Object value) {
        Map<?,?> raw;
        if (value instanceof ConfigurationSection section) raw = section.getValues(false);
        else if (value instanceof Map<?,?> m) raw = m;
        else throw new IllegalArgumentException("Expected map");
        Map<String,Object> out = new LinkedHashMap<>();
        raw.forEach((k,v) -> out.put(String.valueOf(k), freeze(v)));
        return Collections.unmodifiableMap(out);
    }
    private static Object freeze(Object value) {
        if (value instanceof Map<?,?> || value instanceof ConfigurationSection) return map(value);
        if (value instanceof List<?> list) return list.stream().map(TriggerDefinition::freeze).toList();
        if (value instanceof Number n && !Double.isFinite(n.doubleValue())) throw new IllegalArgumentException("Non-finite number");
        return value;
    }
    public static String text(Map<String,Object> m, String key, String fallback) {
        Object value = m.get(key); return value == null ? fallback : String.valueOf(value);
    }
    public static double number(Map<String,Object> m, String key, double fallback) {
        if (!m.containsKey(key)) return fallback;
        if (!(m.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue())) throw new IllegalArgumentException("Invalid number: " + key);
        return n.doubleValue();
    }
    public static double range(Map<String,Object> m, String key, double fallback, double min, double max) {
        double n = number(m,key,fallback);
        if (n < min || n > max) throw new IllegalArgumentException("Out of range: " + key);
        return n;
    }
    public static int integer(Map<String,Object> m, String key, int fallback, int min) {
        double n = range(m,key,fallback,min,Integer.MAX_VALUE);
        if (n != Math.rint(n)) throw new IllegalArgumentException("Expected integer: " + key);
        return (int)n;
    }
    public static Map<String,Object> child(Map<String,Object> m, String key) { return m.containsKey(key) ? map(m.get(key)) : Map.of(); }
    public static String choice(Map<String,Object> m, String key, String fallback, Set<String> choices) {
        String v = text(m,key,fallback).toUpperCase(Locale.ROOT);
        if (!choices.contains(v)) throw new IllegalArgumentException("Unknown " + key + ": " + v);
        return v;
    }
    public static List<String> strings(Map<String,Object> m, String key) {
        if (!m.containsKey(key)) return List.of();
        if (!(m.get(key) instanceof List<?> list) || list.isEmpty() || list.stream().anyMatch(v -> !(v instanceof String s) || s.isBlank()))
            throw new IllegalArgumentException("Expected nonempty string list: " + key);
        return list.stream().map(String::valueOf).toList();
    }
    public static List<TriggerDefinition> parse(ConfigurationSection section, Set<String> buffs) {
        if (section == null) return List.of();
        List<TriggerDefinition> out = new ArrayList<>();
        for (String id : section.getKeys(false)) {
            Map<String,Object> m = map(section.get(id));
            TriggerEvent event = TriggerEvent.valueOf(text(m,"event", "").toUpperCase(Locale.ROOT));
            String target = choice(m,"target","SELF",SELECTORS);
            Map<String,Object> conditions = new LinkedHashMap<>(child(m,"conditions")); validateConditions(conditions,buffs);
            if (m.containsKey("effects") && target.equals("OTHER")) conditions.put("requires-target",true);
            List<Map<String,Object>> actions = new ArrayList<>(actions(m,buffs));
            for (String effect : strings(m,"effects")) {
                effect(effect,buffs); actions.add(Map.of("type","APPLY_EFFECT","effect",effect,"target",target));
            }
            if (actions.isEmpty()) throw new IllegalArgumentException("Trigger has no actions: " + id);
            out.add(new TriggerDefinition(id,event,target,Map.copyOf(conditions),List.copyOf(actions),
                    Math.round(range(m,"cooldown-seconds",m.containsKey("actions") ? 0 : 1,0,86400)*1000),
                    integer(m,"max-activations",0,0),choice(m,"activation-scope","GLOBAL",Set.of("GLOBAL","COMBAT","LIFE")),
                    range(m,"hp-percent",.5,0,1)));
        }
        return List.copyOf(out);
    }
    public static void validateConditions(Map<String,Object> m, Set<String> buffs) {
        for (String key : m.keySet()) {
            if (!CONDITIONS.contains(key)) throw new IllegalArgumentException("Unknown condition: " + key);
            switch (key) {
                case "min-hp-percent", "max-hp-percent" -> range(m,key,0,0,1);
                case "min-distance", "max-distance" -> range(m,key,0,0,1024);
                case "element" -> attribute(text(m,key,""));
                case "buff-present", "buff-absent" -> effect(text(m,key,""),buffs);
                case "inside-field", "outside-field" -> required(m,key);
                case "stack", "context-value" -> {
                    var c = child(m,key); required(c,key.equals("stack") ? "id" : "key");
                    if (key.equals("stack")) choice(c,"scope","SELF",SCOPES);
                    if (c.containsKey("min")) number(c,"min",0);
                    if (c.containsKey("max")) number(c,"max",0);
                    if (number(c,"min",-Double.MAX_VALUE) > number(c,"max",Double.MAX_VALUE)) throw new IllegalArgumentException("Reversed condition bounds");
                }
                default -> { if (!(m.get(key) instanceof Boolean)) throw new IllegalArgumentException("Expected boolean: " + key); }
            }
        }
    }
    private static void required(Map<String,Object> m,String key) { if (text(m,key,"").isBlank()) throw new IllegalArgumentException("Missing " + key); }
    private static void effect(String id,Set<String> buffs) { if (!buffs.contains(id)) throw new IllegalArgumentException("Unknown buff: " + id); }
    private static void attribute(String name) { if (Element.parse(name).isEmpty()) throw new IllegalArgumentException("Unknown attribute: " + name); }
    public static Area area(Map<String,Object> m) {
        return new Area(Area.Shape.valueOf(text(m,"shape","SPHERE").toUpperCase(Locale.ROOT)),
                range(m,m.containsKey("range") ? "range" : "radius",5,0,128),
                range(m,"width",5,0,128),range(m,"height",5,0,128),range(m,"length",5,0,128),range(m,"angle",90,0,360),
                Area.Origin.valueOf(text(m,"origin","SELF").toUpperCase(Locale.ROOT)));
    }
    public static void modifiers(Map<String,Object> m) {
        if (m.isEmpty()) throw new IllegalArgumentException("Empty modifiers");
        for (var entry : m.entrySet()) {
            if (!Set.of("flat","percent","override").contains(entry.getKey())) throw new IllegalArgumentException("Unknown modifier mode: " + entry.getKey());
            var values = map(entry.getValue());
            for (String stat : values.keySet()) { StatKey.valueOf(stat); number(values,stat,0); }
        }
    }
    public static List<Map<String,Object>> actions(Map<String,Object> m, Set<String> buffs) {
        if (!m.containsKey("actions")) return List.of();
        if (!(m.get("actions") instanceof List<?> list) || list.isEmpty()) throw new IllegalArgumentException("Empty/invalid actions");
        List<Map<String,Object>> out = new ArrayList<>();
        for (Object raw : list) {
            Map<String,Object> a = map(raw);
            String type = choice(a,"type","",ACTIONS);
            choice(a,"target","SELF",SELECTORS); validateConditions(child(a,"conditions"),buffs);
            if (a.containsKey("area")) area(child(a,"area"));
            choice(a,"target-filter","ALL",Set.of("ALL","ALLY","ENEMY"));
            range(a,"duration",0,0,86400); range(a,"multiplier",1,0,1e9);
            switch (type) {
                case "APPLY_EFFECT", "REMOVE_EFFECT" -> effect(text(a,"effect",""),buffs);
                case "ADD_STACK", "SET_STACK", "CLEAR_STACK", "CONSUME_STACK" -> {
                    required(a,"id"); choice(a,"scope","SELF",SCOPES);
                    if (text(a,"scope","SELF").equalsIgnoreCase("ALL_TARGETS") && !type.equals("CLEAR_STACK")) throw new IllegalArgumentException("ALL_TARGETS action scope requires CLEAR_STACK");
                    integer(a,"amount",1,0); integer(a,"max",1,1);
                    choice(a,"reapply","REFRESH",Set.of("REFRESH","EXTEND","IGNORE"));
                }
                case "MODIFY_EVENT_STATS" -> modifiers(child(a,"modifiers"));
                case "DAMAGE", "HEAL" -> {
                    choice(a,"reference","ATK", type.equals("DAMAGE") ? Set.of("ATK","HP","DEF") :
                            Set.of("ATK","HP","DEF","FIXED","EVENT_DAMAGE","EVENT_HEAL","EVENT_EFFECTIVE_HEAL","EVENT_OVERHEAL"));
                    attribute(text(a,"attribute","PHYSICAL")); range(a,"def-ignore",0,0,1);
                    if (a.containsKey("components")) {
                        if (!(a.get("components") instanceof List<?> components) || components.isEmpty()) throw new IllegalArgumentException("Empty components");
                        for (Object component : components) {
                            var c = map(component); choice(c,"reference","ATK",Set.of("ATK","HP","DEF"));
                            range(c,"multiplier",1,0,1e9); attribute(text(c,"bonus-attribute","PHYSICAL"));
                        }
                    }
                }
                case "CREATE_FIELD" -> {
                    required(a,"id"); area(child(a,"area"));
                    choice(a,"effect-mode","WHILE_INSIDE",Set.of("ON_ENTER","REFRESH_WHILE_INSIDE","WHILE_INSIDE"));
                    for (String key : List.of("ally-effects","enemy-effects")) for (String id : strings(a,key)) effect(id,buffs);
                }
                case "REMOVE_FIELD" -> required(a,"id");
                case "APPLY_DYNAMIC_MODIFIER" -> {
                    StatKey.valueOf(text(a,"stat",""));
                    choice(a,"source","",Set.of("EVENT_DAMAGE","EVENT_HEAL","EVENT_EFFECTIVE_HEAL","EVENT_OVERHEAL",
                            "SOURCE_ATK","SOURCE_MAX_HP","SOURCE_DEF","TARGET_ATK","TARGET_MAX_HP","TARGET_DEF"));
                    choice(a,"reapply","REFRESH",Set.of("REFRESH","EXTEND","IGNORE"));
                }
            }
            Map<String,Object> normalized=new LinkedHashMap<>(a);
            for (String key : List.of("type","target","scope","reapply","reference","attribute","target-filter","effect-mode","source"))
                if (normalized.containsKey(key)) normalized.put(key,text(a,key,"").toUpperCase(Locale.ROOT));
            out.add(Collections.unmodifiableMap(normalized));
        }
        return List.copyOf(out);
    }
    public static void policy(Map<String,Object> m) {
        if (m.isEmpty()) return;
        required(m,"stack-id"); integer(m,"max-targets",0,0);
        choice(m,"overflow","REMOVE_OLDEST",Set.of("REMOVE_OLDEST","REMOVE_NEWEST","REMOVE_LOWEST_STACK","REJECT_NEW"));
    }
}
