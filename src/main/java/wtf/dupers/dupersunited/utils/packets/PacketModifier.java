package wtf.dupers.dupersunited.utils.packets;

import net.minecraft.network.protocol.Packet;

import java.util.Set;

public interface PacketModifier {
    Set<Class<? extends Packet<?>>> getPacketClasses();
    Packet<?> modifyPacket (Packet<?> packet);
    default PacketModifierPriority getPriority() {
        return PacketModifierPriority.NORMAL;
    }
}