package dev.server.casino.model;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class VanillaModelsTest {
    @Test
    void allGameplayVisualsHaveVisibleVanillaMaterials() {
        assertEquals(Material.PAPER, VanillaModels.material("casino:card_51"));
        assertEquals(Material.ORANGE_CONCRETE, VanillaModels.material("casino:showcase_tile_selected"));
        assertEquals(Material.BROWN_CONCRETE, VanillaModels.material("casino:showcase_tile"));
        assertEquals(Material.SLIME_BALL, VanillaModels.material("casino:plinko_ball"));
        assertEquals(Material.CLOCK, VanillaModels.material("casino:showcase_wheel_fortune"));
        assertEquals(Material.STONE_BUTTON, VanillaModels.material("casino:showcase_button_play"));
        assertEquals(Material.PAPER, VanillaModels.material("other:unknown_skin"));
    }

    @Test
    void gameplayModelsUseOriginalGeometryRatherThanShrunkenPlaceholderItems() {
        for (String name : java.util.List.of("showcase_tile", "showcase_wheel_fortune",
                "showcase_duck_1", "showcase_penguin", "cabinet_rocket", "showcase_hilo_panel"))
            assertNotNull(VanillaGeometry.get(name), name);
    }
}
