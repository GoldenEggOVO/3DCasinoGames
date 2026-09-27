package dev.casino3d.game.mines;

import dev.casino3d.Language;
import dev.casino3d.MachineGeometry;
import dev.casino3d.machine.*;
import dev.casino3d.model.MachineDefinition;
import dev.casino3d.model.ButtonDefinition;
import dev.casino3d.model.ModelTransform;

import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;

import java.security.SecureRandom;
import java.util.*;

public final class MinesMachine extends PracticeMachine<MinesDemoRound> {
    private final List<ItemDisplay> cells = new ArrayList<>();
    private final Map<ItemDisplay, String> faces = new HashMap<>();
    private final Map<ItemDisplay, Flip> flips = new HashMap<>();
    private TextDisplay setting;
    private final boolean refined;
    private static final ModelTransform CONSOLE = new ModelTransform(0, .67, 1.96, -35, 0, 0, 1);

    public MinesMachine(
            MachineManager manager, UUID owner, Location origin, MachineDefinition definition) {
        super(manager, owner, origin, definition, new MinesDemoRound(new SecureRandom()));
        refined = definition.equals(MachineDefinition.builtin("mines"));
    }

    public static ButtonDefinition vanillaButton(String action) {
        boolean small = action.equals("minus") || action.equals("plus");
        double x = (action.equals("minus") || action.equals("start") ? -1 : 1) * (small ? .78 : .60);
        var point = CONSOLE.apply(x, small ? .51 : .075, .065);
        return ButtonDefinition.at(point.x(), point.y(), point.z(), small ? .31 : 1.03, -35, small ? .34 : .86);
    }

    @Override
    protected void buildGame() {
        body(refined ? "cabinet_mines_refined" : "cabinet_mines");
        String[] actions = {"minus", "plus", "start", "cash"};
        String[] models = {"minus", "plus", "play", "cashout"};
        for (int i = 0; i < actions.length; i++)
            button(actions[i], "cabinet_button_" + models[i], refined ? vanillaButton(actions[i]) : definition.button(actions[i]));
        for (int i = 0; i < 25; i++) {
            var point = MachineGeometry.mineCell(i);
            var cell = model("mine_hidden", point.x(), refined ? 1.115 : point.y(), point.z(), .32);
            if (refined) pose(cell, cellPose(0));
            cells.add(cell);
            faces.put(cell, "mine_hidden");
            if (refined) hitDisplay("cell:" + i, cell, new BoundingBox(-2, -2, -2, 2, 2, 2), -1);
            else hit("cell:" + i, cell, point.x(), point.y() - .16, point.z(), .43, .33, -1);
        }
        if (refined) {
            var point = CONSOLE.apply(0, .56, .04);
            setting = text(point.x(), point.y(), point.z(), .30);
            pose(setting, new org.bukkit.util.Transformation(new org.joml.Vector3f(), CONSOLE.rotation(),
                    new org.joml.Vector3f(.30f), new org.joml.Quaternionf()));
        } else setting = text(-.55, 1.43, 1.62, .23);
    }

    private org.bukkit.util.Transformation cellPose(double pitch) {
        var pose = MachineGeometry.itemPose(.32, pitch, 0);
        if (refined) pose.getScale().set(.48f, .13f, .46f);
        return pose;
    }

    @Override
    protected Long cashoutAmount() { return round.active() ? (round.mines() != null && round.mines().safeCount() > 0 ? round.mines().payout() : null) : null; }

    @Override
    protected boolean available(String action) {
        return switch (action) {
            case "minus" -> !round.active() && round.mineCount() > 1;
            case "plus" -> !round.active() && round.mineCount() < 24;
            case "start" -> !round.active();
            case "cash" -> round.active() && round.mines().safeCount() > 0;
            default ->
                    action.startsWith("cell:")
                            && round.active()
                            && (round.mines().revealed()
                                            & (1 << Integer.parseInt(action.substring(5))))
                                    == 0;
        };
    }

    @Override
    protected void action(String action) {
        switch (action) {
            case "minus" -> round.changeMines(-1);
            case "plus" -> round.changeMines(1);
            case "start" -> round.start(System.currentTimeMillis());
            case "cash" -> round.cash(System.currentTimeMillis());
            default -> round.reveal(Integer.parseInt(action.substring(5)));
        }
        refresh();
    }

    @Override
    protected void refresh() {
        var state = round.mines();
        for (int i = 0; i < cells.size(); i++) {
            boolean shown =
                    state != null && ((state.revealed() & (1 << i)) != 0 || !round.active());
            String face =
                    !shown
                            ? "mine_hidden"
                            : (state.mask() & (1 << i)) != 0 ? "mine_bomb" : "mine_gem";
            var cell = cells.get(i);
            if (!face.equals(faces.put(cell, face))) flips.put(cell, new Flip(face, age));
        }
        setting.text(Language.component("mines.count", "count", round.mineCount()));
    }

    @Override
    protected boolean busy() { return round.finished() && !flips.isEmpty(); }

    @Override
    protected boolean revealing() { return !flips.isEmpty(); }

    @Override
    protected void animate() {
        for (var iterator = flips.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            var flip = entry.getValue();
            double t = Math.min(1, (age - flip.start) / 8.0);
            if (age - flip.start == 4 && flip.model.equals("mine_gem") && round.active())
                sound(Sound.BLOCK_NOTE_BLOCK_PLING, .2f, 1.15f);
            if (t >= .5) entry.getKey().setItemStack(model(flip.model));
            pose(entry.getKey(), cellPose(Math.sin(t * Math.PI) * 1.3));
            if (t >= 1) iterator.remove();
        }
    }

    private record Flip(String model, int start) {}
}
