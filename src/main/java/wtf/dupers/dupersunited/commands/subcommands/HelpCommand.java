package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;

public final class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Shows this help screen.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(context -> {
            MainCommand.sendMessage(Component.literal("Listing all available commands:").withStyle(ChatFormatting.WHITE), true);

            MainClient.getCommands().forEach(command -> {
                MutableComponent text = Component.literal("/du " + command.command).withStyle(ChatFormatting.AQUA);
                text.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
                text.append(Component.literal(command.description).withStyle(ChatFormatting.GRAY));

                // swag thing to show what the command does
                text.withStyle(style -> style
                    .withClickEvent(new ClickEvent.SuggestCommand("/du " + command.command + " "))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to show /du " + command.command + "!")))
                );

                MainCommand.sendMessage(text, false);
            });

            return 1;
        });
    }
}