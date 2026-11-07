package wtf.dupers.dupersunited.mixin.glitcha;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.exploit.AnySignModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
public class AbstractSignEditScreenMixin {
    @Inject(method = "lambda$new$1", at = @At("HEAD"), cancellable = true)
    private void dupersunited$allowAnySignText(SignBlockEntity sign, String s, CallbackInfoReturnable<Boolean> cir) {
        if (MainClient.MODULE_MANAGER.isEnabled(AnySignModule.class)) {
            cir.setReturnValue(true);
        }
    }
}