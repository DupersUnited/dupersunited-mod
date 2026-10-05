package wtf.dupers.dupersunited.features.glitchutils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import wtf.dupers.dupersunited.commands.MainCommand;

import java.util.HashMap;
import java.util.Map;

import static wtf.dupers.dupersunited.MainClient.mc;

public class GhostBlock {
    private static final Map<BlockPos, BlockState> ghosts = new HashMap<>();
    private static int blockAmount;

    public static void deleteBlock() {
        if (mc.player == null || mc.level == null) {
            return;
        }

        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 direction = mc.player.getViewVector(1.0F);
        Vec3 end = start.add(direction.scale(1000.0));

        BlockHitResult hit = mc.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        ghosts.put(pos, state);
        mc.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        MainCommand.sendMessage("Placed ghost block at X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ() + ".", true);
    }

    public static void replaceBlock(BlockState blockState) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 direction = mc.player.getLookAngle();
        Vec3 end = start.add(direction.scale(1000.0));

        BlockHitResult hit = mc.level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);

        ghosts.put(pos, state);
        mc.level.setBlock(pos, blockState, 11);
        MainCommand.sendMessage("Replaced block at X: " + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ() + "with " + blockState.getBlock().getName().getString() + ".", true);
    }

    public static void restoreGhosts() {
        if (mc.level == null) {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : ghosts.entrySet()) {
            mc.level.setBlock(entry.getKey(), entry.getValue(), 11);
            blockAmount++;
        }
        ghosts.clear();
        MainCommand.sendMessage("Restored "  + blockAmount + " ghost blocks.", true);
        blockAmount = 0;
    }

    public static void clearGhosts() {
        ghosts.clear();
    }

}