package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.mixin.accessor.PlayerMoveC2SPacketAccessor;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import static wtf.dupers.dupersunited.MainClient.mc;

public class NoFallModule extends Module {
    public NoFallModule() {
        super("NoFall", "Disables fall damage.", Category.misc);
    }

    @Override
    public void onPacketSend(Packet<?> packet) {
        if (mc.player == null) return;
        if (!(packet instanceof ServerboundMovePlayerPacket)) return;
        if (mc.player.getAbilities().instabuild) return;
        if (mc.player.isFallFlying()) return;
        if (mc.player.getDeltaMovement().y > -0.5) return;

        ((PlayerMoveC2SPacketAccessor) packet).dupersunited$setOnGround(true);
    }
}