package wtf.dupers.dupersunited.commands.subcommands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import wtf.dupers.dupersunited.features.macrogui.MacroManager;

public final class MacroGuiCommand extends Command {
    public MacroGuiCommand() {
        super("macrogui", "Record, run, and manage GUI-triggered macros.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder
            .then(ClientCommands.literal("record")
                .executes(ctx -> {
                    GuiMacro.getInstance().prepareRecording(null, "Any");
                    String name = GuiMacro.getInstance().getCurrentMacroName();

                    MainCommand.sendMessage(Component.literal("Recording macro ")
                        .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" on "))
                        .append(Component.literal("any GUI").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal(".")), true);
                    return 1;
                })

                .then(argument("name", StringArgumentType.word())

                    .then(argument("gui", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            String gui = StringArgumentType.getString(ctx, "gui");

                            GuiMacro.getInstance().prepareRecording(name, gui);
                            MainCommand.sendMessage(Component.literal("Recording macro ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" when GUI "))
                                .append(Component.literal(gui).withStyle(ChatFormatting.DARK_AQUA))
                                .append(Component.literal(" opens.")), true);
                            return 1;
                        })
                    )

                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");

                        GuiMacro.getInstance().prepareRecording(name, "Any");
                        MainCommand.sendMessage(Component.literal("Recording macro ")
                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" on "))
                            .append(Component.literal("any GUI").withStyle(ChatFormatting.DARK_AQUA))
                            .append(Component.literal(".")), true);
                        return 1;
                    })
                )
            )

            .then(ClientCommands.literal("run")
                .then(argument("name", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        GuiMacro.getInstance().getMacroNames().forEach(sb::suggest);
                        return sb.buildFuture();
                    })
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");

                        GuiMacro.getInstance().runMacro(name);
                        MainCommand.sendMessage(Component.literal("Macro ")
                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" has been "))
                            .append(Component.literal("enabled").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(".")), true);
                        return 1;
                    })
                )
            )

            .then(ClientCommands.literal("stop")
                .executes(ctx -> {
                    MacroManager.stop();
                    MainCommand.sendMessage(Component.literal("All GUIMacros are now ")
                        .append(Component.literal("disabled").withStyle(ChatFormatting.RED))
                        .append(Component.literal(".")), true);
                    return 1;
                })
            )

            .then(ClientCommands.literal("finish")
                .executes(ctx -> {
                    GuiMacro.getInstance().finalizeRecording();
                    return 1;
                })
            )

            .then(ClientCommands.literal("list")
                .executes(ctx -> {
                    var list = GuiMacro.getInstance().getMacroNames();
                    if (list.isEmpty()) {
                        MainCommand.sendMessage(Component.literal("No macros saved."), true);
                    } else {
                        MainCommand.sendMessage(Component.literal("Macros: ")
                            .append(Component.literal(String.join(", ", list)).withStyle(ChatFormatting.AQUA)), true);
                    }
                    return 1;
                })
            )

            .then(ClientCommands.literal("delete")
                .then(argument("name", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        GuiMacro.getInstance().getMacroNames().forEach(sb::suggest);
                        return sb.buildFuture();
                    })
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");

                        GuiMacro.getInstance().deleteMacro(name);
                        MainCommand.sendMessage(Component.literal("Deleted macro ")
                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(".")), true);
                        return 1;
                    })
                )
            );
    }
}