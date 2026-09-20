package com.github.saku0817.combatcoresystems.model;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SetTriggerTest {
    @Test void readsTriggerAndBuffReference() throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString("triggers: {low: {event: HP_BELOW, hp-percent: 0.3, cooldown-seconds: 15, effects: [shield]}}");
        var trigger = SetTrigger.parse(yaml.getConfigurationSection("triggers"), Set.of("shield")).getFirst();
        assertEquals(SetTrigger.Event.HP_BELOW, trigger.event()); assertEquals(.3, trigger.hpBelow()); assertEquals(15000, trigger.cooldownMillis());
    }
    @Test void missingBuffAndInvalidEventRejected() throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString("triggers: {x: {event: SKILL, effects: [missing]}}");
        assertThrows(IllegalArgumentException.class, () -> SetTrigger.parse(yaml.getConfigurationSection("triggers"), Set.of()));
        yaml.set("triggers.x.event", "UNKNOWN"); assertThrows(IllegalArgumentException.class, () -> SetTrigger.parse(yaml.getConfigurationSection("triggers"), Set.of("missing")));
    }
    @Test void invalidRatioAndTargetRejected() throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString("triggers: {x: {event: HP_BELOW, hp-percent: 30, effects: [shield]}}");
        assertThrows(IllegalArgumentException.class, () -> SetTrigger.parse(yaml.getConfigurationSection("triggers"), Set.of("shield")));
        yaml.set("triggers.x.hp-percent", .3); yaml.set("triggers.x.target", "OTHER");
        assertThrows(IllegalArgumentException.class, () -> SetTrigger.parse(yaml.getConfigurationSection("triggers"), Set.of("shield")));
    }
}
