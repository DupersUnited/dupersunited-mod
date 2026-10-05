package wtf.dupers.dupersunited.features.glitchutils;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.glitcha.PacketLoggerModule;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.item.ItemStack;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.commands.MainCommand.sendMessage;

public class PacketLogger {

    public static void log(Packet<?> packet, String direction) {
        PacketLoggerModule module = (PacketLoggerModule) MainClient.MODULE_MANAGER.getModuleByName("PacketLogger");
        if (module == null || !module.isEnabled()) return;

        if (mc.player == null) return;

        String packetName = null;
        String extraData = "";

        if (module.logGuiClose.getValue() && (packet instanceof ServerboundContainerClosePacket || packet instanceof ClientboundContainerClosePacket)) {
            packetName = "Close Window Packet";
        }
        else if (module.logGuiClick.getValue() && packet instanceof ServerboundContainerClickPacket p) {
            packetName = "Click Window Packet";
            ItemStack stack = mc.player.containerMenu.getCarried();
            int slot = p.slotNum();
            String itemName = stack.isEmpty() ? "None" : stack.getHoverName().getString();
            extraData = String.format(" [Slot: %d, Item: %s]", slot, itemName);
        }
        else if (module.logGuiOpen.getValue() && packet instanceof ClientboundOpenScreenPacket p) {
            packetName = "Open Window Packet";
            String title  = p.getTitle().getString();
            int    syncId = p.getContainerId();
            extraData = String.format(" [Name: %s, ID: %d]", title, syncId);
        }
        else if (module.logGuiUpdates.getValue()) {
            if (packet instanceof ClientboundContainerSetContentPacket p) {
                packetName = "Inventory";
                extraData = String.format(" [ID: %d, Items: %d]", p.containerId(), p.items().size());
            }
            else if (packet instanceof ClientboundContainerSetSlotPacket p) {
                packetName = "Slot Update";
                ItemStack stack = p.getItem();
                String itemName = stack.isEmpty() ? "None" : stack.getHoverName().getString();
                extraData = String.format(" [Slot: %d, Item: %s]", p.getSlot(), itemName);
            }
        }

        if (module.logSigns.getValue() && packet instanceof ServerboundSignUpdatePacket p) {
            packetName = "Update Sign Packet";
            extraData = String.format(" [Lines: %s, %s, %s, %s]",
                    p.getLines()[0], p.getLines()[1], p.getLines()[2], p.getLines()[3]);
        }

        if (packetName == null) return;

        final Component styledAction = direction.equals("OUT") ? Component.literal("Sent ").withStyle(ChatFormatting.RED) : Component.literal("Received ").withStyle(ChatFormatting.GREEN);
        final Component styledName = Component.literal(packetName).withStyle(ChatFormatting.WHITE);
        final Component styledExtra = Component.literal(extraData).withStyle(ChatFormatting.GRAY);

        mc.execute(() -> {
            if (mc.player == null) return;
            sendMessage(Component.empty()
                .append(styledAction)
                .append(styledName)
                .append(styledExtra), true);
        });
    }
}