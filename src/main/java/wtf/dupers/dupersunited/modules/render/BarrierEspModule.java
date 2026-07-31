package wtf.dupers.dupersunited.modules.render;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.IntSetting;

/**
 * Shows barrier blocks with the same marker particles vanilla uses when a
 * creative-mode player holds a barrier item.
 */
public class BarrierEspModule extends Module {
    private static final int SPAWN_INTERVAL_TICKS = 20;
    private static final BlockStateParticleEffect BARRIER_MARKER = new BlockStateParticleEffect(
        ParticleTypes.BLOCK_MARKER,
        Blocks.BARRIER.getDefaultState()
    );

    private final IntSetting range = register(new IntSetting("Range", 32, 8, 48));
    private int tickCounter;

    public BarrierEspModule() {
        super("BarrierESP", "Shows nearby barrier blocks.", Category.render);
        register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
    }

    @Override
    public void onTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        if (tickCounter++ % SPAWN_INTERVAL_TICKS != 0) return;

        BlockPos center = client.player.getBlockPos();
        int radius = range.getValue();
        int radiusSquared = radius * radius;

        for (BlockPos pos : BlockPos.iterateOutwards(center, radius, radius, radius)) {
            if (center.getSquaredDistance(pos) > radiusSquared) continue;
            if (!client.world.getBlockState(pos).isOf(Blocks.BARRIER)) continue;

            client.world.addParticleClient(
                BARRIER_MARKER,
                true,
                false,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                0.0,
                0.0,
                0.0
            );
        }
    }
}
