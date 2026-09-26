package dev.casino3d;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommandCompletionTest {
    private List<String> player(String... args) {
        return CommandCompletion.player(args, List.of("mines", "blackjack", "hilo"),
                List.of("mines", "blue_mines"), List.of("hilo", "mines"));
    }

    @Test void filtersCaseInsensitiveAndNeverOffersPlayerNames() {
        assertEquals(List.of("create"), player("CR"));
        assertEquals(List.of("blackjack"), player("create", "B"));
        assertEquals(List.of(), player("unknown", ""));
        assertEquals(List.of(), player("reload-models", ""));
        assertEquals(List.of(), player("create", "mines", "mines", ""));
    }

    @Test void completionUsesOnlySuppliedSkinsAndOwnedGames() {
        assertEquals(List.of("blue_mines", "mines"), player("create", "mines", ""));
        assertEquals(List.of("hilo", "mines"), player("remove", ""));
        assertEquals(List.of("mines"), player("bet", "m"));
        assertEquals(List.of("1", "10", "100"), player("bet", "mines", "1"));
        assertEquals(List.of(), player("remove", "mines", ""));
    }

    @Test void consoleOnlyCompletesSettlementCommandsAndOutcomePosition() {
        assertEquals(List.of("mines", "resolve"), CommandCompletion.console(new String[]{""}));
        assertEquals(List.of("resolve"), CommandCompletion.console(new String[]{"mines", "r"}));
        assertEquals(List.of(), CommandCompletion.console(new String[]{"resolve", ""}));
        assertEquals(List.of("not-applied"), CommandCompletion.console(new String[]{"resolve", "uuid", "round", "n"}));
        assertEquals(List.of("applied", "not-applied"), CommandCompletion.console(new String[]{"mines", "resolve", "uuid", "round", ""}));
        assertEquals(List.of(), CommandCompletion.console(new String[]{"create", ""}));
    }
}
