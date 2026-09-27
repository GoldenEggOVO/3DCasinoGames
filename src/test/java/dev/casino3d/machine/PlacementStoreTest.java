package dev.casino3d.machine;

import static org.junit.jupiter.api.Assertions.*;

import dev.casino3d.model.MachineDefinition;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlacementStoreTest {
    @TempDir Path directory;

    @Test void restoredDefinitionsReceiveTheSameValidationAsEditableSkins() throws Exception {
        Path file = directory.resolve("placements.json");
        var store = new PlacementStore(file);
        store.save(List.of(new PlacementStore.Placement(UUID.randomUUID(), UUID.randomUUID(),
                0, 64, 0, 0, MachineDefinition.builtin("blackjack"), 1000)));
        String original = Files.readString(file);
        for (String corruption : List.of("model", "bounds", "rotation")) {
            var json = com.google.gson.JsonParser.parseString(original).getAsJsonObject();
            var definition = json.getAsJsonArray("machines").get(0).getAsJsonObject().getAsJsonObject("definition");
            switch (corruption) {
                case "model" -> definition.getAsJsonObject("models").addProperty("cabinet_blackjack", "INVALID");
                case "bounds" -> definition.getAsJsonArray("settingsBounds").set(0, new com.google.gson.JsonPrimitive(999));
                case "rotation" -> definition.getAsJsonObject("anchors").getAsJsonObject("body").addProperty("pitch", 999);
            }
            String invalid = json.toString();
            Files.writeString(file, invalid);
            assertThrows(java.io.IOException.class, store::load, corruption);
            assertEquals(invalid, Files.readString(file));
        }
    }

    @Test
    void allGameTypesAndSettingsSurviveRestartAndDeletion() throws Exception {
        var store = new PlacementStore(directory.resolve("placements.json"));
        UUID owner = UUID.randomUUID(), world = UUID.randomUUID();
        var placements = MachineDefinition.games().stream()
                .map(game -> new PlacementStore.Placement(owner, world, 12.5, 64, -7.5,
                        90, MachineDefinition.builtin(game), 2500)).toList();
        store.save(placements);
        assertEquals(new HashSet<>(placements), new HashSet<>(new PlacementStore(directory.resolve("placements.json")).load()));
        store.save(placements.subList(1, placements.size()));
        assertEquals(11, store.load().size());
        store.save(List.of());
        assertTrue(store.load().isEmpty());
    }

    @Test
    void corruptedDataIsRejectedWithoutOverwritingIt() throws Exception {
        Path file = directory.resolve("placements.json");
        Files.writeString(file, "{broken");
        assertThrows(java.io.IOException.class, () -> new PlacementStore(file).load());
        assertEquals("{broken", Files.readString(file));
    }

    @Test
    void failedWriteKeepsPreviousFile() throws Exception {
        var store = new PlacementStore(directory.resolve("placements.json"));
        store.save(List.of());
        String original = Files.readString(directory.resolve("placements.json"));
        Files.createDirectory(directory.resolve("placements.json.tmp"));
        assertThrows(java.io.IOException.class, () -> store.save(List.of()));
        assertEquals(original, Files.readString(directory.resolve("placements.json")));
    }
}
