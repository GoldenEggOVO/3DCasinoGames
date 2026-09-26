package dev.casino3d.machine;

import static org.junit.jupiter.api.Assertions.*;

import dev.casino3d.model.VanillaGeometry;
import org.junit.jupiter.api.Test;

class VanillaDisplayTest {
    @Test
    void dragonRevealsCanChangeMaterialWithoutReplacingDisplayEntities() {
        var hidden = VanillaGeometry.get("dragon_tile_hidden");
        var safe = VanillaGeometry.get("dragon_tile_safe");
        var trap = VanillaGeometry.get("dragon_tile_trap");

        assertTrue(VanillaDisplay.sameBlockGeometry(hidden, safe));
        assertTrue(VanillaDisplay.sameBlockGeometry(hidden, trap));
        assertFalse(VanillaDisplay.sameBlockGeometry(hidden,
                VanillaGeometry.get("showcase_dragon_tower_compact")));
    }

    @Test
    void kenoSelectionCanRecolorInPlaceButCardFacesNeedNewGeometry() {
        assertTrue(VanillaDisplay.sameBlockGeometry(VanillaGeometry.get("showcase_tile"),
                VanillaGeometry.get("showcase_tile_selected")));
        assertFalse(VanillaDisplay.sameBlockGeometry(VanillaGeometry.get("card_52"),
                VanillaGeometry.get("card_47")));
    }

    @Test
    void hidingAModelNeverSendsNonFiniteTransformComponents() throws Exception {
        var display = (org.bukkit.entity.Display) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{org.bukkit.entity.Display.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "setTransformationMatrix" -> {
                        assertTrue(((org.joml.Matrix4f) arguments[0]).isFinite());
                        yield null;
                    }
                    case "getTransformation" -> new org.bukkit.util.Transformation(
                            new org.joml.Vector3f(), new org.joml.Quaternionf(),
                            new org.joml.Vector3f(), new org.joml.Quaternionf());
                    case "setTransformation" -> {
                        assertTrue(((org.bukkit.util.Transformation) arguments[0]).getScale().isFinite());
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        var apply = VanillaDisplay.class.getDeclaredMethod("applyPose",
                org.bukkit.entity.Display.class, org.joml.Matrix4f.class);
        apply.setAccessible(true);
        apply.invoke(null, display, new org.joml.Matrix4f().scale(0));
    }
}
