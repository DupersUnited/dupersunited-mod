package wtf.dupers.dupersunited.mixin.render;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dupers.dupersunited.features.cosmetics.CosmeticsFeatureRenderer;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<net.minecraft.client.network.AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
    protected PlayerEntityRendererMixin(EntityRendererFactory.Context context, PlayerEntityModel model, float shadowRadius) { super(context,model,shadowRadius); }

    @Inject(method="<init>",at=@At("TAIL"))
    private void addCosmetics(EntityRendererFactory.Context context,boolean slim,CallbackInfo ci){ addFeature(new CosmeticsFeatureRenderer(this)); }
}
