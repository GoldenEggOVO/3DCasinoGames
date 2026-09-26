package dev.casino3d.model;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class VanillaModelsTest {
    @Test
    void allGameplayVisualsHaveVisibleVanillaMaterials() {
        assertEquals(Material.PAPER, VanillaModels.material("3dcasino:card_51"));
        assertEquals(Material.ORANGE_CONCRETE, VanillaModels.material("3dcasino:showcase_tile_selected"));
        assertEquals(Material.BROWN_CONCRETE, VanillaModels.material("3dcasino:showcase_tile"));
        assertEquals(Material.SLIME_BALL, VanillaModels.material("3dcasino:plinko_ball"));
        assertEquals(Material.CLOCK, VanillaModels.material("3dcasino:showcase_wheel_fortune"));
        assertEquals(Material.STONE_BUTTON, VanillaModels.material("3dcasino:showcase_button_play"));
        assertEquals(Material.PAPER, VanillaModels.material("other:unknown_skin"));
    }

    @Test
    void gameplayModelsUseOriginalGeometryRatherThanShrunkenPlaceholderItems() {
        for (String name : java.util.List.of("showcase_tile", "showcase_wheel_fortune",
                "showcase_duck_1", "showcase_penguin", "cabinet_rocket", "showcase_hilo_panel"))
            assertNotNull(VanillaGeometry.get(name), name);
    }
}
