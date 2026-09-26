package dev.server.casino.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Original resource geometry expressed in physical blocks at ItemDisplay scale four. */
public record VanillaGeometry(List<Box> boxes, List<Label> labels) {
    public record Box(float[] from, float[] to, String material, float roll, float pitch, float[] matrix) {}
    public record Label(String text, float[] position, float width, float height, Integer color) {}

    private static final NamespacedKey MODEL_KEY = new NamespacedKey("casino", "vanilla_model");
    private static final Map<String, VanillaGeometry> MODELS = load();

    private static Map<String, VanillaGeometry> load() {
        try (var stream = VanillaGeometry.class.getResourceAsStream("/vanilla-models.json")) {
            if (stream == null) throw new IllegalStateException("Missing vanilla-models.json");
            return Map.copyOf(new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, VanillaGeometry>>() {}.getType()));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot load vanilla models", e);
        }
    }

    public static VanillaGeometry get(String name) {
        return name == null ? null : MODELS.get(name);
    }

    public static void mark(ItemStack item, String reference) {
        if (!reference.startsWith("casino:")) return;
        String name = reference.substring(7);
        if (!MODELS.containsKey(name)) return;
        item.editMeta(meta -> meta.getPersistentDataContainer()
                .set(MODEL_KEY, PersistentDataType.STRING, name));
    }

    public static String name(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(MODEL_KEY, PersistentDataType.STRING);
    }

    /** Map the vanilla unit cube into physical model space; JSON matrices are row-major. */
    public static Matrix4f boxMatrix(Box box) {
        if (box.matrix() != null) return new Matrix4f().set(box.matrix()).transpose();
        float[] a = box.from(), b = box.to();
        return new Matrix4f().translation((a[0]+b[0])/2, (a[1]+b[1])/2, (a[2]+b[2])/2)
                .rotateZ(box.roll()).rotateX(box.pitch())
                .translate((a[0]-b[0])/2, (a[1]-b[1])/2, (a[2]-b[2])/2)
                .scale(b[0]-a[0], b[1]-a[1], b[2]-a[2]);
    }

    public BoundingBox bounds() {
        BoundingBox result = null;
        for (var box : boxes) {
            var matrix = boxMatrix(box);
            for (int i = 0; i < 8; i++) {
                var p = matrix.transformPosition(new Vector3f(i & 1, (i >> 1) & 1, (i >> 2) & 1));
                if (result == null) result = new BoundingBox(p.x, p.y, p.z, p.x, p.y, p.z);
                else result.union(p.x, p.y, p.z);
            }
        }
        return result;
    }

    /** Include the ItemDisplay renderer's extra Y half-turn, then physical-to-item units. */
    public static Matrix4f matrix(Transformation pose) {
        return new Matrix4f().translation(pose.getTranslation()).rotate(pose.getLeftRotation())
                .scale(pose.getScale()).rotate(pose.getRightRotation())
                .rotateY((float) Math.PI).scale(.25f);
    }
}
