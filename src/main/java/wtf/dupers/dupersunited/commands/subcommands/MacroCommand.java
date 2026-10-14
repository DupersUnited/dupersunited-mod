package wtf.dupers.dupersunited.commands.subcommands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.macros.Macros;
import wtf.dupers.dupersunited.features.macros.MacroManager;

public final class MacroCommand extends Command {
    public MacroCommand() {
        super("macro", "Record, run, and manage macros.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder
            .then(literal("record")
                .executes(ctx -> {
                    Macros.getInstance().prepareRecording(null, "Any");
                    String name = Macros.getInstance().getCurrentMacroName();
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
                            Macros.getInstance().prepareRecording(name, gui);
                            MainCommand.sendMessage(Component.literal("Recording macro ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" when GUI "))
                                .append(Component.literal(gui).withStyle(ChatFormatting.DARK_AQUA))
                                .append(Component.literal(" opens.")), true);
                            return 1;
                        }))
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        Macros.getInstance().prepareRecording(name, "Any");
                        MainCommand.sendMessage(Component.literal("Recording macro ")
                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" on "))
                            .append(Component.literal("any GUI").withStyle(ChatFormatting.DARK_AQUA))
                            .append(Component.literal(".")), true);
                        return 1;
                    })))
            .then(literal("add")
                .then(argument("name", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        Macros.getInstance().getMacroNames().forEach(sb::suggest);
                        return sb.buildFuture();
                    })
                    .then(literal("wait")
                        .then(argument("ticks", IntegerArgumentType.integer(0, 72000))
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                Macros.getInstance().appendAction(name, Macros.MacroAction.waitTicks(IntegerArgumentType.getInteger(ctx, "ticks")));
                                MainCommand.sendMessage(Component.literal("Added wait to ")
                                    .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                                return 1;
                            })))
                    .then(literal("waitms")
                        .then(argument("ms", IntegerArgumentType.integer(0, 3600000))
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                Macros.getInstance().appendAction(name, Macros.MacroAction.waitMs(IntegerArgumentType.getInteger(ctx, "ms")));
                                MainCommand.sendMessage(Component.literal("Added wait to ")
                                    .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                                return 1;
                            })))
                    .then(literal("chat")
                        .then(argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                Macros.getInstance().appendAction(name, Macros.MacroAction.sendChat(StringArgumentType.getString(ctx, "text")));
                                MainCommand.sendMessage(Component.literal("Added chat to ")
                                    .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                                return 1;
                            })))
                    .then(literal("command")
                        .then(argument("text", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                String text = StringArgumentType.getString(ctx, "text");
                                Macros.getInstance().appendAction(name, Macros.MacroAction.sendCommand(text.startsWith("/") ? text.substring(1) : text));
                                MainCommand.sendMessage(Component.literal("Added command to ")
                                    .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                                return 1;
                            })))
                    .then(literal("look")
                        .then(argument("yaw", FloatArgumentType.floatArg(-180, 180))
                            .then(argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Macros.getInstance().appendAction(name, Macros.MacroAction.look(FloatArgumentType.getFloat(ctx, "yaw"), FloatArgumentType.getFloat(ctx, "pitch")));
                                    MainCommand.sendMessage(Component.literal("Added look to ")
                                        .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                                    return 1;
                                }))))
                    .then(literal("use")
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            Macros.getInstance().appendAction(name, Macros.MacroAction.useItem());
                            MainCommand.sendMessage(Component.literal("Added use to ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                            return 1;
                        }))
                    .then(literal("attack")
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            Macros.getInstance().appendAction(name, Macros.MacroAction.attack());
                            MainCommand.sendMessage(Component.literal("Added attack to ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                            return 1;
                        }))
                    .then(literal("disconnect")
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            Macros.getInstance().appendAction(name, Macros.MacroAction.disconnect(null));
                            MainCommand.sendMessage(Component.literal("Added disconnect to ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                            return 1;
                        })
                        .then(argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                Macros.getInstance().appendAction(name, Macros.MacroAction.disconnect(StringArgumentType.getString(ctx, "reason")));
                                MainCommand.sendMessage(Component.literal("Added disconnect to ")
                                    .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                                return 1;
                            })))
                    .then(literal("closegui")
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            Macros.getInstance().appendAction(name, Macros.MacroAction.closeGui());
                            MainCommand.sendMessage(Component.literal("Added closegui to ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                            return 1;
                        }))
                    .then(literal("connect")
                        .then(argument("address", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                Macros.getInstance().appendAction(name, Macros.MacroAction.connect(StringArgumentType.getString(ctx, "address")));
                                MainCommand.sendMessage(Component.literal("Added connect to ")
                                    .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                                return 1;
                            })))
                    .then(literal("waitforgui")
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            Macros.getInstance().appendAction(name, Macros.MacroAction.waitForGui(null));
                            MainCommand.sendMessage(Component.literal("Added waitforgui to ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                            return 1;
                        })
                        .then(argument("title", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                Macros.getInstance().appendAction(name, Macros.MacroAction.waitForGui(StringArgumentType.getString(ctx, "title")));
                                MainCommand.sendMessage(Component.literal("Added waitforgui to ")
                                    .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                                return 1;
                            })))))
            .then(literal("run")
                .then(argument("name", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        Macros.getInstance().getMacroNames().forEach(sb::suggest);
                        return sb.buildFuture();
                    })
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        Macros.getInstance().runMacro(name);
                        MainCommand.sendMessage(Component.literal("Macro ")
                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" has been "))
                            .append(Component.literal("enabled").withStyle(ChatFormatting.GREEN))
                            .append(Component.literal(".")), true);
                        return 1;
                    })))
            .then(literal("stop")
                .executes(ctx -> {
                    MacroManager.stop();
                    MainCommand.sendMessage(Component.literal("All macros are now ")
                        .append(Component.literal("disabled").withStyle(ChatFormatting.RED))
                        .append(Component.literal(".")), true);
                    return 1;
                }))
            .then(literal("finish")
                .executes(ctx -> {
                    Macros.getInstance().finalizeRecording();
                    return 1;
                }))
            .then(literal("list")
                .executes(ctx -> {
                    var list = Macros.getInstance().getMacroNames();
                    if (list.isEmpty()) {
                        MainCommand.sendMessage(Component.literal("No macros saved."), true);
                    } else {
                        MainCommand.sendMessage(Component.literal("Macros: ")
                            .append(Component.literal(String.join(", ", list)).withStyle(ChatFormatting.AQUA)), true);
                    }
                    var incompatible = Macros.getInstance().getIncompatibleMacros();
                    if (!incompatible.isEmpty()) {
                        MainCommand.sendMessage(Component.literal("Incompatible macros: ")
                            .append(Component.literal(String.join(", ", incompatible)).withStyle(ChatFormatting.RED)), true);
                    }
                    return 1;
                }))
            .then(literal("delete")
                .then(argument("name", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        Macros.getInstance().getMacroNames().forEach(sb::suggest);
                        return sb.buildFuture();
                    })
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        Macros.getInstance().deleteMacro(name);
                        MainCommand.sendMessage(Component.literal("Deleted macro ")
                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(".")), true);
                        return 1;
                    })))
            .then(literal("describe")
                .then(argument("name", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        Macros.getInstance().getMacroNames().forEach(sb::suggest);
                        return sb.buildFuture();
                    })
                    .then(argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            Macros.getInstance().setMacroDescription(name, StringArgumentType.getString(ctx, "text"));
                            MainCommand.sendMessage(Component.literal("Updated description of ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA)), true);
                            return 1;
                        }))))
            .then(literal("pause")
                .executes(ctx -> {
                    MacroManager.pause();
                    MainCommand.sendMessage(Component.literal("Macro paused."), true);
                    return 1;
                }))
            .then(literal("resume")
                .executes(ctx -> {
                    MacroManager.resume();
                    MainCommand.sendMessage(Component.literal("Macro resumed."), true);
                    return 1;
                }))
            .then(literal("step")
                .executes(ctx -> {
                    MacroManager.step();
                    MainCommand.sendMessage(Component.literal("Stepped one action."), true);
                    return 1;
                }))
            .then(literal("validate")
                .then(argument("name", StringArgumentType.word())
                    .suggests((ctx, sb) -> {
                        Macros.getInstance().getMacroNames().forEach(sb::suggest);
                        return sb.buildFuture();
                    })
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        var problems = Macros.getInstance().validateMacro(name);
                        if (problems.isEmpty()) {
                            MainCommand.sendMessage(Component.literal("Macro ")
                                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" looks valid.").withStyle(ChatFormatting.GREEN)), true);
                        } else {
                            for (String problem : problems) {
                                MainCommand.sendMessage(Component.literal("! ").append(Component.literal(problem)).withStyle(ChatFormatting.RED), true);
                            }
                        }
                        return 1;
                    })));
    }
}
