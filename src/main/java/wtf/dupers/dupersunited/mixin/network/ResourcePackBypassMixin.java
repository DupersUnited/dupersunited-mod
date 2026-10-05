package wtf.dupers.dupersunited.mixin.network;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.misc.RpBypassModule;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ResourcePackBypassMixin {

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void dupersunited$onResourcePackSend(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (!MainClient.MODULE_MANAGER.isEnabled(RpBypassModule.class)) return;

        ClientCommonPacketListenerImpl handler = (ClientCommonPacketListenerImpl) (Object) this;
        ServerboundResourcePackPacket.Action action = ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED;
        handler.send(new ServerboundResourcePackPacket(packet.id(), action));
        ci.cancel();
    }
}