package wtf.dupers.dupersunited.features.fleet.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FleetPackets {
    public interface W2HPacket {
        void write(FriendlyByteBuf buf);
    }

    public interface H2WPacket {
        void write(FriendlyByteBuf buf);
    }

    public record Worker(String name, UUID id) {
        void write(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeUUID(id);
        }

        static Worker read(FriendlyByteBuf buf) {
            return new Worker(buf.readUtf(), buf.readUUID());
        }
    }

    public record HelloW2H(String name, UUID id) implements W2HPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(name);
            buf.writeUUID(id);
        }

        static HelloW2H read(FriendlyByteBuf buf) {
            return new HelloW2H(buf.readUtf(), buf.readUUID());
        }
    }

    public record SayAllW2H(String message) implements W2HPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(message);
        }

        static SayAllW2H read(FriendlyByteBuf buf) {
            return new SayAllW2H(buf.readUtf());
        }
    }

    public record SayH2W(String message, long delayMs) implements H2WPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(message);
            buf.writeVarInt((int) Math.min(delayMs, Integer.MAX_VALUE));
        }

        static SayH2W read(FriendlyByteBuf buf) {
            return new SayH2W(buf.readUtf(), buf.readVarInt());
        }
    }

    public record WorkersH2W(List<Worker> workers) implements H2WPacket {
        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(workers.size());
            for (Worker w : workers) w.write(buf);
        }

        static WorkersH2W read(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<Worker> workers = new ArrayList<>(n);
            for (int i = 0; i < n; i++) workers.add(Worker.read(buf));
            return new WorkersH2W(workers);
        }
    }

    public static ByteBuffer encodeW2H(W2HPacket packet) {
        return encode(packet instanceof HelloW2H ? 0 : 1, packet::write);
    }

    public static ByteBuffer encodeH2W(H2WPacket packet) {
        return encode(packet instanceof SayH2W ? 0 : 1, packet::write);
    }

    private static ByteBuffer encode(int id, java.util.function.Consumer<FriendlyByteBuf> write) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(id);
            write.accept(buf);
            ByteBuffer out = ByteBuffer.allocate(buf.readableBytes());
            buf.readBytes(out);
            out.flip();
            return out;
        } finally {
            buf.release();
        }
    }

    public static W2HPacket decodeW2H(ByteBuffer bytes) {
        return decode(bytes, buf -> switch (buf.readVarInt()) {
            case 0 -> HelloW2H.read(buf);
            case 1 -> SayAllW2H.read(buf);
            default -> throw new IllegalArgumentException("unknown w2h packet");
        });
    }

    public static H2WPacket decodeH2W(ByteBuffer bytes) {
        return decode(bytes, buf -> switch (buf.readVarInt()) {
            case 0 -> SayH2W.read(buf);
            case 1 -> WorkersH2W.read(buf);
            default -> throw new IllegalArgumentException("unknown h2w packet");
        });
    }

    private static <T> T decode(ByteBuffer bytes, java.util.function.Function<FriendlyByteBuf, T> read) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            return read.apply(buf);
        } finally {
            buf.release();
        }
    }
}