package com.github.saku0817.combatcoresystems.config;

import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TriggerCatalogTest {
    private Map<String,YamlConfiguration> files(String weapon) throws Exception {
        var result=new HashMap<String,YamlConfiguration>();
        for (String file : List.of("weapons.yml","equipment.yml","sets.yml","divine_hearts.yml")) result.put(file,new YamlConfiguration());
        result.get("weapons.yml").loadFromString(weapon); return result;
    }
    @Test void cumulativeStagesReplaceActionListsAndMergeMaps() throws Exception {
        var files=files("""
            weapons:
              bow:
                talent:
                  target-stack-policy: {stack-id: aim, max-targets: 1, overflow: REMOVE_OLDEST}
                  triggers:
                    hit:
                      event: NORMAL_ATTACK
                      actions: [{type: ADD_STACK, id: aim, scope: TARGET, max: 5}]
                limit-breaks:
                  2:
                    talent:
                      target-stack-policy: {max-targets: 3}
                  4:
                    talent:
                      triggers:
                        hit:
                          actions: [{type: APPLY_EFFECT, effect: ready}]
            """);
        var errors=new ArrayList<String>(); var catalog=TriggerCatalog.compile(files,Set.of("ready"),errors);
        assertTrue(errors.isEmpty(),errors.toString());
        var base=catalog.get("weapon:bow:0").stream().filter(s -> s.placement().equals("talent")).findFirst().orElseThrow();
        var stage=catalog.get("weapon:bow:4").stream().filter(s -> s.placement().equals("talent")).findFirst().orElseThrow();
        assertEquals(1,TriggerDefinition.child(base.options(),"target-stack-policy").get("max-targets"));
        assertEquals(3,TriggerDefinition.child(stage.options(),"target-stack-policy").get("max-targets"));
        assertEquals("aim",TriggerDefinition.child(stage.options(),"target-stack-policy").get("stack-id"));
        assertEquals(TriggerEvent.NORMAL_ATTACK,stage.triggers().getFirst().event());
        assertEquals("APPLY_EFFECT",stage.triggers().getFirst().actions().getFirst().get("type"));
    }
    @Test void allEventNamesCompile() throws Exception {
        for (var event : TriggerEvent.values()) {
            var yaml=new YamlConfiguration(); yaml.loadFromString("triggers:\n  test:\n    event: "+event+"\n    actions: [{type: APPLY_EFFECT, effect: ready}]\n");
            assertEquals(event,TriggerDefinition.parse(yaml.getConfigurationSection("triggers"),Set.of("ready")).getFirst().event());
        }
    }
    @Test void rejectsUnknownEnumsAndInvalidReferences() throws Exception {
        for (String action : List.of("{type: NOPE}","{type: ADD_STACK, id: x, scope: NOPE}",
                "{type: DAMAGE, attribute: NOPE}","{type: CREATE_FIELD, id: x, area: {shape: NOPE}}",
                "{type: CREATE_FIELD, id: x, ally-effects: [missing]}","{type: MODIFY_EVENT_STATS, modifiers: {multiply: {CRIT_RATE: 1}}}",
                "{type: MODIFY_EVENT_STATS, modifiers: {override: {UNKNOWN: 1}}}","{type: HEAL, duration: -1}",
                "{type: CREATE_FIELD, id: x, area: {width: -1}}")) {
            var files=files("weapons:\n  test:\n    triggers:\n      event:\n        event: HIT\n        actions: ["+action+"]\n");
            var errors=new ArrayList<String>(); TriggerCatalog.compile(files,Set.of(),errors); assertFalse(errors.isEmpty(),action);
        }
    }
}
