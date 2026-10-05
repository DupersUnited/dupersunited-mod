package wtf.dupers.dupersunited.mixin.misc;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.FreecamModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static wtf.dupers.dupersunited.MainClient.mc;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void onHandleInput(CallbackInfo ci) {
        FreecamModule freecam = MainClient.MODULE_MANAGER.getModule(FreecamModule.class);
        if (freecam != null && freecam.isEnabled()) {
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyShift.setDown(false);
        }
    }
}