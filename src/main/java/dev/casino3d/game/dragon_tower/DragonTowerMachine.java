package dev.casino3d.game.dragon_tower;

import dev.casino3d.MachineGeometry;
import dev.casino3d.ShowcaseGeometry;
import dev.casino3d.machine.*;
import dev.casino3d.model.MachineDefinition;
import dev.casino3d.model.ButtonDefinition;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.BoundingBox;

import java.security.SecureRandom;
import java.util.*;

/** Physical display and animation for dragon_tower; the round owns all game rules. */
public final class DragonTowerMachine extends AnimatedMachine<DragonTowerRound> {

    final List<ItemDisplay> tiles = new ArrayList<>();
    private final boolean compact;

    public DragonTowerMachine(
            MachineManager manager, UUID owner, Location origin, MachineDefinition definition) {
        super(manager, owner, origin, definition, new DragonTowerRound(new SecureRandom()));
        compact = vanillaAppearance() && definition.equals(MachineDefinition.builtin("dragon_tower"));
    }

    public static ButtonDefinition vanillaButton(String action) {
        return ButtonDefinition.at(action.equals("play") ? -.33 : .33, .12, .63, .60, -10, 1);
    }

    public static MachineGeometry.Point vanillaCell(int row, int column) {
        var point = ShowcaseGeometry.dragonCell(row, column);
        return new MachineGeometry.Point(point.x(), point.y()-.5, point.z());
    }

    @Override
    protected List<Double> settingsBounds() {
        return compact ? List.of(-.9, 0.0, -.32, .9, 2.85, .84) : super.settingsBounds();
    }

    @Override
    protected void buildGame() {
        body(compact ? "showcase_dragon_tower_compact" : "showcase_dragon_tower");
        for (String action : List.of("play", "cash"))
            button(action, "showcase_button_" + action,
                    compact ? vanillaButton(action) : definition.button(action));
        for (int row = 0; row < 6; row++)
            for (int column = 0; column < 4; column++) {
                var point = compact ? vanillaCell(row, column) : ShowcaseGeometry.dragonCell(row, column);
                var tile =
                        item(
                                compact ? model("dragon_tile_hidden") : new ItemStack(Material.GRAY_TERRACOTTA),
                                point.x(),
                                point.y(),
                                point.z(),
                                compact ? 4 : .28,
                                0);
                pose(tile, dragonPose(column));
                tiles.add(tile);
                hitDisplay("select:" + column, tile, compact
                        ? new BoundingBox(-.14, -.14, -.14, .14, .14, .14)
                        : new BoundingBox(-2.01, -2.01, -2.01, 2.01, 2.01, 2.01), row);
            }
    }

    @Override
    protected boolean available(String action) {
        return round.available(action);
    }

    @Override
    protected void perform(String action) {
        round.action(action);
    }

    @Override
    protected void refresh() {
        for (int i = 0; i < tiles.size(); i++) {
            int row = i / 4, column = i % 4;
            boolean revealed = row < round.traps().size();
            tiles.get(i).setItemStack(compact
                            ? model(!revealed ? "dragon_tile_hidden"
                                    : round.traps().get(row) == column ? "dragon_tile_trap" : "dragon_tile_safe")
                            :
                            new ItemStack(
                                    revealed
                                            ? (round.traps().get(row) == column
                                                    ? Material.TNT
                                                    : Material.EMERALD_BLOCK)
                                            : Material.GRAY_TERRACOTTA));
            pose(tiles.get(i), dragonPose(column));
            tiles.get(i).setGlowing(round.active() && row == round.floors());
            tiles.get(i).setGlowColorOverride(Color.YELLOW);
        }
    }

    @Override
    protected void animateFrame(double progress, double ease) {}

    Transformation dragonPose(int column) {
        var pose = MachineGeometry.itemPose(compact ? 4 : .28, 0, 0);
        pose.getLeftRotation().rotateY((float) ShowcaseGeometry.dragonAngle(column));
        return pose;
    }

    @Override
    protected boolean shouldAnimate(String action) {
        return false;
    }

    @Override
    protected boolean rowAvailable(int row) {
        return row < 0 || row == round.floors();
    }
}
