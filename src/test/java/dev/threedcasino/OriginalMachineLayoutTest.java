package dev.threedcasino;

import static org.junit.jupiter.api.Assertions.*;

import dev.threedcasino.model.MachineDefinition;
import dev.threedcasino.game.plinko.PlinkoMachine;

import org.junit.jupiter.api.Test;

class OriginalMachineLayoutTest {
    @Test
    void blackjackTableIsJustAboveControlsAndStillMeetsTheFloor() throws Exception {
        var model =
                com.google.gson.JsonParser.parseString(
                                java.nio.file.Files.readString(
                                        java.nio.file.Path.of(
                                                "resource-pack/assets/3dcasino/models/item/cabinet_blackjack.json")))
                        .getAsJsonObject();
        double bottom = Double.POSITIVE_INFINITY, top = Double.NEGATIVE_INFINITY;
        for (var element : model.getAsJsonArray("elements")) {
            var box = element.getAsJsonObject();
            bottom = Math.min(bottom, (box.getAsJsonArray("from").get(1).getAsDouble() - 8) / 4);
            top = Math.max(top, (box.getAsJsonArray("to").get(1).getAsDouble() - 8) / 4);
        }
        assertEquals(0, bottom, 1e-9);
        assertEquals(.92, top, 1e-9);
        var button = MachineDefinition.builtin("blackjack").button("start");
        var buttonTop = button.transform().apply(0, .4 * button.size(), .18 * button.size());
        assertTrue(top > buttonTop.y());
        assertTrue(top - buttonTop.y() < .1);
    }

    @Test
    void plinkoCompactButtonRestoresAndKeepsItsHitboxBelowMultipliers() throws Exception {
        var rest = PlinkoMachine.playButtonPose(false);
        var pressed = PlinkoMachine.playButtonPose(true);
        assertEquals(.9, rest.getScale().x * .6 / 4, 1e-6);
        assertEquals(.65, rest.getScale().y / 4, 1e-6);
        assertEquals(rest.getScale(), pressed.getScale());
        assertEquals(rest.getLeftRotation(), pressed.getLeftRotation());
        assertEquals(0, rest.getTranslation().length(), 1e-9);
        assertTrue(pressed.getTranslation().y < 0);
        var hit = PlinkoMachine.playButtonHit();
        // Hit location is in cabinet coordinates; width/height are world units.
        double buttonBase = .30 * .75;
        var top = MachineGeometry.panelPoint(0, .4 * .65, .18 * .65, -Math.toRadians(25));
        assertEquals(.92, hit.width(), 1e-9);
        assertTrue(hit.y() * .75 <= buttonBase);
        assertTrue(hit.y() * .75 <= buttonBase + pressed.getTranslation().y);
        assertTrue(hit.y() * .75 + hit.height() >= buttonBase + top.y());
        assertTrue(hit.y() * .75 + hit.height() < .76 * .75);
    }

    @Test
    void blackjackButtonsRemainSeparatedAndClearOfTheTableWithoutTheirPanel() {
        var definition = MachineDefinition.builtin("blackjack");
        double right = Double.NEGATIVE_INFINITY;
        for (String action : new String[] {"double", "stand", "hit", "start"}) {
            var button = definition.button(action);
            double halfWidth = .3 * button.size();
            assertTrue(button.transform().x() - halfWidth > right + .02);
            right = button.transform().x() + halfWidth;
            assertTrue(Math.abs(button.transform().x()) + halfWidth < 1);
            for (double y : new double[] {0, .4})
                for (double z : new double[] {-.05, .13}) {
                    var point = button.transform().apply(0, y * button.size(), z * button.size() - button.press());
                    assertTrue(point.z() > 1.15);
                    assertTrue(point.y() > .48 && point.y() < .9);
                }
        }
    }

    @Test
    void plinkoLabelsMatchThePayoutOfTheirOwnLandingSlots() {
        for (int slot = 0; slot < 13; slot++) {
            String label = PlinkoMachine.multiplierLabel(slot);
            assertTrue(label.endsWith("X"));
            double shown = Double.parseDouble(label.substring(0, label.length() - 1));
            double payout = CasinoRules.plinkoPayout(1_000_000, slot) / 1_000_000.0;
            assertEquals(payout, shown, .0051);
            assertEquals(label, PlinkoMachine.multiplierLabel(12 - slot));
            var landing = PlinkoPath.at((1 << slot) - 1, 12);
            assertEquals(landing.x(), PlinkoMachine.slotLabelX(slot), 1e-9);
        }
        assertTrue(Double.parseDouble(PlinkoMachine.multiplierLabel(0).replace("X", "")) > 1);
        assertTrue(Double.parseDouble(PlinkoMachine.multiplierLabel(6).replace("X", "")) < 1);
    }
}
