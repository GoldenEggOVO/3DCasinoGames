package dev.casino3d;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.*;

class CasinoGraphicsTest {
    @Test
    void dealerHiddenCardNeverLeaksIntoGraphics() {
        var r = new CasinoRound();
        r.game = CasinoRound.Game.BLACKJACK;
        r.phase = CasinoRound.Phase.ACTIVE;
        r.hand = List.of(1, 2);
        r.dealer = List.of(3, 4);
        String board = String.join("", CasinoGraphics.board(r, 0));
        assertTrue(board.contains("[?]"));
        assertFalse(board.contains(CasinoRules.cards(List.of(4))));
        r.phase = CasinoRound.Phase.PAID;
        assertTrue(
                String.join("", CasinoGraphics.board(r, 0))
                        .contains(CasinoRules.cards(List.of(4))));
    }

    @Test
    void graphicsNeverEndInNegativeAdvance() {
        for (var game : CasinoRound.Game.values()) {
            var r = new CasinoRound();
            r.game = game;
            r.phase = CasinoRound.Phase.PAID;
            r.hand = List.of(1, 2);
            r.dealer = List.of(3, 4);
            r.parameter = 200;
            r.point = 250;
            for (String line : CasinoGraphics.board(r, 0)) {
                String plain = line.replaceAll("<[^>]*>", "");
                assertFalse(plain.codePoints().anyMatch(c -> Character.getType(c) == Character.PRIVATE_USE));
                assertFalse(line.contains("<font:"));
            }
        }
    }

    @Test
    void plinkoShowsThirteenRowsAndOnePathPointPerRow() {
        var r = new CasinoRound();
        r.game = CasinoRound.Game.PLINKO;
        r.path = 0b101010101010;
        var rows = CasinoGraphics.board(r, 0);
        assertEquals(13, rows.size());
        for (String row : rows) assertEquals(1, row.chars().filter(c -> c == 'o').count());
    }
}
