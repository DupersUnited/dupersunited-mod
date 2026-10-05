package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.world.InteractionHand;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.glitchutils.NBTEditor;

public final class NbtCommand extends Command {
    public NbtCommand() {
        super("nbt", "Shows you items NBT data.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(c -> {
            var client = Minecraft.getInstance();
            if (client.player == null) return 0;

            var stack = client.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.isEmpty()) {
                MainCommand.sendMessage("Hold an item first, can't edit your hand silly!", true);
                return 0;
            }

            MainCommand.sendMessage("Opening NBT Editor...", true);
            SharedVariables.screenToOpen = new NBTEditor(stack);
            return 1;
        });
    }
}