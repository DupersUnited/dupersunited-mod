package wtf.dupers.dupersunited.utils.packets;

import net.minecraft.network.protocol.Packet;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class PacketPipeline {
    private static PacketPipeline instance;
    private final Queue<PacketModifier> modifiers = new PriorityBlockingQueue<>(1, Comparator.comparingInt(a -> a.getPriority().value()));

    private PacketPipeline() {}

    public static PacketPipeline getInstance() {
        if (instance == null) instance = new PacketPipeline();
        return instance;
    }

    public synchronized void registerModifier(PacketModifier modifier) {
        modifiers.add(modifier);
    }

    public synchronized void unregisterModifier(PacketModifier modifier) {
        modifiers.remove(modifier);
    }

    public Packet<?> processPacket(Packet<?> packet) {
        Packet<?> modifiedPacket = packet;

        for (PacketModifier modifier : modifiers) {
            if (modifier.getPacketClasses().stream().anyMatch(c -> c.isInstance(packet))) {
                modifiedPacket = modifier.modifyPacket(modifiedPacket);
            }
        }
        return modifiedPacket;
    }


}
