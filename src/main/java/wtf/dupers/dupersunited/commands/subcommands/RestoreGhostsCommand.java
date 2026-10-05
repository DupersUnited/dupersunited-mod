package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.features.glitchutils.GhostBlock;

public final class RestoreGhostsCommand extends Command {
    public RestoreGhostsCommand() {
        super("restore-ghosts", "Restores all ghost blocks placed");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(context -> {
            GhostBlock.restoreGhosts();
            return 1;
        });
    }
}

