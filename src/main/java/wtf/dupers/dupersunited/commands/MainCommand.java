package wtf.dupers.dupersunited.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.features.screens.ClickGui;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import static wtf.dupers.dupersunited.MainClient.mc;

public final class MainCommand {
    private static final Component FEEDBACK_PREFIX = Component.empty()
            .append(Component.literal("DupersUnited ").withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA))
            .append(Component.literal("» ").withStyle(ChatFormatting.DARK_GRAY));

    private MainCommand() {}

    public static void sendMessage(String message, boolean prefix) {
        sendMessage(Component.literal(message), prefix);
    }

    public static void sendMessage(Component message, boolean prefix) {
        if (mc.player != null) {
            if (prefix) {
                mc.gui.hud.getChat().addClientSystemMessage(Component.empty().append(FEEDBACK_PREFIX).append(message));
            } else {
                mc.gui.hud.getChat().addClientSystemMessage(message);
            }
        }
    }

    public static void register(Map<String, Command> commands) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = literal("du").executes(ctx -> {
                SharedVariables.screenToOpen = new ClickGui(mc.gui.screen());
                sendMessage("Opening ClickGUI!", true);
                return 1;
            });

            commands.forEach((subcommand, command) -> {
                LiteralArgumentBuilder<FabricClientCommandSource> builder = literal(subcommand);
                command.build(builder, registryAccess);
                root.then(builder);
            });

            dispatcher.register(root);
        });
    }
}