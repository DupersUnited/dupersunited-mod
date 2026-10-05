package wtf.dupers.dupersunited.mixin.network;

import wtf.dupers.dupersunited.features.glitchutils.PacketLogger;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import wtf.dupers.dupersunited.modules.render.FreecamModule;
import wtf.dupers.dupersunited.api.module.Module;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager.deadGui;
import static wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager.savedScreen;

@Mixin(Connection.class)
public class ClientConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        for (Module module : MainClient.MODULE_MANAGER.modules()) {
            if (module.isEnabled()) module.onPacketSend(packet);
        }

        PacketLogger.log(packet, "OUT");

        if (packet instanceof ServerboundContainerClosePacket && savedScreen != null) {
            mc.execute(() -> {
                if (!deadGui) {
                    deadGui = true;
                    MainCommand.sendMessage(Component.literal("Your saved GUI was closed by the client.").withStyle(ChatFormatting.RED), true);
                }
            });
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At("HEAD"))
    private void onReceive(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        for (Module module : MainClient.MODULE_MANAGER.modules()) {
            if (module.isEnabled()) module.onPacketReceive(packet);
        }

        PacketLogger.log(packet, "IN");

        if (packet instanceof ClientboundPlayerCombatKillPacket death) {
            if (mc.player != null && death.playerId() == mc.player.getId()) {
                mc.execute(() -> {
                    FreecamModule mod = MainClient.MODULE_MANAGER.getModule(FreecamModule.class);
                    if (mod != null && mod.isEnabled()) mod.setEnabled(false);
                });
            }
        }

        if (packet instanceof ClientboundRespawnPacket) {
            mc.execute(() -> {
                FreecamModule freecam = MainClient.MODULE_MANAGER.getModule(FreecamModule.class);
                if (freecam != null && freecam.isEnabled()) freecam.setEnabled(false);
            });
        }

        if (packet instanceof ClientboundContainerClosePacket || packet instanceof ClientboundOpenScreenPacket) {
            mc.execute(() -> {
                if (GuiMacro.getInstance().isRecording) {
                    GuiMacro.getInstance().registerCloseGui();
                }
                if (!deadGui && savedScreen != null) {
                    deadGui = true;
                    MainCommand.sendMessage(Component.literal("Your saved GUI was closed by the server.").withStyle(ChatFormatting.RED), true);
                }
            });
        }
    }
}