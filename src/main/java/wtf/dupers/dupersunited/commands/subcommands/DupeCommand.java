package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.utils.ServerUtils;

public final class DupeCommand extends Command {
    public DupeCommand() {
        super("dupe", "Automatically dupes on DonutSMP");
    }

    private static Minecraft mc = Minecraft.getInstance();
    public static Boolean amILarpingItUp = false;

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(c -> {
            if (!ServerUtils.isDonut()) {
                MainCommand.sendMessage("You must be on Donut SMP to use this command!", true);
                return 1;
            }

            mc.execute(() ->
                mc.player.connection.getConnection().disconnect(
                    Component.empty()
                        .append(Component.literal("You are temporarily banned for duping.\n\n")
                            .withStyle(s -> s.withColor(ChatFormatting.RED)))
                        .append(Component.literal("Time Left: ")
                            .withStyle(s -> s.withColor(ChatFormatting.GRAY)))
                        .append(Component.literal("13 day 23 hours 59 minutes\n\n")
                            .withStyle(s -> s.withColor(ChatFormatting.WHITE)))
                        .append(Component.literal("Ban ID: "))
                        .withStyle(s -> s.withColor(ChatFormatting.GRAY))
                        .append(Component.literal("#1a507CoV\n")
                            .withStyle(s -> s.withColor(ChatFormatting.WHITE)))
                        .append(Component.literal("You may be able to appeal this ban on\n"))
                        .withStyle(s -> s.withColor(ChatFormatting.GRAY))
                        .append(Component.literal("discord.gg/donutsmp\n\n")
                            .withStyle(s -> s.withColor(ChatFormatting.WHITE)))
                )
            );

            amILarpingItUp = true;

            return 1;
        });
    }
}