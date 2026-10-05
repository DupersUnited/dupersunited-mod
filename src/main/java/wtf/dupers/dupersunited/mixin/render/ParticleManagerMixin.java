package wtf.dupers.dupersunited.mixin.render;

import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.NoRenderModule;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleManagerMixin {

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void dupersunited$skipParticlesWhenDisabled(ParticlesRenderState particlesRenderState, Frustum frustum, Camera camera, float partialTickTime, CallbackInfo ci) {
        NoRenderModule mod = MainClient.MODULE_MANAGER.getModule(NoRenderModule.class);
        if (mod == null || !mod.isEnabled()) {
            return;
        }
        if (!mod.particles.getValue()) {
            return;
        }
        ci.cancel();
    }
}