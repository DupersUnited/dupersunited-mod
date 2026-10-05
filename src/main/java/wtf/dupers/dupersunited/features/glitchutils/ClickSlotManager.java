package wtf.dupers.dupersunited.features.glitchutils;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;

import static wtf.dupers.dupersunited.MainClient.mc;

public class ClickSlotManager {
    private static int slot = 0;
    private static int remaining = 0;
    private static long delay = 50;
    private static long nextClickTime = 0;
    private static boolean running = false;

    public static void onTick() {
        if (!running || remaining <= 0 || mc.player == null) {
            running = false;
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextClickTime) return;

        AbstractContainerMenu handler = (mc.player.containerMenu != null)
            ? mc.player.containerMenu
            : mc.player.inventoryMenu;

        if (slot < 0 || slot >= handler.slots.size()) {
            running = false;
            return;
        }

        ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(
            handler.containerId,
            handler.getStateId(),
            (short) slot,
            (byte) 0,
            ContainerInput.PICKUP,
            new Int2ObjectArrayMap<>(),
            HashedStack.EMPTY
        );

        if (mc.getConnection() != null) {
            mc.getConnection().send(packet);
        }

        remaining--;
        nextClickTime = now + delay;
    }

    public static void start(int s, int count, int d) {
        slot = s;
        remaining = count;
        delay = d;
        nextClickTime = System.currentTimeMillis();
        running = true;

        GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.clickSlotSpam(s, count, d));
    }

    public static void stop() {
        running = false;
    }

    public static boolean isRunning() {
        return running;
    }
}