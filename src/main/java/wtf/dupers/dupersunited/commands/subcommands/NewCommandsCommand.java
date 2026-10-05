package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedSet;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

public final class NewCommandsCommand extends Command {
    private static final SimpleCommandExceptionType COULD_NOT_DUMP = new SimpleCommandExceptionType(new LiteralMessage("Could not dump commands, view logs for details."));

    public NewCommandsCommand() {
        super("new-commands", "Scans all server commands and dumps it into /DupersUnited/commands-dump.txt");
    }

    public static final SortedSet<String> commandList = new ObjectRBTreeSet<>(String.CASE_INSENSITIVE_ORDER);

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(ctx -> executeDump(ctx, "commands-dump"))
            .then(argument("filename", StringArgumentType.greedyString())
                .executes(ctx -> executeDump(ctx, StringArgumentType.getString(ctx, "filename"))));
    }

    public static void onCommandTree(RootCommandNode<ClientSuggestionProvider> rootNode) {
        commandList.clear();

        rootNode.getChildren().stream()
            .filter(node -> node instanceof LiteralCommandNode<?>)
            .forEach(node -> commandList.add("/" + node.getName()));
    }

    private static int executeDump(CommandContext<FabricClientCommandSource> ctx, String outputFile) throws CommandSyntaxException {
        if (!Files.exists(SharedVariables.DIRECTORY) || !Files.isDirectory(SharedVariables.DIRECTORY)) {
            try {
                Files.createDirectories(SharedVariables.DIRECTORY);
            } catch (IOException e) {
                MainClient.LOGGER.error("Could not create directory", e);
                throw COULD_NOT_DUMP.create();
            }
        }

        Path file = SharedVariables.DIRECTORY.resolve(outputFile.endsWith(".txt") ? outputFile : outputFile + ".txt");

        try {
            Files.write(file, commandList);
        } catch (IOException e) {
            MainClient.LOGGER.error("Could not write dump file", e);
            throw COULD_NOT_DUMP.create();
        }

        if (!commandList.isEmpty()) {
            MainCommand.sendMessage("Successfully dumped " + commandList.size() + " commands!", true);
        } else {
            MainCommand.sendMessage("No commands captured yet odd...", true); // this will probably trigger if it's your first run of the command is hould maybe fix that
        }

        return 1;
    }
}