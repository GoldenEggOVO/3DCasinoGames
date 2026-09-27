package dev.casino3d.game.wheel_of_fortune;

import dev.casino3d.Language;
import dev.casino3d.MachineGeometry;
import dev.casino3d.machine.*;
import dev.casino3d.model.MachineDefinition;

import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.entity.*;

import java.security.SecureRandom;
import java.util.*;

/** Physical display and animation for wheel_of_fortune; the round owns all game rules. */
public final class WheelOfFortuneMachine extends AnimatedMachine<WheelOfFortuneRound> {

    ItemDisplay disc;
    double discAngle, spinFrom, spinTo;
    final List<TextDisplay> wheelLabels = new ArrayList<>();

    public WheelOfFortuneMachine(
            MachineManager manager, UUID owner, Location origin, MachineDefinition definition) {
        super(manager, owner, origin, definition, new WheelOfFortuneRound(new SecureRandom()));
    }

    @Override
    protected void buildGame() {
        body("showcase_wheel_of_fortune");
        button("play", "showcase_button_round_spin");
        disc = model("showcase_wheel_fortune", 0, 2, .25, 4);
        model("showcase_pointer_fortune", 0, 2.93, .36, 4);
        for (double value :
                dev.casino3d.game.wheel_of_fortune.WheelOfFortuneRound.multipliers()) {
            var label = text(0, 2, .321, .25);
            translatedLabel(label, () -> value < 0 ? Language.component("fortune.again-label")
                    : Component.text(String.format(Locale.ROOT, "%sX", java.math.BigDecimal.valueOf(value)
                            .stripTrailingZeros().toPlainString())));
            wheelLabels.add(label);
        }
    }

    @Override
    protected boolean available(String action) {
        return round.available(action);
    }

    @Override
    protected void perform(String action) {
        round.action(action);
    }

    @Override
    protected void refresh() {
        if (round.finished())
            discAngle =
                    2 * Math.PI * (round.segment() + .5) / WheelOfFortuneRound.multipliers().length;
        rotateDisc(discAngle);
    }

    @Override
    protected void animateFrame(double progress, double ease) {
        double angle = spinFrom + (spinTo - spinFrom) * ease;
        if ((int) Math.floor(discAngle * 20 / (2 * Math.PI)) != (int) Math.floor(angle * 20 / (2 * Math.PI)))
            sound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, .25f, .95f + (float) (1 - progress) * .3f);
        rotateDisc(angle);
    }

    void rotateDisc(double angle) {
        discAngle = angle;
        var transform = MachineGeometry.itemPose(
                4,
                0, 0);
        transform.getLeftRotation().rotateZ((float) angle);
        pose(disc, transform);
        disc.setInterpolationDelay(0);
        for (int i = 0; i < wheelLabels.size(); i++) {
            double theta = 2 * Math.PI * (i + .5) / wheelLabels.size() - angle;
            wheelLabels
                    .get(i)
                    .teleport(at(Math.sin(theta) * .64, 2 + Math.cos(theta) * .64 - .02, .321));
            pose(wheelLabels.get(i), new org.bukkit.util.Transformation(new org.joml.Vector3f(),
                    new org.joml.Quaternionf().rotateZ((float) -theta),
                    new org.joml.Vector3f(.25f), new org.joml.Quaternionf()));
        }
    }

    void planSpin() {
        spinFrom = discAngle;
        double target =
                2 * Math.PI * (round.segment() + .5) / WheelOfFortuneRound.multipliers().length;
        double delta = (target - spinFrom) % (2 * Math.PI);
        if (delta < 0) delta += 2 * Math.PI;
        spinTo = spinFrom + 10 * Math.PI + delta;
    }

    @Override
    protected void animationPlanned() {
        planSpin();
    }

    @Override
    protected boolean repeatAnimation() {
        if (round.active()) {
            rotateDisc(spinTo);
            sound(Sound.BLOCK_NOTE_BLOCK_BIT, .3f, 1.7f);
            round.replayFortune();
            return true;
        }
        return false;
    }
}
