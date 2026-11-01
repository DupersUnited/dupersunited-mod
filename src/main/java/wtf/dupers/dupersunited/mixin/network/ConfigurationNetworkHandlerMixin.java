package wtf.dupers.dupersunited.mixin.network;

import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.configuration.ClientboundSelectKnownPacks;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.utils.ServerUtils;

@Mixin(ClientConfigurationPacketListenerImpl.class)
public abstract class ConfigurationNetworkHandlerMixin {

    @Inject(method = "handleSelectKnownPacks", at = @At("HEAD"))
    private void dupersunited$onSelectKnownPacks(ClientboundSelectKnownPacks packet, CallbackInfo ci) {
        ServerData data = ServerUtils.pendingServer;
        if (data == null || data.ip == null) return;

        String address = data.ip;

        //server sent no pack info so
        if (packet.knownPacks().isEmpty()) {
            MainClient.LOGGER.error("fail no info? @ " + address);
            ServerUtils.setRealServerVersion(address, ServerUtils.UNKNOWNPACKETSYO);
            return;
        }

        String fallbackVersion = null;
        for (KnownPack pack : packet.knownPacks()) {
            if ("core".equals(pack.id())) {
                ServerUtils.setRealServerVersion(address, pack.version());
                return;
            }
            //otherwise remember the first usable pack as a best guess
            if (fallbackVersion == null && !pack.id().isBlank() && !pack.version().isBlank()) {
                fallbackVersion = pack.version();
            }
        }

        if (fallbackVersion != null) {
            ServerUtils.setRealServerVersion(address, fallbackVersion);
        }
    }
}