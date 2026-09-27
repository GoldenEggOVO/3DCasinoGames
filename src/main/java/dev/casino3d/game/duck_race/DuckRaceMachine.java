package dev.casino3d.game.duck_race;

import dev.casino3d.machine.*;
import dev.casino3d.model.MachineDefinition;
import dev.casino3d.model.ButtonDefinition;

import org.bukkit.*;
import org.bukkit.entity.*;

import java.security.SecureRandom;
import java.util.*;

/** Physical display and animation for duck_race; the round owns all game rules. */
public final class DuckRaceMachine extends AnimatedMachine<DuckRaceRound> {
    private static final double START_Z = .7;
    private static final double FINISH_Z = -2.3;

    final List<ItemDisplay> figures = new ArrayList<>();

    public DuckRaceMachine(
            MachineManager manager, UUID owner, Location origin, MachineDefinition definition) {
        super(manager, owner, origin, definition, new DuckRaceRound(new SecureRandom()));
    }

    @Override
    protected void buildGame() {
        body("showcase_duck_race");
        boolean builtin = definition.equals(MachineDefinition.builtin("duck_race"));
        for (int i = 0; i < 4; i++) {
            var original = definition.button("select:" + i);
            var pose = original.transform();
            button("select:" + i, "showcase_button_duck_" + (i + 1), builtin
                    ? ButtonDefinition.at(pose.x(), pose.y(), pose.z() + .22,
                            original.width(), pose.pitch(), original.size())
                    : original);
        }
        button("play", "showcase_button_play", builtin
                ? ButtonDefinition.at(0, .31, 1.73, .65, -35, 1.2)
                : definition.button("play"));
        for (int i = 0; i < 4; i++)
            figures.add(model("showcase_duck_" + (i + 1), -.9 + i * .6, 1.08, START_Z, 4));
    }

    @Override
    protected List<Double> settingsBounds() {
        // Keep existing saved/default skin definitions compatible with the controls.
        if (!definition.equals(MachineDefinition.builtin("duck_race"))) return super.settingsBounds();
        var bounds = new ArrayList<>(super.settingsBounds());
        bounds.set(2, -2.85);
        return bounds;
    }

    private double finishZ(int lane) {
        return lane == round.winner() ? FINISH_Z : FINISH_Z + .8 + lane * .18;
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
        for (int i = 0; i < 4; i++)
            figures.get(i)
                    .teleport(
                            at(
                                    -.9 + i * .6,
                                    1.08,
                                    round.finished()
                                            ? finishZ(i)
                                            : START_Z));
    }

    @Override
    protected void movementSound(double progress) {
        if (age % 8 == 0) sound(Sound.BLOCK_WOOD_STEP, .12f, 1.1f + (float) progress * .4f);
    }

    @Override
    protected void animateFrame(double progress, double ease) {
        for (int i = 0; i < 4; i++) {
            double finish = finishZ(i);
            double z =
                    START_Z
                            + (finish - START_Z) * progress
                            + Math.sin(progress * Math.PI) * Math.sin(age * .18 + i) * .08;
            figures.get(i)
                    .teleport(at(-.9 + i * .6, 1.08 + Math.abs(Math.sin(age * .35 + i)) * .035, z));
        }
    }
}
