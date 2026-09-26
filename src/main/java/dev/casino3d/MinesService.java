package dev.casino3d;

import static dev.casino3d.Language.text;
import dev.casino3d.economy.Wallet;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

final class MinesService {
    private final RoundStore store;
    private final Wallet wallet;
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, MinesRound> current = new HashMap<>();
    private boolean storageFailed;

    MinesService(RoundStore store, Wallet wallet) throws IOException {
        this.store = store;
        this.wallet = wallet;
        for (var r : store.load()) {
            var prior = current.get(r.player);
            if (prior != null && !prior.finished() && !r.finished())
                throw new IOException(text("storage.multiple-rounds", "player", r.player));
            if (prior == null
                    || (!r.finished() && prior.finished())
                    || (r.finished() == prior.finished() && r.created > prior.created))
                current.put(r.player, r);
        }
    }

    MinesRound get(UUID player) {
        var r = current.get(player);
        return r == null ? null : r.copy();
    }

    private void save(MinesRound r) throws IOException {
        if (storageFailed) throw new IOException("error.storage");
        try {
            store.save(r);
            current.put(r.player, r);
        } catch (IOException ex) {
            storageFailed = true;
            throw ex;
        }
    }

    MinesRound start(UUID p, long stake, int mines, boolean practice) throws IOException {
        if (!practice && !wallet.available()) throw new IllegalStateException("error.economy-practice");
        var old = current.get(p);
        if (old != null && !old.finished())
            throw new IllegalStateException("error.unfinished");
        int[] cells = new int[25];
        for (int i = 0; i < 25; i++) cells[i] = i;
        for (int i = 24; i > 0; i--) {
            int j = random.nextInt(i + 1), t = cells[i];
            cells[i] = cells[j];
            cells[j] = t;
        }
        int mask = 0;
        if (mines < 1 || mines > 24) throw new IllegalArgumentException("error.mines");
        for (int i = 0; i < mines; i++) mask |= 1 << cells[i];
        var r = MinesRound.create(p, stake, mines, practice, mask);
        save(r);
        // 崩溃或不确定的第三方结果保留 pending，绝不自动再次扣款。
        boolean paid = practice || wallet.take(p, stake);
        r = r.copy();
        r.phase = paid ? MinesRound.Phase.ACTIVE : MinesRound.Phase.CANCELLED;
        save(r);
        if (!paid) throw new IllegalStateException("error.debit-not-started");
        return r.copy();
    }

    MinesRound reveal(UUID p, UUID id, int revision, int cell) throws IOException {
        var r = active(p, id, revision);
        r.reveal(cell);
        save(r);
        if (r.allSafe()) return cashout(p, r.id, r.revision);
        return r.copy();
    }

    MinesRound cashout(UUID p, UUID id, int revision) throws IOException {
        var r = active(p, id, revision);
        r.payoutDue = r.payout();
        r.phase = MinesRound.Phase.CREDIT_PENDING;
        r.revision++;
        save(r);
        boolean paid = r.practice || wallet.give(p, r.payoutDue);
        r = r.copy();
        r.phase = paid ? MinesRound.Phase.PAID : MinesRound.Phase.ACTIVE;
        save(r);
        if (!paid) throw new IllegalStateException("error.payout-retry");
        return r.copy();
    }

    private MinesRound active(UUID p, UUID id, int revision) {
        var r = current.get(p);
        if (r == null
                || !r.id.equals(id)
                || r.revision != revision
                || r.phase != MinesRound.Phase.ACTIVE)
            throw new IllegalStateException("error.reopen");
        return r.copy();
    }

    void resolve(UUID p, UUID id, boolean applied) throws IOException {
        var r = current.get(p);
        if (r == null || !r.id.equals(id)) throw new IllegalArgumentException("error.round-mismatch");
        r = r.copy();
        if (r.phase == MinesRound.Phase.DEBIT_PENDING)
            r.phase = applied ? MinesRound.Phase.ACTIVE : MinesRound.Phase.CANCELLED;
        else if (r.phase == MinesRound.Phase.CREDIT_PENDING)
            r.phase = applied ? MinesRound.Phase.PAID : MinesRound.Phase.ACTIVE;
        else throw new IllegalStateException("error.no-review");
        r.revision++;
        save(r);
    }
}
