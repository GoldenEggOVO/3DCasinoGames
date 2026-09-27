package dev.casino3d;

import dev.casino3d.ui.MenuSessions;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuSessionsTest {
    @Test void oldPagesOtherPlayersAndDoubleClicksCannotConsumeCurrentSession() {
        var clock = new AtomicLong();
        var sessions = new MenuSessions<String>(Duration.ofMinutes(5), clock::get);
        UUID player = UUID.randomUUID(), old = UUID.randomUUID(), current = UUID.randomUUID();
        sessions.put(player, old, "old");
        sessions.put(player, current, "current");
        assertNull(sessions.consume(player, old));
        assertNull(sessions.consume(UUID.randomUUID(), current));
        assertEquals("current", sessions.consume(player, current));
        assertNull(sessions.consume(player, current));
    }

    @Test void expiryAndClearRemoveUnclickedPages() {
        var clock = new AtomicLong();
        var sessions = new MenuSessions<String>(Duration.ofMinutes(5), clock::get);
        UUID player = UUID.randomUUID(), token = UUID.randomUUID();
        sessions.put(player, token, "value");
        clock.set(Duration.ofMinutes(5).toNanos());
        sessions.expire();
        assertTrue(sessions.players().isEmpty());
        assertNull(sessions.consume(player, token));
        sessions.put(player, token, "new");
        sessions.clear();
        assertNull(sessions.consume(player, token));
    }
}
