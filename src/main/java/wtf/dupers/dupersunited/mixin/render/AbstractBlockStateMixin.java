package wtf.dupers.dupersunited.mixin.render;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.NoTextureRotationsModule;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AbstractBlockStateMixin {

    @Unique
    private static final long UNIFORM_RENDERING_SEED = 67L;

    @Inject(method = "getSeed", at = @At("HEAD"), cancellable = true)
    private void dupersunited$uniformRenderingSeed(BlockPos pos, CallbackInfoReturnable<Long> cir) {
        NoTextureRotationsModule mod = MainClient.MODULE_MANAGER.getModule(NoTextureRotationsModule.class);
        if (mod != null && mod.isEnabled()) {
            cir.setReturnValue(UNIFORM_RENDERING_SEED);
        }
    }
}