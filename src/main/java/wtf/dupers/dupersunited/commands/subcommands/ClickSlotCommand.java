package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.glitchutils.ClickSlotManager;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

public final class ClickSlotCommand extends Command {
    public ClickSlotCommand() {
        super("click-slot", "Automatically clicks a specific inventory slot.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(c -> {
                MainCommand.sendMessage(Component.literal("Usage: /du click-slot <slot> <count> <delayMs>").withStyle(ChatFormatting.RED), true);
                return 1;
            })

            .then(argument("slot", IntegerArgumentType.integer(0, 90))
                .then(argument("count", IntegerArgumentType.integer(1, 10000))
                    .then(argument("delayMs", IntegerArgumentType.integer(0, 10000))
                        .executes(c -> {
                            int slot = IntegerArgumentType.getInteger(c, "slot");
                            int count = IntegerArgumentType.getInteger(c, "count");
                            int delay = IntegerArgumentType.getInteger(c, "delayMs");

                            ClickSlotManager.start(slot, count, delay);
                            MainCommand.sendMessage(Component.literal("Clicking slot ")
                                .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.GREEN))
                                .append(" (x")
                                .append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.AQUA))
                                .append(")"), true);
                            return 1;
                        }))));
    }
}