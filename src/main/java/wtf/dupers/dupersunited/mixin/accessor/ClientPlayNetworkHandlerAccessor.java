package wtf.dupers.dupersunited.mixin.accessor;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface ClientPlayNetworkHandlerAccessor {

    @Accessor("registryAccess")
    RegistryAccess.Frozen dupersunited$getCombinedDynamicRegistries();

    @Accessor("enabledFeatures")
    FeatureFlagSet dupersunited$getEnabledFeatures();

    @Accessor("COMMAND_NODE_BUILDER")
    static ClientboundCommandsPacket.NodeBuilder<?> dupersunited$getCommandNodeFactory() { throw new AssertionError(); }
}