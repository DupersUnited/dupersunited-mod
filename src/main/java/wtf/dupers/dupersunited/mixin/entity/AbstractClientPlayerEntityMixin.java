package wtf.dupers.dupersunited.mixin.entity;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dupers.dupersunited.features.cosmetics.CapeManager;
import wtf.dupers.dupersunited.features.ModSettings;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Shadow
    @Nullable
    protected abstract PlayerInfo getPlayerInfo();

    @Inject(at = @At("RETURN"), method = "getSkin", cancellable = true)
    private void getSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
        if (!ModSettings.showModCapes.getValue()) return;
        PlayerInfo entry = this.getPlayerInfo();
        if (entry == null) return;

        Identifier cape = CapeManager.getProfile(entry.getProfile().id());
        if (cape != null) {
            PlayerSkin textures = cir.getReturnValue();
            cir.setReturnValue(new PlayerSkin(
                    textures.body(),
                    new ClientAsset.ResourceTexture(cape, cape),
                    new ClientAsset.ResourceTexture(cape, cape),
                    textures.model(),
                    textures.secure()
            ));
        }
    }
}
