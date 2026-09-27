package dev.casino3d;

import dev.casino3d.machine.FeedbackState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeedbackStateTest {
    @Test void resultsStayHiddenUntilRevealedAndAreConsumedOnce() {
        var state = new FeedbackState();
        state.launch(1L, 1000);
        assertEquals(1, state.pending());
        assertNull(state.last());
        assertEquals(-900, state.reveal(1L, 100).net());
        assertEquals(FeedbackState.Outcome.LOSS, state.last().outcome());
        assertNull(state.reveal(1L, 100));
        assertEquals(-900, state.totalNet());
        state.launch(2L, 1000);
        assertNull(state.last());
        assertEquals(0, state.reveal(2L, 1000).net());
    }
    @Test void doubleDownUsesActualTotalStakeAndKeepsCompletedResultStable() {
        var state = new FeedbackState();
        state.launch(1L, 1000);
        state.reviseStake(1L, 2000);
        assertEquals(2000, state.reveal(1L, 4000).net());
        state.reviseStake(1L, 100);
        assertEquals(2000, state.last().stake());
    }
    @Test void parallelBallsRetainTheirOwnStakeAndCanLandOutOfOrder() {
        var state = new FeedbackState();
        state.launch("a", 1000);
        state.launch("b", 2000);
        assertEquals(1000, state.reveal("b", 3000).net());
        assertEquals(1, state.pending());
        assertEquals(-1000, state.reveal("a", 0).net());
        assertEquals(0, state.pending());
        assertEquals(0, state.totalNet());
        assertNull(state.reveal("b", 3000));
    }
    @Test void soundOutcomeUsesNetRatherThanPositiveReturn() {
        assertEquals(FeedbackState.Outcome.LOSS, new FeedbackState.Result(100, 10).outcome());
        assertEquals(FeedbackState.Outcome.EVEN, new FeedbackState.Result(100, 100).outcome());
        assertEquals(FeedbackState.Outcome.WIN, new FeedbackState.Result(100, 200).outcome());
        assertEquals(FeedbackState.Outcome.BIG_WIN, new FeedbackState.Result(100, 500).outcome());
    }
}
