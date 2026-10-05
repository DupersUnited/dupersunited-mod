package wtf.dupers.dupersunited.features.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class CosmeticRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");
    private static final Map<String, GlbCosmeticModel> MODELS = new HashMap<>();
    private static final int[][] FACES = {
            {0, 1, 2, 3},
            {5, 4, 7, 6},
            {4, 0, 3, 7},
            {1, 5, 6, 2},
            {3, 2, 6, 7},
            {4, 5, 1, 0}
    };
    private static final float[][] NORMALS = {
            {0, 0, -1},
            {0, 0, 1},
            {-1, 0, 0},
            {1, 0, 0},
            {0, 1, 0},
            {0, -1, 0}
    };
    private static final float[][] TEXTURE_COORDINATES = {
            {0, 0},
            {1, 0},
            {1, 1},
            {0, 1}
    };

    private CosmeticRenderer() {}

    public static void render(CosmeticCatalog.Item item, PoseStack poseStack, SubmitNodeCollector queue, int light) {
        if (!item.model().isBlank()) {
            MODELS.computeIfAbsent(item.model(), GlbCosmeticModel::load).render(poseStack, queue, light);
            return;
        }

        if (item.cubes().isEmpty()) return;

        queue.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (entry, vertices) -> {
            for (CosmeticCatalog.Cube cube : item.cubes()) {
                renderCube(entry, vertices, cube, light);
            }
        });
    }

    private static void renderCube(PoseStack.Pose entry, VertexConsumer vertices,
                                   CosmeticCatalog.Cube cube, int light) {
        float[][] points = {
                {cube.x1(), cube.y1(), cube.z1()},
                {cube.x2(), cube.y1(), cube.z1()},
                {cube.x2(), cube.y2(), cube.z1()},
                {cube.x1(), cube.y2(), cube.z1()},
                {cube.x1(), cube.y1(), cube.z2()},
                {cube.x2(), cube.y1(), cube.z2()},
                {cube.x2(), cube.y2(), cube.z2()},
                {cube.x1(), cube.y2(), cube.z2()}
        };

        for (int face = 0; face < FACES.length; face++) {
            for (int vertex = 0; vertex < FACES[face].length; vertex++) {
                writeVertex(entry, vertices, points[FACES[face][vertex]],
                        TEXTURE_COORDINATES[vertex], NORMALS[face], cube.color(), light);
            }
        }
    }

    private static void writeVertex(PoseStack.Pose entry, VertexConsumer vertices, float[] point,
                                    float[] texture, float[] normal, int color, int light) {
        vertices.addVertex(entry, point[0], point[1], point[2])
                .setColor(color)
                .setUv(texture[0], texture[1])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, normal[0], normal[1], normal[2]);
    }
}
