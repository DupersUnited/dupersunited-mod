package wtf.dupers.dupersunited.modules.glitcha;

import net.minecraft.client.Minecraft;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.StringSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class ClickSlotModule extends Module {

    private final StringSetting slot = new StringSetting("Slot", "0");
    private final StringSetting count = new StringSetting("Count", "1");
    private final StringSetting delayMs = new StringSetting("Delay", "50");
    private final BooleanSetting loop = new BooleanSetting("Loop", false);

    private int  clicksRemaining = 0;
    private long nextClickAt = 0L;

    public ClickSlotModule() {
        super("ClickSlot", "Sends left-click pickup packets to a slot a certain amount of times.", Category.glitcha);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
        this.register(slot);
        this.register(count);
        this.register(delayMs);
        this.register(loop);

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    Minecraft mc = Minecraft.getInstance();

    @Override
    public void onEnable() {
        resetClicks();

        MainCommand.sendMessage(Component.literal("Clicking slot ")
                .append(Component.literal(String.valueOf(getParsedSlot())).withStyle(ChatFormatting.GREEN))
                .append(" (x")
                .append(Component.literal(String.valueOf(getParsedCount())).withStyle(ChatFormatting.AQUA))
                .append(")"), true);
    }

    private void resetClicks() {
        clicksRemaining = getParsedCount();
        nextClickAt = System.currentTimeMillis();
    }

    // failsafes in case user is a dumbass and tries to put text into the click amount & delay
    private int getParsedSlot() {
        try {
            int val = Integer.parseInt(slot.getValue().trim());
            return Math.max(0, Math.min(val, 90));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int getParsedCount() {
        try {
            return Integer.parseInt(count.getValue().trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private int getParsedDelay() {
        try {
            return Integer.parseInt(delayMs.getValue().trim());
        } catch (NumberFormatException e) {
            return 50;
        }
    }

    @Override
    public void onDisable() {
        clicksRemaining = 0;
        MainCommand.sendMessage("Finished clicking.", true);
    }

    private void onTick(Minecraft client) {
        if (!isEnabled()) return;

        if (clicksRemaining <= 0) {
            if (loop.getValue()) {
                resetClicks();
            } else {
                setEnabled(false);
                return;
            }
        }

        if (mc.getConnection() == null) {
            setEnabled(false);
            return;
        }

        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        if (now < nextClickAt) return;

        short targetSlot = (short) getParsedSlot();

        AbstractContainerMenu handler = (mc.player.containerMenu != null)
                ? mc.player.containerMenu
                : mc.player.inventoryMenu;

        if (targetSlot < 0 || targetSlot >= handler.slots.size()) {
            setEnabled(false);
            return;
        }

        ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(
                handler.containerId,
                handler.getStateId(),
                targetSlot,
                (byte) 0,
                ContainerInput.PICKUP,
                new Int2ObjectArrayMap<>(),
                HashedStack.EMPTY
        );

        mc.getConnection().send(packet);

        clicksRemaining--;
        nextClickAt = now + Math.max(0, getParsedDelay());
    }

    @Override
    public void toggle() {
        setEnabled(!isEnabled());
    }
}