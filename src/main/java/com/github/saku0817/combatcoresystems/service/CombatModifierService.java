package com.github.saku0817.combatcoresystems.service;

import com.github.saku0817.combatcoresystems.config.DefinitionRegistry;
import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.entity.LivingEntity;
import java.util.*;
import static com.github.saku0817.combatcoresystems.model.trigger.TriggerDefinition.*;

/** Snapshot-cached rules; active buff ownership and reapplication order come from BuffService. */
public final class CombatModifierService {
    private final DefinitionRegistry definitions;
    private final BuffService buffs;
    private DefinitionRegistry.Snapshot snapshot;
    private final Map<String,List<CombatModifiers.Rule>> compiled=new HashMap<>();
    private record Applied(CombatModifiers.Rule rule,int stacks) {}
    public CombatModifierService(DefinitionRegistry definitions,BuffService buffs) {this.definitions=definitions;this.buffs=buffs;}
    public static List<CombatModifiers.Rule> compile(Map<String,Object> config) {
        List<CombatModifiers.Rule> rules=new ArrayList<>(CombatModifiers.parse(child(config,"combat-modifiers")));
        rules.addAll(CombatModifiers.parse(child(config,"context-modifiers")));return List.copyOf(rules);
    }
    private List<CombatModifiers.Rule> rules(String id) {
        if(snapshot!=definitions.snapshot()){snapshot=definitions.snapshot();compiled.clear();}
        return compiled.computeIfAbsent(id,key->{
            var raw=snapshot.config("buffs.yml").getConfigurationSection("buffs."+id);
            return raw==null?List.of():compile(map(raw));
        });
    }
    private void apply(LivingEntity entity,CombatModifiers output,EventContext context) {
        if(entity==null)return;
        List<Applied> all=new ArrayList<>();
        for(var effect:buffs.effects(entity)) {
            if(!effect.isPermanent()&&effect.getRemainingMillis()<=0)continue;
            for(var rule:rules(effect.getId()))all.add(new Applied(rule,effect.getStacks()));
        }
        all.sort(Comparator.comparingInt(a->a.rule().priority()));
        for(Applied entry:all)output.apply(List.of(entry.rule()),context,entry.stacks());
    }
    public void prepare(EventContext context) {
        apply(context.source,context.sourceCombat,context);
        context.attribute=context.sourceCombat.element(context.attribute);
        apply(context.target,context.targetCombat,context);
    }
}
