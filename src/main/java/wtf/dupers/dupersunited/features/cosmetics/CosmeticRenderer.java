package wtf.dupers.dupersunited.features.cosmetics;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class CosmeticRenderer {
    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/block/white_concrete.png");
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

    public static void render(CosmeticCatalog.Item item, MatrixStack matrices,
                              OrderedRenderCommandQueue queue, int light) {
        if (!item.model().isBlank()) {
            MODELS.computeIfAbsent(item.model(), GlbCosmeticModel::load).render(matrices, queue, light);
            return;
        }

        if (item.cubes().isEmpty()) return;

        queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(TEXTURE), (entry, vertices) -> {
            for (CosmeticCatalog.Cube cube : item.cubes()) {
                renderCube(entry, vertices, cube, light);
            }
        });
    }

    private static void renderCube(MatrixStack.Entry entry, VertexConsumer vertices,
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

    private static void writeVertex(MatrixStack.Entry entry, VertexConsumer vertices, float[] point,
                                    float[] texture, float[] normal, int color, int light) {
        vertices.vertex(entry, point[0], point[1], point[2])
                .color(color)
                .texture(texture[0], texture[1])
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, normal[0], normal[1], normal[2]);
    }
}
