package wtf.dupers.dupersunited.mixin.network;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.misc.BrandSpoofModule;
import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientBrandRetriever.class, priority = 2000)
public class ClientBrandRetrieverMixin {

    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true)
    private static void dupersunited$spoofBrand(CallbackInfoReturnable<String> cir) {
        if (MainClient.MODULE_MANAGER.isEnabled(BrandSpoofModule.class)) {
            cir.setReturnValue("vanilla");
        }
    }
}