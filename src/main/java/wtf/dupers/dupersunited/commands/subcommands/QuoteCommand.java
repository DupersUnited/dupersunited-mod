package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.api.command.Command;

import static wtf.dupers.dupersunited.SharedVariables.randomQuote;

public final class QuoteCommand extends Command {
    public QuoteCommand() {
        super("quote", "Posts a random quote");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(c -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.connection.sendChat(randomQuote());
            }
            return 1;
        });
    }
}