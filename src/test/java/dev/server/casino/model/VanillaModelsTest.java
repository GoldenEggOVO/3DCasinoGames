package dev.server.casino.model;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class VanillaModelsTest {
    @Test
    void allGameplayVisualsHaveVisibleVanillaMaterials() {
        assertEquals(Material.PAPER, VanillaModels.material("casino:card_51"));
        assertEquals(Material.LIME_CONCRETE, VanillaModels.material("casino:showcase_tile_selected"));
        assertEquals(Material.LIGHT_GRAY_CONCRETE, VanillaModels.material("casino:showcase_tile"));
        assertEquals(Material.SLIME_BALL, VanillaModels.material("casino:plinko_ball"));
        assertEquals(Material.CLOCK, VanillaModels.material("casino:showcase_wheel_fortune"));
        assertEquals(Material.STONE_BUTTON, VanillaModels.material("casino:showcase_button_play"));
        assertEquals(Material.PAPER, VanillaModels.material("other:unknown_skin"));
    }

    @Test
    void oversizedCustomModelsAreReducedToUsableVanillaSizes() {
        assertEquals(.07, VanillaModels.size("showcase_tile"), 1e-9);
        assertEquals(.4, VanillaModels.size("showcase_wheel_fortune"), 1e-9);
        assertEquals(1, VanillaModels.size("card_51"), 1e-9);
        assertEquals(1, VanillaModels.size("plinko_ball"), 1e-9);
    }
}
