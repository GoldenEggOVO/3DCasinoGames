package dev.casino3d;

import java.util.List;

/** Stable identifiers shared by the test-machine menu and command dispatcher. */
final class MachineCatalog {
    record Entry(String id, String labelKey) {
        String label() { return Language.text(labelKey); }
    }

    static final List<Entry> ENTRIES =
            List.of(
                    new Entry("plinko", "games.plinko"),
                    new Entry("mines", "games.mines"),
                    new Entry("blackjack", "games.blackjack"),
                    new Entry("crash", "games.crash"),
                    new Entry("slots", "games.slots"),
                    new Entry("duck_race", "games.duck_race"),
                    new Entry("wheel_of_fortune", "games.wheel_of_fortune"),
                    new Entry("money_wheel", "games.money_wheel"),
                    new Entry("penguin_cross", "games.penguin_cross"),
                    new Entry("keno", "games.keno"),
                    new Entry("hilo", "games.hilo"),
                    new Entry("dragon_tower", "games.dragon_tower"));

    private MachineCatalog() {}
}
