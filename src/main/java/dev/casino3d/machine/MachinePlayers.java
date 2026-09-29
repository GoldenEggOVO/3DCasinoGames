package dev.casino3d.machine;

import dev.casino3d.game.PracticeRound;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Personal practice data and one exclusive player for a physical machine. */
final class MachinePlayers<R extends PracticeRound> {
    static final int IDLE_TICKS = 20 * 120;
    static final class Data<R extends PracticeRound> {
        R round;
        final FeedbackState feedback = new FeedbackState();
        long observedRound;
        Data(R round) { this.round = round; }
    }
    private final Supplier<R> factory;
    private final java.util.function.BiConsumer<UUID, Data<R>> initialize;
    private final Map<UUID, Data<R>> data = new HashMap<>();
    private UUID player;
    private boolean started;
    private int idleTicks;

    MachinePlayers(Supplier<R> factory) { this(factory, (id, data) -> {}); }
    MachinePlayers(Supplier<R> factory, java.util.function.BiConsumer<UUID, Data<R>> initialize) {
        this.factory = factory;
        this.initialize = initialize;
    }
    Data<R> data(UUID id) {
        return data.computeIfAbsent(id, key -> {
            var state = new Data<>(factory.get());
            initialize.accept(key, state);
            return state;
        });
    }
    UUID player() { return player; }
    boolean accepts(UUID id) { return player == null || player.equals(id); }
    boolean claim(UUID id) {
        if (!accepts(id)) return false;
        player = id;
        idleTicks = 0;
        return true;
    }
    void started() { started = true; }
    void update(boolean inProgress) {
        if (player != null && started && !inProgress) release();
    }
    void tick() { if (player != null) idleTicks++; }
    boolean expired() { return player != null && idleTicks >= IDLE_TICKS; }
    void abandon() {
        if (player != null) {
            var state = data(player);
            var profile = PersonalDataStore.Profile.capture(state.round, state.feedback);
            state.round = factory.get();
            state.observedRound = 0;
            profile.restore(state.round, state.feedback);
        }
        release();
    }
    private void release() { player = null; started = false; idleTicks = 0; }
}
