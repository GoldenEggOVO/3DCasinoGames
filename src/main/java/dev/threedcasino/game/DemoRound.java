package dev.threedcasino.game;

import static dev.threedcasino.Language.text;
import java.util.Locale;
import java.util.Random;

/** Shared practice stake capture and result text for the original demo machines. */
public abstract class DemoRound extends PracticeRound {
    private long configuredStake = STAKE;

    protected DemoRound(Random random) {
        super(random);
        result = text("demo.idle");
    }

    public final long configuredStake() {
        return configuredStake;
    }

    public final void setConfiguredStake(long amount) {
        if (!active && amount >= MIN_STAKE && amount <= MAX_STAKE && amount % 100 == 0) {
            configuredStake = amount;
        }
    }

    protected final void beginDemo() {
        begin();
        stake = configuredStake;
        result = text("demo.playing");
    }

    protected final void settle(long amount) {
        payout = amount;
        active = false;
        finished = true;
        result = amount > 0 ? text("demo.payout", "amount", money(amount)) : text("demo.finished");
    }

    protected static String money(long cents) {
        return String.format(Locale.ROOT, "%.2f", cents / 100.0);
    }

    public abstract void start(long now);
}
