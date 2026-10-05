package wtf.dupers.dupersunited.features.glitchutils;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import org.jetbrains.annotations.Nullable;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class GuiPacketDelayManager {
    private static boolean paused = false;
    private static boolean releasing = false;
    private static @Nullable String requiredGuiTitle = null;
    private static final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private static final Set<Class<? extends Packet<?>>> TARGET_PACKETS = Set.of(
            ServerboundContainerButtonClickPacket.class,
            ServerboundContainerClickPacket.class
    );

    private GuiPacketDelayManager() {}

    public static boolean shouldPause(Packet<?> packet) {
        if (!paused || releasing) return false;

        if (requiredGuiTitle != null) {
            if (mc.gui.screen() == null) return false;
            String currentTitle = mc.gui.screen().getTitle().getString();
            if (!currentTitle.toLowerCase().contains(requiredGuiTitle.toLowerCase())) {
                return false;
            }
        }

        return TARGET_PACKETS.contains(packet.getClass());
    }

    public static boolean isPaused() { return paused; }
    public static boolean isReleasing() { return releasing; }

    public static void pause(@Nullable String guiTitle) {
        paused = true;
        requiredGuiTitle = guiTitle;

        if (GuiMacro.getInstance().isRecording) {
            GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.pausePackets(guiTitle));
        }
    }

    public static void pause() {
        pause(null);
    }

    public static void resume() {
        paused = false;
        requiredGuiTitle = null;

        if (GuiMacro.getInstance().isRecording) {
            GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.resumePackets(null));
        }

        ClientPacketListener handler = mc.getConnection();
        if (handler != null) flush(handler);
    }

    public static void toggle() {
        if (paused) resume();
        else pause();
    }

    public static void queue(Packet<?> packet) {
        packetQueue.add(packet);
    }

    private static void flush(ClientPacketListener handler) {
        releasing = true;
        Packet<?> packet;
        while ((packet = packetQueue.poll()) != null) {
            handler.send(packet);
        }
        releasing = false;
    }

    public static Queue<Packet<?>> getPacketQueue() { return packetQueue; }
    public static void clear() { packetQueue.clear(); }
}