package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xpple.clientarguments.arguments.CBlockInput;
import dev.xpple.clientarguments.arguments.CBlockStateArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.features.glitchutils.GhostBlock;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

public final class ReplaceBlockCommand extends Command {
    public ReplaceBlockCommand() {
        super("replace-block", "Places a client side block");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(argument("block", CBlockStateArgument.blockState(registryAccess))
            .executes(context -> {
                CBlockInput blockInput = CBlockStateArgument.getBlockState(context, "block");
                GhostBlock.replaceBlock(blockInput.getState());
                return 1;
            })
        );
    }
}