package dev.casino3d.machine;

import dev.casino3d.game.PracticeRound;
import dev.casino3d.model.MachineDefinition;

import org.bukkit.Location;
import org.bukkit.Sound;

import java.util.UUID;

/** Timing and completion sounds shared by games with finite reveal animations. */
public abstract class AnimatedMachine<R extends PracticeRound> extends PracticeMachine<R> {
    protected int animationStart;
    protected int animationEnd;

    protected AnimatedMachine(
            MachineManager manager,
            UUID owner,
            Location origin,
            MachineDefinition definition,
            java.util.function.Supplier<R> round) {
        super(manager, owner, origin, definition, round);
    }

    @Override
    protected final boolean busy() {
        return age < animationEnd;
    }

    protected abstract void perform(String action);

    protected abstract void animateFrame(double progress, double ease);

    protected boolean shouldAnimate(String action) {
        return action.equals("play") || action.equals("step");
    }

    protected void beforeAction(String action) {}

    protected void animationPlanned() {}

    protected boolean repeatAnimation() {
        return false;
    }

    protected void animationFinished() {}

    protected void idleTick() {}

    @Override
    protected void action(String action) {
        beforeAction(action);
        perform(action);
        if (shouldAnimate(action)) {
            animationStart = age;
            animationEnd = age + (action.equals("play") ? 80 : 30);
            animationPlanned();
        } else refresh();
    }

    @Override
    protected final void animate() {
        idleTick();
        if (animationEnd == 0) return;
        if (!busy()) {
            if (repeatAnimation()) {
                animationStart = age;
                animationEnd = age + 80;
                animationPlanned();
                return;
            }
            animationEnd = 0;
            refresh();
            animationFinished();
            sound(Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, .18f, .7f);
            if (round.active()) sound(Sound.BLOCK_NOTE_BLOCK_PLING, .22f, 1.2f);
            return;
        }
        double progress = (age - animationStart) / (double) (animationEnd - animationStart);
        animateFrame(progress, 1 - Math.pow(1 - progress, 3));
        movementSound(progress);
    }

    /** Each game supplies a cue tied to its visible movement. */
    protected void movementSound(double progress) {}
}
