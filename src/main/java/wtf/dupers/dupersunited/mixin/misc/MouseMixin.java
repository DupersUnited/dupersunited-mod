package wtf.dupers.dupersunited.mixin.misc;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.misc.BetterTabModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.keyPlayerList.isDown()) {
            BetterTabModule module = MainClient.MODULE_MANAGER.getModule(BetterTabModule.class);
            if (module != null && module.isEnabled()) {
                module.onMouseScroll(vertical);
                ci.cancel(); // whatever this is surely not gonna be an issue haha if it is then idfk vinzys fault maybe
            }
        }
    }
}