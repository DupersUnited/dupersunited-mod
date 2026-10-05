package wtf.dupers.dupersunited.mixin.render;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.FullBrightModule;

@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
    @Inject(method = "extract", at = @At("TAIL"))
    private void dupersunited$fullbright(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        FullBrightModule mod = MainClient.MODULE_MANAGER.getModule(FullBrightModule.class);
        if (mod == null || !mod.isEnabled()) return;

        if (mod.mode.getValue().equals("Gamma")) {
            state.brightness = 4.0f;
        } else if (mod.mode.getValue().equals("Ambient")) {
            state.ambientColor = new Vector3f(0.8f, 0.8f, 0.8f);
        }
    }
}