package dev.casino3d.game.crash;

import static dev.casino3d.Language.text;
import dev.casino3d.CasinoRules;
import dev.casino3d.game.DemoRound;

import java.util.Random;

public final class CrashRound extends DemoRound {
    private boolean cashed;
    private int crashPoint = 100;
    private int multiplier = 100;
    private long started;

    public CrashRound(Random random) {
        super(random);
    }

    public boolean cashed() {
        return cashed;
    }

    public int crashPoint() {
        return crashPoint;
    }

    public int multiplier() {
        return multiplier;
    }

    public long started() {
        return started;
    }

    @Override
    public void start(long now) {
        if (active) {
            return;
        }
        beginDemo();
        cashed = false;
        started = now;
        multiplier = 100;
        crashPoint = CasinoRules.point(random.nextLong(CasinoRules.RANDOM_SPACE));
    }

    public boolean cash(long now) {
        if (!active) {
            return false;
        }
        tick(now);
        if (!active || cashed) {
            return false;
        }
        cashed = true;
        payout = stake * multiplier / 100;
        result = text("crash.collected", "amount", money(payout));
        return true;
    }

    public void tick(long now) {
        if (active) {
            multiplier = CasinoRules.crashMultiplier(started, now);
            if (multiplier >= crashPoint) {
                multiplier = crashPoint;
                settle(cashed ? payout : 0);
            }
        }
    }
}
