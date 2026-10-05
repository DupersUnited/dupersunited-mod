package wtf.dupers.dupersunited.mixin.glitcha;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.FreeLookModule;
import wtf.dupers.dupersunited.modules.render.FreecamModule;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouse(CallbackInfo ci) {
        FreecamModule freecam = MainClient.MODULE_MANAGER.getModule(FreecamModule.class);
        if (freecam != null && freecam.isEnabled()) {
            freecam.changeLookDirection(accumulatedDX * 0.15, accumulatedDY * 0.15);
            ci.cancel();
            return;
        }

        FreeLookModule freeLook = MainClient.MODULE_MANAGER.getModule(FreeLookModule.class);
        if (freeLook == null || !freeLook.isPlayerMode()) return;

        freeLook.cameraYaw += (float) (accumulatedDX * freeLook.sensitivity.getValue() * 0.15);
        freeLook.smoothYaw = freeLook.cameraYaw;
        ci.cancel();
    }
}