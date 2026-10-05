package wtf.dupers.dupersunited.features.glitchutils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static wtf.dupers.dupersunited.commands.MainCommand.sendMessage;

public class SetHand {
    public static void setHand(net.minecraft.world.item.ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int slot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setItem(slot, stack);
        sendMessage(Component.literal("Set your hand to ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(stack.getCount() + "x "))
                .append(stack.getItem().getName(stack)),
            true
        );
    }
}
