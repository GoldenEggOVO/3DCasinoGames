package dev.casino3d.machine;

import java.util.LinkedHashMap;
import java.util.Map;

/** Presentation-only practice results. Does not hold or transfer an economy balance. */
public final class FeedbackState {
    public enum Outcome { LOSS, EVEN, WIN, BIG_WIN }
    public record Result(long stake, long returned) {
        public long net() { return returned - stake; }
        public Outcome outcome() {
            if (net() < 0) return Outcome.LOSS;
            if (net() == 0) return Outcome.EVEN;
            return returned / stake >= 5 ? Outcome.BIG_WIN : Outcome.WIN;
        }
    }
    private final Map<Object, Long> pending = new LinkedHashMap<>();
    private Result last;
    private long totalNet;

    public void launch(Object id, long stake) {
        if (stake <= 0) throw new IllegalArgumentException("Positive stake required");
        if (pending.putIfAbsent(id, stake) == null) last = null;
    }
    public void reviseStake(Object id, long stake) {
        if (pending.containsKey(id)) pending.put(id, stake);
    }
    public Result reveal(Object id, long returned) {
        Long stake = pending.remove(id);
        if (stake == null) return null;
        last = new Result(stake, returned);
        totalNet += last.net();
        return last;
    }
    public Result last() { return last; }
    public int pending() { return pending.size(); }
    public long totalNet() { return totalNet; }
}
