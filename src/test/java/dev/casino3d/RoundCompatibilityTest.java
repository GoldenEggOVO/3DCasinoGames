package dev.casino3d;

import static org.junit.jupiter.api.Assertions.*;

import dev.casino3d.game.PracticeRound;
import dev.casino3d.game.blackjack.BlackjackRound;
import dev.casino3d.game.crash.CrashRound;
import dev.casino3d.game.dragon_tower.DragonTowerRound;
import dev.casino3d.game.duck_race.DuckRaceRound;
import dev.casino3d.game.hilo.HiloRound;
import dev.casino3d.game.keno.KenoRound;
import dev.casino3d.game.mines.MinesDemoRound;
import dev.casino3d.game.money_wheel.MoneyWheelRound;
import dev.casino3d.game.penguin_cross.PenguinCrossRound;
import dev.casino3d.game.slots.SlotsRound;
import dev.casino3d.game.wheel_of_fortune.WheelOfFortuneRound;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

/** Differential checks against the frozen preview rules, including random call order. */
class RoundCompatibilityTest {
    @Test
    void allShowcaseActionsMatchPreviewAcrossSeedsAndStakes() throws Exception {
        for (String kind :
                List.of(
                        "slots",
                        "duck_race",
                        "wheel_of_fortune",
                        "money_wheel",
                        "penguin_cross",
                        "dragon_tower",
                        "keno",
                        "hilo")) {
            for (int seed = 0; seed < 30; seed++) {
                FrozenShowcaseRound original = new FrozenShowcaseRound(kind, new Random(seed));
                PracticeRound current = create(kind, new Random(seed));
                long stake = seed % 2 == 0 ? 333 : 2400;
                original.setStake(stake);
                current.setStake(stake);
                for (int turn = 0; turn < 15; turn++) {
                    for (String action :
                            List.of(
                                    "select:1",
                                    "select:2",
                                    "next",
                                    "previous",
                                    "over",
                                    "under",
                                    "flip",
                                    "play",
                                    "step",
                                    "select:0",
                                    "select:3",
                                    "cash",
                                    "invalid")) {
                        assertEquals(
                                original.available(action),
                                current.getClass()
                                        .getMethod("available", String.class)
                                        .invoke(current, action),
                                kind);
                        original.action(action);
                        current.getClass()
                                .getMethod("action", String.class)
                                .invoke(current, action);
                        check(original, current);
                        if (current instanceof WheelOfFortuneRound fortune) {
                            original.replayFortune();
                            fortune.replayFortune();
                            check(original, current);
                        }
                    }
                }
            }
        }
    }

    @Test
    void blackjackDeckActionsAndSettlementMatchPreview() {
        for (int seed = 0; seed < 300; seed++) {
            FrozenCasinoDemoRound original =
                    new FrozenCasinoDemoRound("blackjack", new Random(seed));
            BlackjackRound current = new BlackjackRound(new Random(seed));
            original.setConfiguredStake(3500);
            current.setConfiguredStake(3500);
            for (int turn = 0; turn < 4; turn++) {
                original.start(turn);
                current.start(turn);
                assertEquals(original.deck, current.deck());
                assertEquals(original.player, current.player());
                assertEquals(original.dealer, current.dealer());
                switch (turn) {
                    case 0 -> {
                        original.doubleDown();
                        current.doubleDown();
                    }
                    case 1 -> {
                        original.hit();
                        current.hit();
                        original.doubleDown();
                        current.doubleDown();
                    }
                    case 2 -> {
                        original.hit();
                        current.hit();
                        original.hit();
                        current.hit();
                    }
                    default -> {}
                }
                original.stand();
                current.stand();
                assertEquals(original.player, current.player());
                assertEquals(original.dealer, current.dealer());
                assertEquals(original.next, current.cardsDrawn());
                assertEquals(original.stake, current.stake());
                assertEquals(original.payout, current.payout());
                assertEquals(localizedDemoResult(original.result), current.result());
                assertEquals(original.active, current.active());
            }
        }
    }

    @Test
    void minesBoardImmediateRevealsAndSettlementMatchPreview() {
        for (int seed = 0; seed < 100; seed++) {
            FrozenCasinoDemoRound original = new FrozenCasinoDemoRound("mines", new Random(seed));
            MinesDemoRound current = new MinesDemoRound(new Random(seed));
            original.changeMines(seed % 24 - 2);
            current.changeMines(seed % 24 - 2);
            original.start(0);
            current.start(0);
            assertEquals(original.mines.mask, current.mines().mask());
            assertEquals(original.cash(0), current.cash(0));
            for (int cell = 0; cell < 25; cell++) {
                original.reveal(cell);
                current.reveal(cell);
                assertEquals(original.mines.revealed, current.mines().revealed());
                assertEquals(original.mines.phase, current.mines().phase());
                if (seed % 2 == 0) {
                    assertEquals(original.cash(0), current.cash(0));
                }
                assertEquals(original.payout, current.payout());
                assertEquals(original.active, current.active());
                assertEquals(localizedDemoResult(original.result), current.result());
            }
        }
    }

    @Test
    void crashCollectionAndFlightMatchPreviewAtBoundaries() {
        for (int seed = 0; seed < 100; seed++) {
            FrozenCasinoDemoRound original = new FrozenCasinoDemoRound("crash", new Random(seed));
            CrashRound current = new CrashRound(new Random(seed));
            original.start(100);
            current.start(100);
            assertEquals(original.point, current.crashPoint());
            for (long time : new long[] {99, 100, 129, 130, 400, 700, 12100, 999999}) {
                assertEquals(original.cash(time), current.cash(time));
                original.tick(time);
                current.tick(time);
                assertEquals(original.value, current.multiplier());
                assertEquals(original.cashed, current.cashed());
                assertEquals(original.payout, current.payout());
                assertEquals(original.active, current.active());
                assertEquals(localizedDemoResult(original.result), current.result());
            }
        }
    }

    /** Only presentation changed; retain an explicit comparison of every result message. */
    private static String localizedDemoResult(String original) {
        if (original.equals("点击开始 · 每局 10 练习金币")) return Language.text("demo.idle");
        if (original.equals("进行中")) return Language.text("demo.playing");
        if (original.equals("本局结束 · 点击再来一局")) return Language.text("demo.finished");
        var payout = java.util.regex.Pattern.compile("练习返还 ([0-9.]+) · 点击再来一局").matcher(original);
        if (payout.matches()) return Language.text("demo.payout", "amount", payout.group(1));
        var collected = java.util.regex.Pattern.compile("已领取 ([0-9.]+) · 等待爆点").matcher(original);
        if (collected.matches()) return Language.text("crash.collected", "amount", collected.group(1));
        throw new AssertionError("Unmapped frozen result: " + original);
    }

    private static PracticeRound create(String kind, Random random) {
        return switch (kind) {
            case "slots" -> new SlotsRound(random);
            case "duck_race" -> new DuckRaceRound(random);
            case "wheel_of_fortune" -> new WheelOfFortuneRound(random);
            case "money_wheel" -> new MoneyWheelRound(random);
            case "penguin_cross" -> new PenguinCrossRound(random);
            case "dragon_tower" -> new DragonTowerRound(random);
            case "keno" -> new KenoRound(random);
            case "hilo" -> new HiloRound(random);
            default -> throw new AssertionError(kind);
        };
    }

    private static void check(FrozenShowcaseRound original, PracticeRound current) {
        assertEquals(original.active, current.active(), original.kind);
        assertEquals(original.finished, current.finished(), original.kind);
        assertEquals(original.payout, current.payout(), original.kind);
        assertEquals(original.result, current.result(), original.kind);
        switch (current) {
            case SlotsRound slots -> assertEquals(original.values, slots.symbols());
            case DuckRaceRound duck -> {
                assertEquals(original.value, duck.winner());
                assertEquals(original.choice, duck.choice());
            }
            case WheelOfFortuneRound fortune -> assertEquals(original.value, fortune.segment());
            case MoneyWheelRound money -> {
                assertEquals(original.value, money.segment());
                assertEquals(original.choice, money.choice());
            }
            case PenguinCrossRound penguin -> {
                assertEquals(original.stage, penguin.steps());
                assertEquals(original.value, penguin.lastRoll());
            }
            case DragonTowerRound tower -> {
                assertEquals(original.stage, tower.floors());
                assertEquals(original.value, tower.lane());
                assertEquals(original.values, tower.traps());
            }
            case KenoRound keno -> {
                assertEquals(original.selected, keno.selected());
                assertEquals(original.values, keno.drawnNumbers());
                assertEquals(original.value, keno.hits());
            }
            case HiloRound hilo -> {
                assertEquals(original.value, hilo.threshold());
                assertEquals(original.high, hilo.high());
                assertEquals(original.values, hilo.rolls());
            }
            default -> throw new AssertionError(current.getClass());
        }
    }
}
