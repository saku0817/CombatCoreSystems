package com.github.saku0817.combatcoresystems.service;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class V142RegressionTest {
    @Test void virtualMobHealthScalesPastMinecraftAttributeLimit() {
        assertEquals(1024, MobService.physicalMaximum(50_000, 1024));
        assertEquals(512, MobService.physicalHealth(25_000, 50_000, 1024), 1e-9);
        assertEquals(1024, MobService.physicalHealth(50_000, 50_000, 1024), 1e-9);
    }

    @Test void newControlsAndWebEditorDefaultsExist() throws Exception {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(Path.of("src/main/resources/config.yml").toFile());
        assertTrue(config.getBoolean("controls.allow-empty-cast"));
        assertTrue(config.getBoolean("controls.bedrock-selected-slot-drop-skill"));
        assertFalse(config.getBoolean("web-editor.enabled"));
        String page = Files.readString(Path.of("src/main/resources/web-editor.html"));
        assertTrue(page.contains("ダメージシミュレータ"));
        assertTrue(page.contains("リアルタイムプレビュー"));
    }
}
