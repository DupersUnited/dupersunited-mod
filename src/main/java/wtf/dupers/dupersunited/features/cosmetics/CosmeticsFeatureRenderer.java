package wtf.dupers.dupersunited.features.cosmetics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.CosmeticsModule;

public final class CosmeticsFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final float MODEL_SCALE = 0.68f;
    private static boolean preview;

    public CosmeticsFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        CosmeticsModule module = MainClient.getModule(CosmeticsModule.class);
        MinecraftClient client = MinecraftClient.getInstance();
        if (module == null
                || (!module.isEnabled() && !preview)
                || client.player == null
                || state.id != client.player.getId()
                || state.invisible) {
            return;
        }

        renderHead(module, matrices, queue, light, state.age);
        renderTail(module, matrices, queue, light, state.age);
    }

    public static void preview(Runnable action) {
        preview = true;
        try {
            action.run();
        } finally {
            preview = false;
        }
    }

    private void renderHead(CosmeticsModule module, MatrixStack matrices,
                            OrderedRenderCommandQueue queue, int light, float age) {
        CosmeticCatalog.Item item = module.selectedHead();
        if (isEmpty(item)) return;

        matrices.push();
        getContextModel().getRootPart().applyTransform(matrices);
        getContextModel().head.applyTransform(matrices);
        matrices.translate(module.headX.getValue() / 16.0f,
                -0.16f - module.headY.getValue() / 16.0f,
                module.headZ.getValue() / 16.0f);
        rotate(matrices, module.headPitch.getValue(), module.headYaw.getValue(), module.headRoll.getValue());
        scale(matrices, module.headSize.getValue(), module.headWidth.getValue(), module.headStretch.getValue());

        if (item.id().equals("minecraft-duck")) {
            matrices.translate(0, -0.5f, 0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
            matrices.scale(1, -1, 1);
        }

        if (module.animate.getValue()) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(age * 0.08f) * 1.6f));
        }

        CosmeticRenderer.render(item, matrices, queue, light);
        matrices.pop();
    }

    private void renderTail(CosmeticsModule module, MatrixStack matrices,
                            OrderedRenderCommandQueue queue, int light, float age) {
        CosmeticCatalog.Item item = module.selectedTail();
        if (isEmpty(item)) return;

        matrices.push();
        getContextModel().getRootPart().applyTransform(matrices);
        getContextModel().body.applyTransform(matrices);
        matrices.translate(module.tailX.getValue() / 16.0f,
                0.38f - module.tailY.getValue() / 16.0f,
                module.tailZ.getValue() / 16.0f);
        rotate(matrices, module.tailPitch.getValue(), module.tailYaw.getValue(), module.tailRoll.getValue());
        scale(matrices, module.tailSize.getValue(), module.tailWidth.getValue(), module.tailStretch.getValue());

        if (module.animate.getValue()) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) Math.sin(age * 0.16f) * 12.0f));
        }

        CosmeticRenderer.render(item, matrices, queue, light);
        matrices.pop();
    }

    private static boolean isEmpty(CosmeticCatalog.Item item) {
        return item.cubes().isEmpty() && item.model().isBlank();
    }

    private static void rotate(MatrixStack matrices, int pitch, int yaw, int roll) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
    }

    private static void scale(MatrixStack matrices, int sizeValue, int widthValue, int heightValue) {
        float size = MODEL_SCALE * sizeValue / 100.0f;
        float width = size * widthValue / 100.0f;
        float height = size * heightValue / 100.0f;
        matrices.scale(width, height, width);
    }
}
