package com.github.saku0817.combatcoresystems.model;

import com.github.saku0817.combatcoresystems.model.trigger.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatModifiersTest {
    @Test void categoryOverrideWinsWithinOneDefinitionRegardlessOfKeyOrder() {
        var config=new LinkedHashMap<String,Object>();
        config.put("dot",Map.of("critical-mode","ENABLED"));config.put("critical-mode","DISABLED");
        var rules=CombatModifiers.parse(config);
        assertEquals(List.of("","dot"),rules.stream().map(CombatModifiers.Rule::category).toList());
        var c=context(SourceKind.DOT);var m=new CombatModifiers();m.apply(rules,c,1);
        assertEquals(CombatModifiers.CriticalMode.ENABLED,m.criticalMode());
        m.apply(CombatModifiers.action(Map.of("critical",Map.of("mode","FORCED"))),c,1);
        assertEquals(CombatModifiers.CriticalMode.FORCED,m.criticalMode());
    }
    @Test void elementScopedReductionFollowsFinalEventOverride() {
        var c=context(SourceKind.SKILL);c.attribute=Element.FIRE;
        var modifiers=new CombatModifiers();
        modifiers.apply(CombatModifiers.parse(Map.of("scope",Map.of("element","WATER"),"damage-taken",percent(-.5))),c,1);
        assertEquals(1,modifiers.factor("damage-taken",c));
        c.attribute=Element.WATER;
        assertEquals(.5,modifiers.factor("damage-taken",c));
        c.attribute=Element.FIRE;
        assertEquals(1,modifiers.factor("damage-taken",c));
    }
    @Test void japaneseElementScopeMatchesTheValidatedElement() {
        var c=context(SourceKind.SKILL);c.attribute=Element.FIRE;
        var rules=CombatModifiers.parse(Map.of("scope",Map.of("element","炎"),"damage-dealt",percent(.5)));
        var m=new CombatModifiers();m.apply(rules,c,1);
        assertEquals(1.5,m.factor("damage-dealt",c));
        c.attribute=Element.WATER;var other=new CombatModifiers();other.apply(rules,c,1);
        assertEquals(1,other.factor("damage-dealt",c));
    }
    private EventContext context(SourceKind kind) {var c=new EventContext(TriggerEvent.BEFORE_HIT,null,null,null);c.sourceKind=kind;return c;}
    private Map<String,Object> percent(double value) {return Map.of("percent",value);}
    @Test void categoriesAddInternallyAndMultiplyAcrossLayersWithoutLeaking() {
        var c=context(SourceKind.NORMAL_ATTACK);var m=new CombatModifiers();
        var rules=CombatModifiers.parse(Map.of("damage-dealt",percent(.2),"normal-attack",Map.of("damage-dealt",percent(.3))));
        m.apply(rules,c,1);m.apply(CombatModifiers.parse(Map.of("damage-dealt",percent(.3))),c,1);
        assertEquals(1.5*1.3,m.factor("damage-dealt",c),1e-12);
        var skill=context(SourceKind.SKILL);var other=new CombatModifiers();other.apply(rules,skill,1);
        assertEquals(1.2,other.factor("damage-dealt",skill),1e-12);
    }
    @Test void healingExampleAndNonnegativeFactorsMatchSpecification() {
        var c=context(SourceKind.SKILL);var source=new CombatModifiers();var target=new CombatModifiers();
        source.apply(CombatModifiers.parse(Map.of("skill",Map.of("healing-dealt",percent(.5)))),c,1);
        target.apply(CombatModifiers.parse(Map.of("healing-received",percent(-.7))),c,1);
        assertEquals(450,1000*source.factor("healing-dealt",c)*target.factor("healing-received",c),1e-9);
        assertEquals(540,1000*1.2*source.factor("healing-dealt",c)*target.factor("healing-received",c),1e-9);
        target.apply(CombatModifiers.parse(Map.of("healing-received",percent(-1))),c,1);
        assertEquals(0,target.factor("healing-received",c));
    }
    @Test void elementOverrideAndCritOverrideAreScopedToNormalAttacks() {
        var rules=CombatModifiers.parse(Map.of("normal-attack",Map.of("element",Map.of("override","THUNDER"),"crit-rate",Map.of("override",1))));
        var c=context(SourceKind.NORMAL_ATTACK);var normal=new CombatModifiers();normal.apply(rules,c,1);
        assertEquals(Element.THUNDER,normal.element(Element.PHYSICAL));assertEquals(1,normal.critical("crit-rate",.2,c));
        var s=context(SourceKind.SKILL);var skill=new CombatModifiers();skill.apply(rules,s,1);
        assertEquals(Element.PHYSICAL,skill.element(Element.PHYSICAL));assertEquals(.2,skill.critical("crit-rate",.2,s));
    }
    @Test void tagsAndSourceScopeDoNotAffectOtherAbilities() {
        var rules=CombatModifiers.parse(Map.of("scope",Map.of("source-kind","SKILL","source-id","skill:one","tags",List.of("SLASH")),"damage-dealt",percent(.5)));
        var c=context(SourceKind.SKILL);c.sourceId="skill:one";c.tags.add("SLASH");
        var m=new CombatModifiers();m.apply(rules,c,1);assertEquals(1.5,m.factor("damage-dealt",c));
        c.sourceId="skill:two";var other=new CombatModifiers();other.apply(rules,c,1);assertEquals(1,other.factor("damage-dealt",c));
    }
    @Test void actionCriticalTakesPriorityWithoutChangingStats() {
        var c=context(SourceKind.DOT);var m=new CombatModifiers();
        m.apply(CombatModifiers.parse(Map.of("crit-rate",Map.of("override",1))),c,1);
        m.apply(CombatModifiers.action(Map.of("critical",Map.of("mode","ENABLED","rate",Map.of("override",.5),"damage",percent(.5)))),c,1);
        assertEquals(.5,m.critical("crit-rate",.2,c));assertEquals(1,m.critical("crit-damage",.5,c));
        assertEquals(CombatModifiers.CriticalMode.ENABLED,m.criticalMode());
    }
    @Test void invalidKeysAndNumbersFailValidation() {
        assertThrows(IllegalArgumentException.class,()->CombatModifiers.parse(Map.of("damage-dealt",Map.of("flat",10))));
        assertThrows(IllegalArgumentException.class,()->CombatModifiers.parse(Map.of("unknown",percent(.5))));
        assertThrows(IllegalArgumentException.class,()->CombatModifiers.parse(Map.of("damage-taken",percent(Double.NaN))));
        assertThrows(IllegalArgumentException.class,()->CombatModifiers.action(Map.of("critical",Map.of("mode","MAYBE"))));
        assertThrows(IllegalArgumentException.class,()->CombatModifiers.parse(Map.of("scope",Map.of("source-kind","INVALID"),"damage-dealt",percent(.5))));
    }
}
