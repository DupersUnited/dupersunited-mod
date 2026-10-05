package wtf.dupers.dupersunited.features.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.CosmeticsModule;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class CosmeticsFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final float MODEL_SCALE = 0.68f;
    private static boolean preview;

    public CosmeticsFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        CosmeticsModule module = MainClient.getModule(CosmeticsModule.class);
        if (module == null || (!module.isEnabled() && !preview) || mc.player == null || state.id != mc.player.getId() || state.isInvisible) return;

        renderHead(module, poseStack, submitNodeCollector, lightCoords, state.ageInTicks);
        renderTail(module, poseStack, submitNodeCollector, lightCoords, state.ageInTicks);
    }

    public static void preview(Runnable action) {
        preview = true;
        try {
            action.run();
        } finally {
            preview = false;
        }
    }

    private void renderHead(CosmeticsModule module, PoseStack matrices, SubmitNodeCollector queue, int light, float age) {
        CosmeticCatalog.Item item = module.selectedHead();
        if (isEmpty(item)) return;

        matrices.pushPose();
        getParentModel().root().translateAndRotate(matrices);
        getParentModel().head.translateAndRotate(matrices);
        matrices.translate(module.headX.getValue() / 16.0f,
                -0.16f - module.headY.getValue() / 16.0f,
                module.headZ.getValue() / 16.0f);
        rotate(matrices, module.headPitch.getValue(), module.headYaw.getValue(), module.headRoll.getValue());
        scale(matrices, module.headSize.getValue(), module.headWidth.getValue(), module.headStretch.getValue());

        if (item.id().equals("minecraft-duck")) {
            matrices.translate(0, -0.5f, 0);
            matrices.mulPose(Axis.ZP.rotationDegrees(90));
            matrices.scale(1, -1, 1);
        }

        if (module.animate.getValue()) {
            matrices.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(age * 0.08f) * 1.6f));
        }

        CosmeticRenderer.render(item, matrices, queue, light);
        matrices.popPose();
    }

    private void renderTail(CosmeticsModule module, PoseStack matrices, SubmitNodeCollector queue, int light, float age) {
        CosmeticCatalog.Item item = module.selectedTail();
        if (isEmpty(item)) return;

        matrices.pushPose();
        getParentModel().root().translateAndRotate(matrices);
        getParentModel().body.translateAndRotate(matrices);
        matrices.translate(module.tailX.getValue() / 16.0f,
                0.38f - module.tailY.getValue() / 16.0f,
                module.tailZ.getValue() / 16.0f);
        rotate(matrices, module.tailPitch.getValue(), module.tailYaw.getValue(), module.tailRoll.getValue());
        scale(matrices, module.tailSize.getValue(), module.tailWidth.getValue(), module.tailStretch.getValue());

        if (module.animate.getValue()) {
            matrices.mulPose(Axis.YP.rotationDegrees((float) Math.sin(age * 0.16f) * 12.0f));
        }

        CosmeticRenderer.render(item, matrices, queue, light);
        matrices.popPose();
    }

    private static boolean isEmpty(CosmeticCatalog.Item item) {
        return item.cubes().isEmpty() && item.model().isBlank();
    }

    private static void rotate(PoseStack matrices, int pitch, int yaw, int roll) {
        matrices.mulPose(Axis.XP.rotationDegrees(pitch));
        matrices.mulPose(Axis.YP.rotationDegrees(yaw));
        matrices.mulPose(Axis.ZP.rotationDegrees(roll));
    }

    private static void scale(PoseStack matrices, int sizeValue, int widthValue, int heightValue) {
        float size = MODEL_SCALE * sizeValue / 100.0f;
        float width = size * widthValue / 100.0f;
        float height = size * heightValue / 100.0f;
        matrices.scale(width, height, width);
    }
}
