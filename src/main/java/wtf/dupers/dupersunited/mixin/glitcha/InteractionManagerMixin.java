package wtf.dupers.dupersunited.mixin.glitcha;

import wtf.dupers.dupersunited.features.glitchutils.PacketPauseManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class InteractionManagerMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (!PacketPauseManager.isPaused()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return;
        }

        client.getConnection().send(
            new ServerboundUseItemOnPacket(hand, hitResult, 0)
        );

        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}