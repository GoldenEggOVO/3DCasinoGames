package dev.server.casino;

import static org.junit.jupiter.api.Assertions.*;

import dev.server.casino.game.mines.MinesMachine;
import dev.server.casino.model.ModelTransform;
import org.junit.jupiter.api.Test;

class MinesLayoutTest {
    @Test
    void controlsFitTheSlopedConsoleWithTwoSeparatedRows() {
        var console = new ModelTransform(0, .67, 1.96, -35, 0, 0, 1);
        for (String action : new String[] {"minus", "plus", "start", "cash"}) {
            var button = MinesMachine.vanillaButton(action);
            var p = button.transform();
            var local = console.inverse(p.x(), p.y(), p.z());
            assertEquals(.065, local.z(), 1e-6);
            assertEquals(-35, p.pitch());
            assertTrue(Math.abs(local.x()) + button.width()/2 < 1.15);
            assertTrue(local.y() > 0 && local.y() + button.height() < .67);
            // Even fully pressed, the coloured cap stays in front of the .018 inset.
            assertTrue(local.z() + .13*button.size() - button.press() > .018);
        }
        var play = MinesMachine.vanillaButton("start");
        var minus = MinesMachine.vanillaButton("minus");
        assertTrue(play.transform().x() < 0);
        assertTrue(MinesMachine.vanillaButton("cash").transform().x() > 0);
        assertTrue(play.transform().y() + play.height() < minus.transform().y());
    }
}
