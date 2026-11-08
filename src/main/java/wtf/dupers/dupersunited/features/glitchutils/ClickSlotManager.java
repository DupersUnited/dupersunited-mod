package wtf.dupers.dupersunited.features.glitchutils;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import wtf.dupers.dupersunited.features.macros.Macros;

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


        if (slot < 0 || slot >= mc.player.containerMenu.slots.size()) {
            running = false;
            return;
        }

        ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(
            mc.player.containerMenu.containerId,
            mc.player.containerMenu.getStateId(),
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

        Macros macros = Macros.getInstance();
        for (int i = 0; i < Math.max(0, count); i++) {
            macros.recordAction(Macros.MacroAction.click(s, ContainerInput.PICKUP, 0));
        }
    }

    public static void stop() {
        running = false;
    }

    public static boolean isRunning() {
        return running;
    }
}