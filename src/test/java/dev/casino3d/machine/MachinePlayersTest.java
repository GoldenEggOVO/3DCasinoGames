package dev.casino3d.machine;

import dev.casino3d.game.keno.KenoRound;
import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MachinePlayersTest {
    private final UUID alice = UUID.randomUUID(), bob = UUID.randomUUID();
    private MachinePlayers<KenoRound> players() {
        return new MachinePlayers<>(() -> new KenoRound(new Random(1)));
    }

    @Test void reservesPreparationAndExcludesOtherPlayersUntilSettlement() {
        var players = players();
        assertTrue(players.claim(alice));
        var data = players.data(alice);
        data.round.action("select:1");
        assertFalse(players.claim(bob));
        players.update(false);
        assertEquals(alice, players.player()); // selection must survive until PLAY
        data.round.action("play");
        players.started();
        players.update(true); // reveal animation still running
        assertFalse(players.claim(bob));
        players.update(false);
        assertNull(players.player());
        assertTrue(players.claim(bob));
    }

    @Test void playerRoundsSelectionsAndFeedbackAreSeparate() {
        var players = players();
        var a = players.data(alice);
        var b = players.data(bob);
        a.round.action("select:7");
        a.feedback.launch(1, 1000);
        a.feedback.reveal(1, 1500);
        assertNotSame(a.round, b.round);
        assertTrue(b.round.selected().isEmpty());
        assertEquals(0, b.feedback.totalNet());
        assertNull(b.feedback.last());
        assertEquals(500, players.data(alice).feedback.totalNet());
        assertTrue(players.data(alice).round.selected().contains(7));
    }

    @Test void rejectedClicksCannotExtendAnAbandonedPlayersReservation() {
        var players = players();
        players.claim(alice);
        for (int i = 0; i < MachinePlayers.IDLE_TICKS; i++) {
            assertFalse(players.claim(bob));
            players.tick();
        }
        assertTrue(players.expired());
    }

    @Test void abandoningResetsUnfinishedDataButPreservesCompletedPersonalResults() {
        var players = players();
        players.claim(alice);
        var a = players.data(alice);
        a.round.action("select:7");
        a.feedback.launch(1, 1000);
        a.feedback.reveal(1, 1500);
        a.feedback.launch(2, 1000);
        players.abandon();
        assertNull(players.player());
        assertTrue(players.data(alice).round.selected().contains(7));
        assertFalse(players.data(alice).round.active());
        assertEquals(0, players.data(alice).round.sequence());
        assertEquals(0, players.data(alice).feedback.pending());
        assertEquals(500, players.data(alice).feedback.totalNet());
        assertTrue(players.claim(bob));
    }

    @Test void abandoningKeepsFreshPreferencesAndTotalsWhenSavedProfileIsStale() {
        var players = new MachinePlayers<>(() -> new KenoRound(new Random(1)),
                (id, data) -> data.feedback.restore(100, null));
        players.claim(alice);
        var a = players.data(alice);
        a.round.action("select:9");
        a.feedback.launch(1, 1000);
        a.feedback.reveal(1, 1500);
        a.feedback.launch(2, 1000);
        players.abandon();
        assertEquals(600, a.feedback.totalNet());
        assertEquals(0, a.feedback.pending());
        assertTrue(a.round.selected().contains(9));
        assertEquals(0, a.round.sequence());
    }

    @Test void plinkoBatchStaysExclusiveUntilAllBallsFinish() {
        var players = players();
        players.claim(alice);
        players.started();
        players.update(true);
        assertTrue(players.claim(alice));
        assertFalse(players.claim(bob));
        players.update(true);
        assertFalse(players.claim(bob));
        players.update(false);
        assertTrue(players.claim(bob));
    }
}
