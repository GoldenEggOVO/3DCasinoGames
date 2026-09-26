package dev.casino3d;

import static org.junit.jupiter.api.Assertions.*;
import dev.casino3d.game.hilo.HiloMachine;
import org.junit.jupiter.api.Test;

class HiloAnimationTest {
    @Test
    void revealTraversesBothEndsBeforeSettlingOnTheActualRoll() {
        for (int roll : new int[]{0, 1, 50, 98, 99}) {
            assertEquals(0, HiloMachine.revealValue(0, roll), 1e-8);
            assertEquals(100, HiloMachine.revealValue(.35, roll), 1e-8);
            assertEquals(0, HiloMachine.revealValue(.70, roll), 1e-8);
            assertEquals(roll, HiloMachine.revealValue(1, roll), 1e-8);
            double previous = 0;
            for (int frame = 0; frame <= 80; frame++) {
                double value = HiloMachine.revealValue(frame / 80d, roll);
                assertTrue(value >= 0 && value <= 100);
                assertTrue(Math.abs(value-previous) < 8, "Visible discontinuity at frame " + frame);
                previous = value;
            }
        }
    }
}
