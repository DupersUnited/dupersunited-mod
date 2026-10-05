package wtf.dupers.dupersunited.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.FullBrightModule;

@Mixin(Lightmap.class)
public class LightmapMixin {
    @ModifyExpressionValue(method = "getBrightness", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;ambientLight()F"))
    private static float dupersunited$fullbrightAmbient(float original) {
        FullBrightModule mod = MainClient.MODULE_MANAGER.getModule(FullBrightModule.class);
        if (mod != null && mod.isEnabled() && mod.mode.getValue().equals("Ambient")) {
            return 1.0f;
        }
        return original;
    }
}