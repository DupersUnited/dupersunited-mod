package wtf.dupers.dupersunited.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import wtf.dupers.dupersunited.modules.render.BlockEspModule;

import static wtf.dupers.dupersunited.MainClient.mc;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void renderBlockEsp(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        BlockEspModule esp = BlockEspModule.getInstance();
        if (esp == null || !esp.isEnabled()) return;
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = levelRenderState.cameraRenderState.pos;
        esp.onRender(poseStack, submitNodeCollector, camPos);
    }
}   