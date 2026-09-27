package dev.casino3d;

import static dev.casino3d.Language.text;
import java.util.ArrayList;
import java.util.List;

/** Recovery-menu snapshots use only vanilla text; hidden cards remain hidden. */
final class CasinoGraphics {
    private CasinoGraphics() {}

    static List<String> board(CasinoRound r, long now) {
        List<String> out = new ArrayList<>();
        switch (r.game) {
            case BLACKJACK -> {
                out.add(text("graphics.dealer"));
                out.add(r.finished() || r.phase == CasinoRound.Phase.CREDIT_READY
                        ? CasinoRules.cards(r.dealer)
                        : CasinoRules.cards(r.dealer.subList(0, 1)) + " [?]");
                out.add(text("graphics.hand", "total", CasinoRules.total(r.hand)));
                out.add(CasinoRules.cards(r.hand));
            }
            case PLINKO -> {
                int column = 12;
                for (int y = 0; y <= 12; y++) {
                    StringBuilder line = new StringBuilder();
                    for (int x = 0; x < 25; x++)
                        line.append(x == column ? 'o' : x >= 12 - y && x <= 12 + y
                                && (x - (12 - y)) % 2 == 0 ? '.' : ' ');
                    out.add(line.toString());
                    if (y < 12) column += (r.path & (1 << y)) == 0 ? -1 : 1;
                }
            }
            case DICE -> {
                int selected = r.roll * 25 / 10000;
                out.add("-".repeat(selected) + "|" + "-".repeat(24 - selected));
                out.add("0           50          100");
            }
            case LIMBO -> out.add(Amounts.money(r.point) + "x");
            case CRASH -> {
                int multiplier = r.finished() ? r.point
                        : Math.min(r.parameter, CasinoRules.crashMultiplier(r.started, now));
                out.add(Amounts.money(multiplier) + "x");
                out.add(text("graphics.crash-snapshot"));
            }
        }
        return out;
    }
}
