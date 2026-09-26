package dev.casino3d.game.money_wheel;

import dev.casino3d.MachineGeometry;
import dev.casino3d.machine.*;
import dev.casino3d.model.MachineDefinition;

import org.bukkit.*;
import org.bukkit.entity.*;

import java.security.SecureRandom;
import java.util.*;

/** Physical display and animation for money_wheel; the round owns all game rules. */
public final class MoneyWheelMachine extends AnimatedMachine<MoneyWheelRound> {

    ItemDisplay disc;
    double discAngle, spinFrom, spinTo;
    final List<TextDisplay> wheelLabels = new ArrayList<>();

    public MoneyWheelMachine(
            MachineManager manager, UUID owner, Location origin, MachineDefinition definition) {
        super(manager, owner, origin, definition, new MoneyWheelRound(new SecureRandom()));
    }

    @Override
    protected void buildGame() {
        body("showcase_money_wheel");
        button("play", "showcase_button_round_spin");
        for (int i = 0; i < 4; i++) button("select:" + i, "showcase_button_money_" + i);
        disc = model("showcase_wheel_money", 0, 2, .25, 4);
        model("showcase_pointer", 0, 2.93, .36, 4);
        int[] segments = MoneyWheelRound.segments(), multipliers = MoneyWheelRound.multipliers();
        for (int segment : segments) {
            var label = text(0, 2, .30, .30);
            label.text(net.kyori.adventure.text.Component.text(multipliers[segment] + "X"));
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
        selectedButtons(a -> a.equals("select:" + round.choice()));
        if (round.finished())
            discAngle = 2 * Math.PI * (round.segment() + .5) / MoneyWheelRound.segments().length;
        rotateDisc(discAngle);
    }

    @Override
    protected void animateFrame(double progress, double ease) {
        rotateDisc(spinFrom + (spinTo - spinFrom) * ease);
    }

    void rotateDisc(double angle) {
        discAngle = angle;
        var transform = MachineGeometry.itemPose(
                4, 0, 0);
        transform.getLeftRotation().rotateZ((float) angle);
        pose(disc, transform);
        disc.setInterpolationDelay(0);
        for (int i = 0; i < wheelLabels.size(); i++) {
            double theta = 2 * Math.PI * (i + .5) / wheelLabels.size() - angle;
            wheelLabels
                    .get(i)
                    .teleport(at(Math.sin(theta) * .70, 2 + Math.cos(theta) * .70 - .03, .30));
        }
    }

    void planSpin() {
        spinFrom = discAngle;
        double target = 2 * Math.PI * (round.segment() + .5) / MoneyWheelRound.segments().length;
        double delta = (target - spinFrom) % (2 * Math.PI);
        if (delta < 0) delta += 2 * Math.PI;
        spinTo = spinFrom + 10 * Math.PI + delta;
    }

    @Override
    protected void animationPlanned() {
        planSpin();
    }
}
