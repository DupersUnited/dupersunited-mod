package wtf.dupers.dupersunited.mixin.screen;

import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.TransferState;
import wtf.dupers.dupersunited.features.AutoReconnect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void dupersunited$cacheServer(Screen parent, Minecraft minecraft, ServerAddress hostAndPort, ServerData data, boolean isQuickPlay, TransferState transferState, CallbackInfo ci) {
        AutoReconnect.cacheAddress(hostAndPort.getHost() + ":" + hostAndPort.getPort());
    }
}