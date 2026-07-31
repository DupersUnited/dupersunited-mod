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
    private static final Map<String,GlbCosmeticModel> MODELS=new HashMap<>();
    private static final int[][] FACES = {{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}};
    private static final float[][] NORMALS = {{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};

    private CosmeticRenderer() {}

    public static void render(CosmeticCatalog.Item item, MatrixStack matrices, OrderedRenderCommandQueue queue, int light) {
        if(!item.model().isBlank()){MODELS.computeIfAbsent(item.model(),GlbCosmeticModel::load).render(matrices,queue,light);return;}
        if (item.cubes().isEmpty()) return;
        queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(TEXTURE), (entry, vertices) -> item.cubes().forEach(c -> cube(entry, vertices, c, light)));
    }

    private static void cube(MatrixStack.Entry entry, VertexConsumer out, CosmeticCatalog.Cube c, int light) {
        float[][] p={{c.x1(),c.y1(),c.z1()},{c.x2(),c.y1(),c.z1()},{c.x2(),c.y2(),c.z1()},{c.x1(),c.y2(),c.z1()},{c.x1(),c.y1(),c.z2()},{c.x2(),c.y1(),c.z2()},{c.x2(),c.y2(),c.z2()},{c.x1(),c.y2(),c.z2()}};
        for(int f=0;f<6;f++){ int[] q=FACES[f]; vertex(entry,out,p[q[0]],0,0,NORMALS[f],c.color(),light); vertex(entry,out,p[q[1]],1,0,NORMALS[f],c.color(),light); vertex(entry,out,p[q[2]],1,1,NORMALS[f],c.color(),light); vertex(entry,out,p[q[3]],0,1,NORMALS[f],c.color(),light); }
    }

    private static void vertex(MatrixStack.Entry e, VertexConsumer v, float[] p, float u, float t, float[] n, int color, int light) {
        v.vertex(e,p[0],p[1],p[2]).color(color).texture(u,t).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e,n[0],n[1],n[2]);
    }
}
