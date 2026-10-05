package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.glitchutils.PayAllManager;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;


public final class PayAllCommand extends Command {
    public PayAllCommand() {
        super("pay-all", "Starts pay all macro");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(literal("start")
                .executes(c -> {
                    if (PayAllManager.isRunning()) {
                        MainCommand.sendMessage(Component.literal("Payall ")
                            .append(Component.literal("is already running").withStyle(ChatFormatting.WHITE))
                            .append("."), true);
                        return 0;
                    }

                    PayAllManager.startPayAll();
                    MainCommand.sendMessage(Component.literal("Starting ")
                        .append(Component.literal("PayAll").withStyle(ChatFormatting.AQUA))
                        .append(" process...").withStyle(ChatFormatting.WHITE), true);
                    return 1;
                }))

            .then(literal("stop")
                .executes(c -> {
                    if (!PayAllManager.isRunning()) {
                        MainCommand.sendMessage(Component.literal("Payall ")
                            .append(Component.literal("is not currently running").withStyle(ChatFormatting.WHITE))
                            .append("."), true);
                        return 0;
                    }

                    PayAllManager.stopPayAll();
                    MainCommand.sendMessage(Component.literal("Stopped ")
                        .append(Component.literal("PayAll").withStyle(ChatFormatting.AQUA))
                        .append(" process.").withStyle(ChatFormatting.WHITE), true);
                    return 1;
                }));
    }
}