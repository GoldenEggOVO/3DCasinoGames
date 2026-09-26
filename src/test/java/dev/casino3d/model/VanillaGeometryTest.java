package dev.casino3d.model;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VanillaGeometryTest {
    @Test
    void rowMajorAffinePartsRetainTranslationShearAndRotatedBounds() {
        var part = new VanillaGeometry.Box(new float[]{0,0,0}, new float[]{1,1,1},
                "STONE", 0, 0, new float[]{0,-2,.5f,10, 3,0,0,20, 0,0,4,30, 0,0,0,1});
        var matrix = VanillaGeometry.boxMatrix(part);
        var point = matrix.transformPosition(new org.joml.Vector3f(1,1,1));
        assertEquals(8.5, point.x, 1e-6);
        assertEquals(23, point.y, 1e-6);
        assertEquals(34, point.z, 1e-6);
        var bounds = new VanillaGeometry(java.util.List.of(part), java.util.List.of()).bounds();
        assertEquals(8, bounds.getMinX(), 1e-6);
        assertEquals(10.5, bounds.getMaxX(), 1e-6);
        assertEquals(20, bounds.getMinY(), 1e-6);
        assertEquals(23, bounds.getMaxY(), 1e-6);
        assertEquals(30, bounds.getMinZ(), 1e-6);
        assertEquals(34, bounds.getMaxZ(), 1e-6);
    }

    @Test
    void compositePoseMatchesResourceItemCoordinatesIncludingPitchAndNonuniformScale() {
        var pose = dev.casino3d.MachineGeometry.itemPose(4, -Math.PI / 2, .2);
        var point = VanillaGeometry.matrix(pose).transformPosition(new org.joml.Vector3f(1, 2, 3));
        assertEquals(1, point.x, 1e-5);
        assertEquals(3, point.y, 1e-5);
        assertEquals(-1.8, point.z, 1e-5);
        pose.getScale().set(8, 4, 2);
        point = VanillaGeometry.matrix(pose).transformPosition(new org.joml.Vector3f(1, 2, 3));
        assertEquals(2, point.x, 1e-5);
        assertEquals(1.5, point.y, 1e-5);
        assertEquals(-1.8, point.z, 1e-5);
    }

    @Test
    void cardCornersArePhysicalHalfTurnPairsAndPipsOccupyReadableArea() {
        for (int index = 0; index < 52; index++) {
            var boxes = VanillaGeometry.get("card_" + index).boxes();
            var topCorner = boxes.stream().filter(b -> b.to()[0] < -1.2 && b.from()[1] > 1.1).toList();
            assertFalse(topCorner.isEmpty());
            // Greedy rectangles can partition the same mirrored silhouette differently.
            for (double x = -1.87; x < -1.25; x += .02)
                for (double y = 1.15; y < 2.45; y += .02)
                    assertEquals(inkAt(boxes, x, y), inkAt(boxes, -x, -y), "Inverted corner: " + index);
        }
        // 9 diamonds: the left pip column must be visibly wider than the previous tiny glyphs.
        var diamonds = VanillaGeometry.get("card_47").boxes().stream()
                .filter(b -> b.material().equals("RED_CONCRETE") && b.from()[0] > -1.2 && b.to()[0] < -.4)
                .toList();
        double width = diamonds.stream().mapToDouble(b -> b.to()[0]).max().orElseThrow()
                - diamonds.stream().mapToDouble(b -> b.from()[0]).min().orElseThrow();
        assertTrue(width * .43 / 4 >= .055, "Pip is too small at the actual .43 display scale");
        assertNotNull(VanillaGeometry.get("card_52"));
    }

    private boolean inkAt(java.util.List<VanillaGeometry.Box> boxes, double x, double y) {
        return boxes.stream().anyMatch(b -> !b.material().equals("WHITE_CONCRETE")
                && x >= b.from()[0] && x < b.to()[0] && y >= b.from()[1] && y < b.to()[1]);
    }

    @Test
    void shippedVanillaBodiesHaveUsablePhysicalBounds() throws Exception {
        try (var stream = getClass().getResourceAsStream("/vanilla-models.json")) {
            assertNotNull(stream, "Original model geometry must be included in the runtime JAR");
            var models = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            for (String game : MachineDefinition.games()) {
                String name = (java.util.Set.of("blackjack", "crash", "mines", "plinko").contains(game)
                        ? "cabinet_" : "showcase_") + game;
                var model = models.getAsJsonObject(name);
                assertNotNull(model, game);
                var bounds = VanillaGeometry.get(name).bounds();
                assertTrue(Double.isFinite(bounds.getMinX()) && Double.isFinite(bounds.getMaxX()), game);
                assertTrue(bounds.getWidthX() > 0 && bounds.getHeight() > 0 && bounds.getWidthZ() > 0, game);
                assertTrue(bounds.getMinY() >= -.01, game + " must rest on the floor");
            }
        }
    }
}
