package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;


public final class WaitCommand extends Command {
    public WaitCommand() {
        super("wait", "Waits a given number of milliseconds (tick-aligned), then optionally runs a command or sends a chat message.");
    }

    private record PendingWait(int ticksRemaining, String cmd) {}

    private static final List<PendingWait> pending = new ArrayList<>();

    public static void onTick() {
        if (pending.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        Iterator<PendingWait> it = pending.iterator();

        while (it.hasNext()) {
            PendingWait wait = it.next();
            int remaining = wait.ticksRemaining() - 1;

            if (remaining <= 0) {
                it.remove();
                if (wait.cmd() != null && client.player != null) {
                    if (wait.cmd().startsWith("/")) {
                        client.player.connection.sendCommand(wait.cmd().substring(1));
                    } else {
                        client.player.connection.sendChat(wait.cmd());
                    }
                } else if (wait.cmd() == null) {
                    MainCommand.sendMessage(
                        Component.literal("Done waiting!").withStyle(ChatFormatting.WHITE),
                        true
                    );
                }
            } else {
                it.remove();
                pending.add(new PendingWait(remaining, wait.cmd()));
                break;
            }
        }
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(argument("ms", IntegerArgumentType.integer(1))
            .executes(c -> {
                int ms = IntegerArgumentType.getInteger(c, "ms");
                int ticks = Math.max(1, ms / 50);

                MainCommand.sendMessage(
                    Component.literal("Waiting ")
                        .append(Component.literal(ms + "ms").withStyle(ChatFormatting.RED))
                        .append(Component.literal(" (" + ticks + " ticks)...").withStyle(ChatFormatting.GREEN)),
                    true
                );

                pending.add(new PendingWait(ticks, null));
                return 1;
            })

            .then(argument("cmd", StringArgumentType.greedyString())
                .executes(c -> {
                    int ms = IntegerArgumentType.getInteger(c, "ms");
                    String cmd = StringArgumentType.getString(c, "cmd");
                    int ticks = Math.max(1, ms / 50);

                    boolean isCommand = cmd.startsWith("/");

                    MainCommand.sendMessage(
                        Component.literal("Waiting ")
                            .append(Component.literal(ms + "ms").withStyle(ChatFormatting.RED))
                            .append(Component.literal(" " + ticks + " tick(s)...").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(isCommand ? " then running: " : " then saying: ").withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(cmd).withStyle(ChatFormatting.AQUA)),
                        true
                    );

                    pending.add(new PendingWait(ticks, cmd));
                    return 1;
                })
            )
        );
    }
}