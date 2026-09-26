package dev.casino3d.machine;

import static org.junit.jupiter.api.Assertions.*;

import dev.casino3d.model.VanillaGeometry;
import org.junit.jupiter.api.Test;

class VanillaDisplayTest {
    @Test
    void dragonRevealsCanChangeMaterialWithoutReplacingDisplayEntities() {
        var hidden = VanillaGeometry.get("dragon_tile_hidden");
        var safe = VanillaGeometry.get("dragon_tile_safe");
        var trap = VanillaGeometry.get("dragon_tile_trap");

        assertTrue(VanillaDisplay.sameBlockGeometry(hidden, safe));
        assertTrue(VanillaDisplay.sameBlockGeometry(hidden, trap));
        assertFalse(VanillaDisplay.sameBlockGeometry(hidden,
                VanillaGeometry.get("showcase_dragon_tower_compact")));
    }
}
