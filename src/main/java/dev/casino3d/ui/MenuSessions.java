package dev.casino3d.ui;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Main-thread session tokens; monotonic expiration, owner binding and single consumption. */
public final class MenuSessions<T> {
    public record Entry<T>(UUID token, long created, T value) {}
    private final Map<UUID, Entry<T>> entries = new HashMap<>();
    private final long lifetime;
    private final LongSupplier clock;

    public MenuSessions(Duration lifetime, LongSupplier clock) {
        this.lifetime = lifetime.toNanos();
        if (this.lifetime <= 0) throw new IllegalArgumentException("Lifetime must be positive");
        this.clock = java.util.Objects.requireNonNull(clock);
    }

    public void put(UUID player, UUID token, T value) {
        entries.put(player, new Entry<>(token, clock.getAsLong(), value));
    }

    public Entry<T> get(UUID player) {
        var entry = entries.get(player);
        if (entry != null && clock.getAsLong() - entry.created >= lifetime) {
            entries.remove(player);
            return null;
        }
        return entry;
    }

    public T consume(UUID player, UUID token) {
        var entry = get(player);
        if (entry == null || !entry.token.equals(token)) return null;
        entries.remove(player);
        return entry.value;
    }

    public void remove(UUID player) { entries.remove(player); }
    public void clear() { entries.clear(); }
    public Set<UUID> players() { return Set.copyOf(entries.keySet()); }

    public void expire() {
        long now = clock.getAsLong();
        entries.values().removeIf(entry -> now - entry.created >= lifetime);
    }
}
