package dev.casino3d;

import static dev.casino3d.Amounts.money;
import static dev.casino3d.Language.text;

import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/** Finishes existing persisted rounds; never starts new menu games. */
final class RoundRecoveryMenu {
    private final CasinoMenus menus;
    private final CasinoService games;

    RoundRecoveryMenu(CasinoMenus menus, CasinoService games) {
        this.menus = menus;
        this.games = games;
    }

    boolean pending(Player player) {
        var round = games.get(player.getUniqueId());
        return round != null && !round.finished();
    }

    private interface Checked {
        void run() throws IOException;
    }

    private void checked(Checked action) {
        try {
            action.run();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    void open(Player p) {
        checked(
                () -> {
                    var r = games.advance(p.getUniqueId());
                    if (r == null) {
                        menus.open(p);
                        return;
                    }
                    var selected = r.game;
                    boolean active = !r.finished();
                    String body =
                            text("menu.recovery.body", "mode", text(r.practice ? "menu.recovery.practice" : "menu.recovery.coins"),
                                    "amount", money(r.stake), "result", result(r));
                    var page = menus.new Page(p, name(selected), body);
                    page.artwork(
                            (r.phase == CasinoRound.Phase.ACTIVE
                                            || r.finished()
                                            || r.phase == CasinoRound.Phase.CREDIT_READY)
                                    ? CasinoGraphics.board(r, System.currentTimeMillis())
                                    : List.of());
                    if (active) {
                        if (r.phase == CasinoRound.Phase.ACTIVE
                                && r.game == CasinoRound.Game.BLACKJACK) {
                            page.button("hit", text("menu.blackjack.hit"), v -> act(p, r, "hit"));
                            page.button("stand", text("menu.blackjack.stand"), v -> act(p, r, "stand"));
                            if (r.hand.size() == 2 && r.stake <= 5000)
                                page.button(
                                        "double",
                                        text("menu.blackjack.double", "amount", money(r.stake)),
                                        v -> {
                                            var confirm =
                                                    menus
                                                    .new Page(
                                                            p,
                                                            text("menu.blackjack.confirm-double"),
                                                            text("menu.blackjack.double-body", "amount", money(r.stake),
                                                                    "currency", text(r.practice ? "menu.currency.practice" : "menu.currency.coins"),
                                                                    "total", money(r.stake * 2)));
                                            confirm.button(
                                                    "confirm",
                                                    text("menu.blackjack.confirm-double"),
                                                    values -> act(p, r, "double"));
                                            confirm.button("back", text("menu.back"), values -> open(p));
                                            confirm.show();
                                        });
                        } else if (r.phase == CasinoRound.Phase.ACTIVE
                                && r.game == CasinoRound.Game.CRASH) {
                            page.crash = true;
                            page.button("cash", text("menu.crash.cash-out"), v -> act(p, r, "cash"));
                        } else if (r.phase == CasinoRound.Phase.CREDIT_READY)
                            page.button("retry", text("menu.recovery.retry"), v -> act(p, r, "retry"));
                        page.button("refresh", text("menu.recovery.refresh"), v -> open(p));
                    }
                    page.button(
                            "rules",
                            text("menu.recovery.rules"),
                            v -> {
                                var rules =
                                        menus
                                        .new Page(
                                                p, text("menu.rules.title", "game", name(selected)), description(selected));
                                rules.button("back", text("menu.recovery.back-game"), v2 -> open(p));
                                rules.show();
                            });
                    page.button("back", text("menu.recovery.back-casino"), v -> menus.open(p));
                    page.show();
                });
    }

    private void act(Player p, CasinoRound round, String action) {
        checked(
                () -> {
                    games.action(p.getUniqueId(), round.id, round.revision, action);
                    open(p);
                });
    }

    static String name(CasinoRound.Game game) {
        return switch (game) {
            case DICE -> text("menu.game.dice");
            case BLACKJACK -> text("menu.game.blackjack");
            case PLINKO -> text("menu.game.plinko");
            case LIMBO -> text("menu.game.limbo");
            case CRASH -> text("menu.game.crash");
        };
    }

    private static String result(CasinoRound r) {
        if (r.phase == CasinoRound.Phase.DEBIT_PENDING
                || r.phase == CasinoRound.Phase.DOUBLE_PENDING
                || r.phase == CasinoRound.Phase.CREDIT_PENDING)
            return text("menu.recovery.pending-review", "player", r.player, "round", r.id);
        String outcome =
                switch (r.game) {
                    case DICE -> text("menu.result.dice", "roll", money(r.roll), "target", r.parameter);
                    case LIMBO -> text("menu.result.limbo", "multiplier", money(r.point), "target", money(r.parameter));
                    case PLINKO ->
                            text("menu.result.plinko", "path", path(r.path), "slot", Integer.bitCount(r.path) + 1);
                    case BLACKJACK ->
                            text("menu.result.blackjack", "cards", CasinoRules.cards(r.hand),
                                    "total", CasinoRules.total(r.hand),
                                    "dealer", r.finished() || r.phase == CasinoRound.Phase.CREDIT_READY
                                            ? text("menu.result.dealer-revealed", "cards", CasinoRules.cards(r.dealer),
                                                    "total", CasinoRules.total(r.dealer))
                                            : text("menu.result.dealer-hidden", "cards", CasinoRules.cards(r.dealer.subList(0, 1))));
                    case CRASH ->
                            r.finished()
                                    ? text("menu.result.crash-finished", "multiplier", money(r.point), "target", money(r.parameter))
                                    : text("menu.result.crash-running", "target", money(r.parameter));
                };
        if (r.finished())
            outcome +=
                    "\n"
                            + (r.phase == CasinoRound.Phase.CANCELLED
                                    ? text("menu.result.cancelled")
                                    : text("menu.result.finished", "amount", money(r.payout),
                                            "currency", text(r.practice ? "menu.currency.practice" : "menu.currency.coins")));
        if (r.phase == CasinoRound.Phase.CREDIT_READY)
            outcome += text("menu.result.credit-ready", "amount", money(r.payout));
        return outcome;
    }

    static String path(int path) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 12; i++) s.append((path & (1 << i)) == 0 ? "↙" : "↘");
        return s.toString();
    }

    static String description(CasinoRound.Game game) {
        return switch (game) {
            case DICE -> text("menu.rules.dice");
            case LIMBO -> text("menu.rules.limbo");
            case PLINKO -> text("menu.rules.plinko", "table", plinkoTable());
            case CRASH -> text("menu.rules.crash");
            case BLACKJACK -> text("menu.rules.blackjack");
        };
    }

    private static String plinkoTable() {
        List<String> amounts = new ArrayList<>();
        for (int i = 0; i <= 12; i++) amounts.add(money(CasinoRules.plinkoPayout(100, i)));
        return String.join(" | ", amounts);
    }
}
