package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** Compilation is performed during candidate validation, not on each event. */
public final class TriggerCatalog {
    public record Source(String definition, String placement, List<TriggerDefinition> triggers,
                         Map<String,Object> options) {}
    private TriggerCatalog() {}
    public static Map<String,List<Source>> compile(Map<String,YamlConfiguration> files, Set<String> buffs, List<String> errors) {
        Map<String,List<Source>> out = new LinkedHashMap<>();
        var weapons = files.get("weapons.yml").getConfigurationSection("weapons");
        if (weapons != null) for (String id : weapons.getKeys(false)) {
            var raw = weapons.getConfigurationSection(id);
            if (raw == null) continue;
            var effective = new YamlConfiguration(); DefinitionRegistry.overlay(effective,raw);
            effective.set("limit-breaks",null);
            for (int stage = 0; stage <= 5; stage++) {
                var patch = raw.getConfigurationSection("limit-breaks." + stage);
                if (patch != null) DefinitionRegistry.overlay(effective,patch);
                List<Source> sources = new ArrayList<>();
                for (String placement : List.of("weapon","talent","skill","ultimate")) {
                    var section = placement.equals("weapon") ? effective : effective.getConfigurationSection(placement);
                    compileSource(sources,"weapon:" + id,placement,section,buffs,errors);
                }
                out.put("weapon:" + id + ":" + stage,List.copyOf(sources));
            }
        }
        for (String root : List.of("sets","equipment","divine-hearts")) {
            String file = root.equals("divine-hearts") ? "divine_hearts.yml" : root + ".yml";
            var section = files.get(file).getConfigurationSection(root);
            if (section == null) continue;
            for (String id : section.getKeys(false)) {
                List<Source> sources = new ArrayList<>();
                if (root.equals("sets")) {
                    for (String tier : List.of("two-piece","four-piece"))
                        compileSource(sources,"set:" + id,tier,section.getConfigurationSection(id + "." + tier),buffs,errors);
                } else compileSource(sources,root + ":" + id,"root",section.getConfigurationSection(id),buffs,errors);
                out.put(root + ":" + id,List.copyOf(sources));
            }
        }
        return Map.copyOf(out);
    }
    private static void compileSource(List<Source> out,String definition,String placement,ConfigurationSection section,Set<String> buffs,List<String> errors) {
        if (section == null) return;
        try {
            var options = TriggerDefinition.map(section);
            var triggers = TriggerDefinition.parse(section.getConfigurationSection("triggers"),buffs);
            TriggerDefinition.policy(TriggerDefinition.child(options,"target-stack-policy"));
            if (options.containsKey("area")) TriggerDefinition.area(TriggerDefinition.child(options,"area"));
            TriggerDefinition.actions(options,buffs);
            if (placement.equals("skill") || placement.equals("ultimate")) TriggerDefinition.validateConditions(TriggerDefinition.child(options,"conditions"),buffs);
            if (!triggers.isEmpty() || options.containsKey("actions")) out.add(new Source(definition,placement,triggers,options));
        } catch (IllegalArgumentException ex) { errors.add(definition + "." + placement + ": " + ex.getMessage()); }
    }
}
