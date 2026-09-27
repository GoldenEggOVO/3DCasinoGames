package dev.casino3d.machine;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;

/** Short vanilla motifs; tick-owned lifetime, no background tasks or external audio pack. */
final class MachineSounds {
    private record Note(int tick, Sound sound, float volume, float pitch) {}
    private final List<Note> notes = new ArrayList<>();
    private final Location origin;
    MachineSounds(Location origin) { this.origin = origin; }

    void play(Sound sound, float volume, float pitch) {
        for (var player : origin.getWorld().getNearbyPlayers(origin, 9))
            player.playSound(origin, sound, SoundCategory.BLOCKS, volume, pitch);
    }
    void start() {
        notes.clear();
        play(Sound.BLOCK_NOTE_BLOCK_BIT, .25f, .9f);
    }
    void result(FeedbackState.Result result, int tick) {
        switch (result.outcome()) {
            case LOSS -> play(Sound.BLOCK_NOTE_BLOCK_BASS, .3f, .7f);
            case EVEN -> play(Sound.BLOCK_NOTE_BLOCK_PLING, .22f, 1f);
            case WIN, BIG_WIN -> {
                boolean big = result.outcome() == FeedbackState.Outcome.BIG_WIN;
                float[] pitches = big ? new float[]{1f, 1.26f, 1.5f, 2f} : new float[]{1f, 1.26f, 1.5f};
                for (int i = 0; i < pitches.length; i++)
                    notes.add(new Note(tick + i * 3, Sound.BLOCK_NOTE_BLOCK_CHIME, big ? .35f : .25f, pitches[i]));
            }
        }
    }
    void tick(int tick) {
        for (var iterator = notes.iterator(); iterator.hasNext();) {
            var note = iterator.next();
            if (note.tick <= tick) { play(note.sound, note.volume, note.pitch); iterator.remove(); }
        }
    }
    void clear() { notes.clear(); }
}
