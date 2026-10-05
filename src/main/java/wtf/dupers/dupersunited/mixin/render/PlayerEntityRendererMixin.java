package wtf.dupers.dupersunited.mixin.render;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dupers.dupersunited.features.cosmetics.CosmeticsFeatureRenderer;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<
    AbstractClientPlayer,
    AvatarRenderState,
    PlayerModel
> {
    protected PlayerEntityRendererMixin(
        EntityRendererProvider.Context context,
        PlayerModel model,
        float shadowRadius
    ) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addCosmetics(EntityRendererProvider.Context context, boolean slimSteve, CallbackInfo ci) {
        addLayer(new CosmeticsFeatureRenderer(this));
    }
}
