package dev.casino3d.machine;

import dev.casino3d.game.keno.KenoRound;
import dev.casino3d.game.hilo.HiloRound;
import dev.casino3d.game.mines.MinesDemoRound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PersonalDataStoreTest {
    @TempDir Path folder;
    private final UUID alice = UUID.randomUUID(), bob = UUID.randomUUID(), owner = UUID.randomUUID();

    @Test void preferencesAndSettledResultsSurviveRestartWithoutSharingOrResumingRounds() throws Exception {
        var keno = new KenoRound(new Random(1));
        keno.action("select:7");
        keno.action("play");
        var feedback = new FeedbackState();
        feedback.launch(1, 1000); feedback.reveal(1, 1400);
        feedback.launch(2, 1000); // an unfinished game must not be restored
        var store = new PersonalDataStore(folder);
        store.save(alice, owner, "keno", PersonalDataStore.Profile.capture(keno, feedback));
        var loaded = new PersonalDataStore(folder);
        var restored = new KenoRound(new Random(2));
        var restoredFeedback = new FeedbackState();
        loaded.load(alice, owner, "keno").restore(restored, restoredFeedback);
        assertEquals(java.util.Set.of(7), restored.selected());
        assertFalse(restored.active());
        assertFalse(restored.finished());
        assertEquals(0, restored.sequence());
        assertEquals(400, restoredFeedback.totalNet());
        assertEquals(0, restoredFeedback.pending());
        assertNull(loaded.load(bob, owner, "keno"));
        assertNull(loaded.load(alice, UUID.randomUUID(), "keno"));
        assertNull(loaded.load(alice, owner, "hilo"));
    }

    @Test void savesHiloDirectionThresholdAndMinesCount() throws Exception {
        var hilo = new HiloRound(new Random(1)); hilo.setThreshold(23); hilo.action("under");
        var mines = new MinesDemoRound(new Random(1)); mines.changeMines(5);
        var store = new PersonalDataStore(folder);
        store.save(alice, owner, "hilo", PersonalDataStore.Profile.capture(hilo, new FeedbackState()));
        store.save(alice, owner, "mines", PersonalDataStore.Profile.capture(mines, new FeedbackState()));
        store = new PersonalDataStore(folder);
        var restoredHilo = new HiloRound(new Random(2));
        store.load(alice, owner, "hilo").restore(restoredHilo, new FeedbackState());
        assertEquals(23, restoredHilo.threshold()); assertFalse(restoredHilo.high());
        var restoredMines = new MinesDemoRound(new Random(2));
        store.load(alice, owner, "mines").restore(restoredMines, new FeedbackState());
        assertEquals(8, restoredMines.mineCount());
    }

    @Test void corruptFileIsReportedAndNeverOverwritten() throws Exception {
        Files.writeString(folder.resolve(alice + ".json"), "broken original");
        var store = new PersonalDataStore(folder);
        assertThrows(java.io.IOException.class, () -> store.load(alice, owner, "keno"));
        assertThrows(java.io.IOException.class, () -> store.save(alice, owner, "keno",
                PersonalDataStore.Profile.capture(new KenoRound(new Random()), new FeedbackState())));
        assertEquals("broken original", Files.readString(folder.resolve(alice + ".json")));
    }
}
