package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class KickCommand extends Command {
    public KickCommand() {
        super("kick", "Disconnects you from the server using various methods.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(literal("disconnect")
                .executes(c -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.getConnection() == null) return 0;
                    client.execute(() ->
                        client.getConnection().getConnection()
                            .disconnect(Component.literal("Disconnected via kick command (/du kick disconnect)"))
                    );
                    return 1;
                })
            )
            .then(literal("pos")
                .executes(c -> {
                    Minecraft client = Minecraft.getInstance();
                    LocalPlayer player = client.player;
                    if (player == null || client.getConnection() == null) return 0;
                    client.execute(() ->
                        client.getConnection().send(
                            new ServerboundMovePlayerPacket.PosRot(
                                Double.NaN,
                                Double.NEGATIVE_INFINITY,
                                Double.POSITIVE_INFINITY,
                                0.0f,
                                0.0f,
                                !player.onGround(),
                                player.horizontalCollision
                            )
                        )
                    );
                    MainCommand.sendMessage(Component.literal("Sending invalid position packet...").withStyle(ChatFormatting.WHITE), true);
                    return 1;
                })
            )
            .then(literal("hurt")
                .executes(c -> {
                    Minecraft client = Minecraft.getInstance();
                    LocalPlayer player = client.player;
                    if (player == null || client.getConnection() == null) return 0;
                    client.execute(() -> player.setHealth(0f));
                    MainCommand.sendMessage(Component.literal("Sending invalid health packet...").withStyle(ChatFormatting.WHITE), true);
                    return 1;
                })
            )
            .then(literal("chat")
                .executes(c -> {
                    Minecraft client = Minecraft.getInstance();
                    LocalPlayer player = client.player;
                    if (player == null || client.getConnection() == null) return 0;
                    client.execute(() ->
                        player.connection.sendChat("§0§1§")
                    );
                    MainCommand.sendMessage(Component.literal("Sending malformed chat packet...").withStyle(ChatFormatting.WHITE), true);
                    return 1;
                })
            )
            .then(literal("crash")
                .executes(c -> {
                    Minecraft client = Minecraft.getInstance();
                    CrashReport report = CrashReport.forThrowable(new Throwable(), "Killed by DupersUnited crash command");

                    client.emergencySaveAndCrash(report);

                    System.exit(1);
                    return 1;
                })
            );
    }
}