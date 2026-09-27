package dev.casino3d.machine;

import dev.casino3d.Language;
import dev.casino3d.ui.LabelLayout;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Small fixed cabinet readout; never creates an interaction hitbox. */
final class MachineScreen {
    private record Layout(double x, double y, double z, float width, float height) {}
    private final PracticeMachine<?> machine;
    private final Layout layout;
    private final TextDisplay header, value, footer;
    private Component previousHeader, previousValue, previousFooter;
    private FeedbackState.Result previousResult;
    private long previousLanguage = -1, previousStake = -1, previousTotal;
    private int previousPending = -1;
    private Long previousCashout;

    MachineScreen(PracticeMachine<?> machine) {
        this.machine = machine;
        layout = switch (machine.game()) {
            case "blackjack" -> new Layout(0, 1.47, -.748, 1.17f, .25f);
            case "mines" -> new Layout(0, 1.45, -1.15, 1.25f, .32f);
            case "crash" -> new Layout(0, .55, .65, 1.50f, .34f);
            case "plinko" -> new Layout(-1.30, .42, .48, 1.24f, .30f);
            case "slots" -> new Layout(0, .40, .565, 1.60f, .32f);
            case "duck_race" -> new Layout(1.10, .38, 1.73, 1.08f, .30f);
            case "wheel_of_fortune" -> new Layout(0, .63, .63, 1.10f, .36f);
            case "money_wheel" -> new Layout(0, 1.05, .64, 1.35f, .23f);
            case "penguin_cross" -> new Layout(0, 1.32, -.76, 1.25f, .32f);
            case "keno" -> new Layout(0, 1.12, -.87, 1.35f, .30f);
            case "hilo" -> new Layout(0, .31, .57, 1.40f, .30f);
            case "dragon_tower" -> new Layout(0, 2.61, .595, 1.17f, .19f);
            default -> throw new IllegalArgumentException("Unknown machine: " + machine.game());
        };
        slab(layout.width + .055f, layout.height + .055f, .055f, 0, Material.GRAY_CONCRETE);
        slab(layout.width, layout.height, .015f, .037f, Material.BLACK_CONCRETE);
        switch (machine.game()) {
            case "mines" -> stand(1.07);
            case "penguin_cross" -> stand(.94);
            case "keno" -> stand(.72);
            case "wheel_of_fortune", "money_wheel" -> {
                for (int sign : new int[] {-1, 1})
                    block(sign * .22, layout.y, .43, .08f, .065f, .40f, Material.GRAY_CONCRETE);
            }
            case "duck_race" -> {
                block(layout.x, .62, 1.63, .075f, .44f, .10f, Material.GRAY_CONCRETE);
                block(layout.x, .80, 1.21, .065f, .07f, .85f, Material.GRAY_CONCRETE);
            }
            default -> { }
        }
        header = machine.text(layout.x, layout.y + layout.height * .29, layout.z + .056, .1);
        value = machine.text(layout.x, layout.y - layout.height * .05, layout.z + .056, .2);
        footer = machine.text(layout.x, layout.y - layout.height * .35, layout.z + .056, .1);
    }
    private void stand(double bottom) {
        double top = layout.y - layout.height / 2;
        for (int sign : new int[] {-1, 1})
            block(layout.x + sign * layout.width * .32, (top + bottom) / 2, layout.z - .03,
                    .055f, (float) (top - bottom), .055f, Material.GRAY_CONCRETE);
    }
    private void slab(float width, float height, float depth, float z, Material material) {
        block(layout.x, layout.y, layout.z + z, width, height, depth, material);
    }
    private void block(double x, double y, double z, float width, float height, float depth, Material material) {
        machine.origin.getWorld().spawn(machine.at(x, y, z), BlockDisplay.class, d -> {
            machine.common(d);
            d.setBlock(material.createBlockData());
            machine.pose(d, new Transformation(new Vector3f(-width/2, -height/2, -depth/2),
                    new Quaternionf(), new Vector3f(width, height, depth), new Quaternionf()));
            d.setInterpolationDuration(0);
        });
    }
    private void label(TextDisplay display, Component text, float height) {
        var fit = LabelLayout.fit(text, layout.width * .94f, height);
        display.text(fit.text());
        machine.pose(display, new Transformation(new Vector3f(), new Quaternionf(),
                new Vector3f(fit.scale()), new Quaternionf()));
        display.setInterpolationDuration(0);
    }
    void update(FeedbackState state, long stake, Long cashout) {
        var result = state.last();
        if (previousLanguage == Language.revision() && previousStake == stake
                && previousPending == state.pending() && previousTotal == state.totalNet()
                && previousResult == result && java.util.Objects.equals(previousCashout, cashout)) return;
        previousLanguage = Language.revision(); previousStake = stake;
        previousPending = state.pending(); previousTotal = state.totalNet();
        previousResult = result; previousCashout = cashout;
        var top = result == null ? Language.component("feedback.bet", "amount", money(stake))
                : Language.component("feedback.return", "bet", money(result.stake()), "amount", money(result.returned()));
        var main = result != null ? Language.component("feedback." + result.outcome().name().toLowerCase(java.util.Locale.ROOT),
                "amount", signed(result.net()))
                : cashout != null ? Language.component("feedback.cashout", "amount", money(cashout))
                : Language.component(state.pending() > 0 ? "feedback.running" : "feedback.ready");
        var bottom = state.pending() > 1 ? Language.component("feedback.pending", "count", state.pending())
                : Language.component("feedback.session", "amount", signed(state.totalNet()));
        if (!top.equals(previousHeader)) { label(header, top, layout.height * .20f); previousHeader = top; }
        if (!main.equals(previousValue)) { label(value, main, layout.height * .28f); previousValue = main; }
        if (!bottom.equals(previousFooter)) { label(footer, bottom, layout.height * .16f); previousFooter = bottom; }
    }
    private static String money(long cents) { return String.format(java.util.Locale.ROOT, "%.2f", cents / 100.0); }
    private static String signed(long value) { return (value > 0 ? "+" : "") + money(value); }
}
