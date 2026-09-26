package dev.threedcasino;

import static org.junit.jupiter.api.Assertions.*;
import dev.threedcasino.game.dragon_tower.DragonTowerMachine;
import dev.threedcasino.model.VanillaGeometry;
import org.junit.jupiter.api.Test;

class DragonLayoutTest {
    @Test
    void compactBodyFitsTheTowerAndLowerControlsWithoutTheTable() {
        var body = VanillaGeometry.get("showcase_dragon_tower_compact");
        assertNotNull(body);
        var bounds = body.bounds();
        assertTrue(bounds.getWidthX() < 1.9);
        assertTrue(bounds.getWidthZ() < 1.1);
        assertEquals(0, bounds.getMinY(), 1e-5);
        for (String action : new String[]{"play", "cash"}) {
            var button = DragonTowerMachine.vanillaButton(action);
            assertTrue(Math.abs(button.transform().x()) + .3*button.size() < .9);
            assertTrue(button.transform().y() + .4*button.size() < .61);
            assertTrue(button.transform().z() < .8);
        }
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 4; col++) {
                var cell = DragonTowerMachine.vanillaCell(row,col);
                assertTrue(cell.y() - .14 > .60);
                assertTrue(cell.y() + .14 < 2.55);
                assertEquals(ShowcaseGeometry.dragonCell(row,col).x(),cell.x());
            }
    }
}
