package com.github.saku0817.combatcoresystems.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DefaultYamlTest {
    @Test void bundledConfigurationHasNoDuplicateKeysAndAllRootsAreMappings() throws Exception {
        LoaderOptions options = new LoaderOptions(); options.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        for (String file : DefinitionRegistry.FILES) {
            Object value = yaml.load(Files.readString(Path.of("src/main/resources", file)));
            assertInstanceOf(Map.class, value, file);
            assertEquals(1, ((Map<?, ?>) value).get("data-version"), file);
        }
    }
}
