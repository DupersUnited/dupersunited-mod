package wtf.dupers.dupersunited.mixin.network;

import com.mojang.brigadier.tree.RootCommandNode;
import wtf.dupers.dupersunited.commands.subcommands.NewCommandsCommand;
import wtf.dupers.dupersunited.features.glitchutils.PluginScanner;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import wtf.dupers.dupersunited.features.screens.TPSDisplay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow @Final private static ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> COMMAND_NODE_BUILDER;
    @Shadow @Final private RegistryAccess.Frozen registryAccess;
    @Shadow @Final private FeatureFlagSet enabledFeatures;

    @Inject(method = "handleSetTime", at = @At("TAIL"))
    private void dupersunited$onWorldTimeUpdate(ClientboundSetTimePacket packet, CallbackInfo ci) {
        TPSDisplay.onWorldTimeUpdate();
    }

    @Inject(method = "handleCommandSuggestions", at = @At("TAIL"))
    private void dupersunited$onCommandSuggestions(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
        PluginScanner.onCommandSuggestions(packet);
    }

    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void dupersunited$onCommandTree(ClientboundCommandsPacket packet, CallbackInfo ci) {
        // create a copy of the command tree so that fabric's client command api doesn't touch it
        RootCommandNode<ClientSuggestionProvider> rootNode = packet.getRoot(
            CommandBuildContext.simple(this.registryAccess, this.enabledFeatures),
            COMMAND_NODE_BUILDER
        );

        PluginScanner.onCommandTree(rootNode);
        NewCommandsCommand.onCommandTree(rootNode);
    }

    @Inject(method = "sendChat(Ljava/lang/String;)V", at = @At("HEAD"))
    private void dupersunited$onSendChat(String message, CallbackInfo ci) {
        GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.sendChat(message));
    }

    @Inject(method = "sendCommand(Ljava/lang/String;)V", at = @At("HEAD"))
    private void dupersunited$onSendCommand(String command, CallbackInfo ci) {
        GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.sendCommand(command));
    }
}