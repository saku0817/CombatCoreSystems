package com.github.saku0817.combatcoresystems.model;

import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TriggerFoundationTest {
    @Test void overrideIsFinalAndLastWins() {
        var modifier=new EventModifier(); modifier.add("percent",StatKey.CRIT_RATE,.2);
        assertEquals(.3,modifier.advanced(StatKey.CRIT_RATE,.1),1e-12);
        modifier.add("override",StatKey.CRIT_RATE,1); assertEquals(1,modifier.advanced(StatKey.CRIT_RATE,.1));
        modifier.add("override",StatKey.CRIT_RATE,.5); assertEquals(.5,modifier.advanced(StatKey.CRIT_RATE,.1));
    }
    @Test void chainGuardsReentryAndDepth() {
        var chain=new TriggerChain(); var owner=UUID.randomUUID();
        assertTrue(chain.enter(owner,"weapon:a","hit")); chain.leave(); assertFalse(chain.enter(owner,"weapon:a","hit"));
        for (int i=0;i<16;i++) assertTrue(chain.enter(owner,"weapon:b","t"+i));
        assertFalse(chain.enter(owner,"weapon:b","overflow"));
        for (int i=0;i<16;i++) chain.leave(); assertEquals(0,chain.depth());
    }
    @Test void legacyEffectsStillCompile() throws Exception {
        var yaml=new YamlConfiguration(); yaml.loadFromString("triggers:\n  old:\n    event: SKILL\n    target: OTHER\n    effects: [power]\n    cooldown-seconds: 15\n");
        var trigger=TriggerDefinition.parse(yaml.getConfigurationSection("triggers"),Set.of("power")).getFirst();
        assertEquals(15000,trigger.cooldown()); assertEquals("OTHER",trigger.actions().getFirst().get("target"));
    }
    @Test void rejectsInvalidNumbersSelectorsAndEmptyActions() {
        assertThrows(IllegalArgumentException.class,()->TriggerDefinition.actions(Map.of("actions",List.of()),Set.of()));
        for (Map<String,Object> action : List.<Map<String,Object>>of(
                Map.of("type","DAMAGE","def-ignore",1.1),Map.of("type","HEAL","target","NOT_REAL"),
                Map.of("type","ADD_STACK","id","a","max",0),Map.of("type","DAMAGE","multiplier",Double.NaN),
                Map.of("type","APPLY_EFFECT","effect","missing")))
            assertThrows(IllegalArgumentException.class,()->TriggerDefinition.actions(Map.of("actions",List.of(action)),Set.of()));
    }
    @Test void geometryIsOrientedAndBounded() {
        var box=new Area(Area.Shape.FORWARD_BOX,5,4,4,5,90,Area.Origin.SELF);
        assertTrue(box.contains(2,2,5)); assertFalse(box.contains(0,0,-1)); assertFalse(box.contains(3,0,2));
        var cone=new Area(Area.Shape.CONE,5,4,4,5,90,Area.Origin.SELF);
        assertTrue(cone.contains(1,0,1)); assertFalse(cone.contains(2,0,1));
    }
}
