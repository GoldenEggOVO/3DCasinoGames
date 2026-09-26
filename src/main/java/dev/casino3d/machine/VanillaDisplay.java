package dev.casino3d.machine;

import dev.casino3d.model.VanillaGeometry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** A hidden item retains the existing animation/input API; its vanilla parts follow its pose. */
final class VanillaDisplay {
    private record Part(Display display, Matrix4f local) {}
    private final ItemDisplay carrier;
    private final Consumer<Entity> register;
    private final List<Entity> ownerParts;
    private final List<Part> children = new ArrayList<>();
    private String name;
    private Matrix4f previous;
    private Location location;
    private boolean glowing;
    private Color glowColor;

    VanillaDisplay(ItemDisplay carrier, Consumer<Entity> register, List<Entity> ownerParts) {
        this.carrier = carrier;
        this.register = register;
        this.ownerParts = ownerParts;
        carrier.setVisibleByDefault(false);
        sync();
    }

    boolean sync() {
        if (!carrier.isValid()) {
            remove();
            return false;
        }
        var matrix = VanillaGeometry.matrix(carrier.getTransformation());
        var nextLocation = carrier.getLocation();
        String nextName = VanillaGeometry.name(carrier.getItemStack());
        if (!Objects.equals(name, nextName)) {
            var model = VanillaGeometry.get(nextName);
            if (sameBlockGeometry(VanillaGeometry.get(name), model)
                    && children.size() == model.boxes().size()
                    && children.stream().allMatch(part -> part.display instanceof BlockDisplay)) {
                for (int i = 0; i < children.size(); i++)
                    ((BlockDisplay) children.get(i).display)
                            .setBlock(Material.valueOf(model.boxes().get(i).material()).createBlockData());
                name = nextName;
            } else {
                remove();
                name = nextName;
                carrier.setVisibleByDefault(model == null);
                if (model != null) build(model, matrix);
                previous = matrix;
                location = nextLocation;
                glowing = carrier.isGlowing();
                glowColor = carrier.getGlowColorOverride();
            }
        }
        boolean moved = !nextLocation.equals(location);
        boolean transformed = !matrix.equals(previous);
        boolean glowChanged = previous == null || glowing != carrier.isGlowing()
                || !Objects.equals(glowColor, carrier.getGlowColorOverride());
        for (var part : children) {
            if (moved) part.display.teleport(nextLocation);
            if (transformed) {
                part.display.setInterpolationDuration(1);
                applyPose(part.display, new Matrix4f(matrix).mul(part.local));
                part.display.setInterpolationDelay(0);
            }
            if (glowChanged) {
                part.display.setGlowing(carrier.isGlowing());
                part.display.setGlowColorOverride(carrier.getGlowColorOverride());
            }
        }
        previous = matrix;
        location = nextLocation;
        glowing = carrier.isGlowing();
        glowColor = carrier.getGlowColorOverride();
        return true;
    }

    static boolean sameBlockGeometry(VanillaGeometry first, VanillaGeometry second) {
        if (first == null || second == null || !first.labels().isEmpty()
                || !second.labels().isEmpty() || first.boxes().size() != second.boxes().size()) return false;
        for (int i = 0; i < first.boxes().size(); i++) {
            var a = first.boxes().get(i);
            var b = second.boxes().get(i);
            if (!Arrays.equals(a.from(), b.from()) || !Arrays.equals(a.to(), b.to())
                    || !Arrays.equals(a.matrix(), b.matrix())
                    || a.roll() != b.roll() || a.pitch() != b.pitch()) return false;
        }
        return true;
    }

    private static void applyPose(Display display, Matrix4f matrix) {
        // The server's SVD has an absolute epsilon: tiny stretched facets lose
        // shear. Decompose at a larger scale, then send the corrected components.
        float length = matrix.getScale(new Vector3f()).length();
        float factor = length == 0 ? 1 : 64f / length;
        display.setTransformationMatrix(new Matrix4f(matrix).scale(factor));
        var pose = display.getTransformation();
        pose.getScale().div(factor);
        display.setTransformation(pose);
    }

    private void initialize(Display display, Matrix4f pose) {
        register.accept(display);
        // Set the complete pose before the spawn packet; a new part has no prior pose to animate.
        applyPose(display, pose);
        display.setInterpolationDuration(0);
        display.setGlowing(carrier.isGlowing());
        display.setGlowColorOverride(carrier.getGlowColorOverride());
    }

    private void build(VanillaGeometry model, Matrix4f parent) {
        for (var box : model.boxes()) {
            var local = VanillaGeometry.boxMatrix(box);
            var display = carrier.getWorld().spawn(carrier.getLocation(), BlockDisplay.class, d -> {
                initialize(d, new Matrix4f(parent).mul(local));
                d.setBlock(Material.valueOf(box.material()).createBlockData());
            });
            children.add(new Part(display, local));
        }
        for (int index = 0; index < model.labels().size(); index++) {
            var label = model.labels().get(index);
            String translated = dev.casino3d.Language.modelLabel(name, index, label.text());
            float size = Math.min(label.height() / .2f, label.width() / (Math.max(1, translated.length()) * .15f));
            float[] p = label.position();
            var local = new Matrix4f().translation(p[0], p[1] - .125f*size, p[2]).scale(size);
            var display = carrier.getWorld().spawn(carrier.getLocation(), TextDisplay.class, d -> {
                initialize(d, new Matrix4f(parent).mul(local));
                d.setBillboard(Display.Billboard.FIXED);
                d.setDefaultBackground(false);
                d.setBackgroundColor(Color.fromARGB(0));
                d.setLineWidth(1000);
                d.text(Component.text(translated, TextColor.color(label.color() == null ? 0xf6edcf : label.color())));
            });
            children.add(new Part(display, local));
        }
    }

    private void remove() {
        for (var part : children) {
            ownerParts.remove(part.display);
            part.display.remove();
        }
        children.clear();
    }
}
