package dev.server.casino;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Set;

class ArchitectureTest {
    @Test
    void pluginMetadataHasNoOtherCustomPluginDependency() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(stream);
            var plugin =
                    YamlConfiguration.loadConfiguration(
                            new InputStreamReader(stream, StandardCharsets.UTF_8));
            assertTrue(plugin.getStringList("depend").isEmpty());
            var otherPlugins = Set.of("ServerGames", "ServerBoards", "ServerMenu", "KaMenu");
            assertTrue(
                    plugin.getStringList("softdepend").stream()
                            .noneMatch(otherPlugins::contains));
        }
    }

    @Test
    void pluginManagersAreNotPublicMutableFields() {
        for (var field : CasinoPlugin.class.getDeclaredFields()) {
            assertFalse(
                    Modifier.isPublic(field.getModifiers())
                            && !Modifier.isFinal(field.getModifiers()),
                    field.getName());
        }
    }

    @Test
    void optionalMenusDoNotOwnSettlementServicesOrJobs() throws Exception {
        for (var field : Class.forName("dev.server.casino.CasinoMenus").getDeclaredFields()) {
            assertNotEquals(CasinoService.class, field.getType());
            assertFalse(BukkitTask.class.isAssignableFrom(field.getType()));
        }
    }
}
