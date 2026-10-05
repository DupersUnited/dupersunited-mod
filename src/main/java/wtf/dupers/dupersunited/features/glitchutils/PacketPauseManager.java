package wtf.dupers.dupersunited.features.glitchutils;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class PacketPauseManager {
    private static boolean paused = false;
    private static boolean releasing = false;
    private static final Queue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    private static final Set<Class<? extends Packet<?>>> TARGET_PACKETS = ConcurrentHashMap.newKeySet();

    private PacketPauseManager() {}

    public static void addTarget(Class<? extends Packet<?>> packetClass) { TARGET_PACKETS.add(packetClass); }
    public static void clearTargets() { TARGET_PACKETS.clear(); }

    public static boolean shouldPause(Packet<?> packet) {
        if (!paused || releasing) return false;

        return TARGET_PACKETS.contains(packet.getClass());
    }

    public static boolean isPaused() { return paused; }
    public static boolean isReleasing() { return releasing; }

    public static void pause() {
        paused = true;
    }

    public static void resume() {
        paused = false;
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