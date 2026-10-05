package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.api.command.arguments.ModuleArgumentType;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.*;
import wtf.dupers.dupersunited.commands.MainCommand;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;


public final class ModuleCommand extends Command {
    public ModuleCommand() {
        super("module", "Shows modules current binds & settings.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(argument("module", ModuleArgumentType.module())
            .executes(context -> {
                Module module = ModuleArgumentType.get(context);

                var settings = module.getSettings();
                if (settings.isEmpty()) {
                    MainCommand.sendMessage(Component.empty()
                        .append(Component.literal(module.getName()).withStyle(ChatFormatting.AQUA))
                        .append(" has no settings."), true);

                    return 1;
                }

                MainCommand.sendMessage(Component.literal("Settings for ")
                    .append(Component.literal(module.getName()).withStyle(ChatFormatting.AQUA))
                    .append(":"), true);

                for (var setting : settings) {
                    MainCommand.sendMessage(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(setting.getName()).withStyle(ChatFormatting.DARK_AQUA))
                        .append(": ")
                        .append(Component.literal(String.valueOf(setting.getValue())).withStyle(ChatFormatting.AQUA)), true);
                }
                return 1;
            })
            .then(argument("setting", StringArgumentType.word())
                .suggests((context, suggestions) -> {
                    String moduleName = StringArgumentType.getString(context, "module");
                    Module module = MainClient.MODULE_MANAGER.getModuleByName(moduleName);
                    return SharedSuggestionProvider.suggest(module.getSettings().stream().map(Setting::getName), suggestions);
                })
                .then(argument("value", StringArgumentType.greedyString())
                    .executes(context -> {
                        String moduleName = StringArgumentType.getString(context, "module");
                        String settingName = StringArgumentType.getString(context, "setting");
                        String value = StringArgumentType.getString(context, "value");

                        Module module = MainClient.MODULE_MANAGER.getModuleByName(moduleName);
                        if (module == null) {
                            MainCommand.sendMessage(
                                Component.literal("Module not found.").withStyle(ChatFormatting.RED),
                                true
                            );
                            return 0;
                        }

                        var setting = module.getSettingByName(settingName);
                        if (setting == null) {
                            MainCommand.sendMessage(Component.literal("Setting ")
                                .append(Component.literal(settingName).withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(" not found").withStyle(ChatFormatting.RED))
                                .append(" on ")
                                .append(Component.literal(moduleName).withStyle(ChatFormatting.AQUA))
                                .append("."), true);

                            return 0;
                        }

                        try {
                            switch (setting) {
                                case FloatSetting fs -> fs.setValue(Float.parseFloat(value));
                                case BooleanSetting bs -> bs.setValue(Boolean.parseBoolean(value));
                                case ModeSetting ms -> ms.setValue(value);
                                case EnumSetting<?> es -> es.setValue(value);
                                case StringSetting ss -> ss.setValue(value);
                                default -> {
                                    MainCommand.sendMessage(
                                        Component.literal("Unsupported setting type.").withStyle(ChatFormatting.RED),
                                        true
                                    );
                                    return 0;
                                }
                            }

                            MainCommand.sendMessage(Component.empty()
                                .append(Component.literal(moduleName).withStyle(ChatFormatting.AQUA))
                                .append(" setting ")
                                .append(Component.literal(settingName).withStyle(ChatFormatting.AQUA))
                                .append(" set to ")
                                .append(Component.literal(String.valueOf(setting.getValue())).withStyle(ChatFormatting.AQUA))
                                .append("."), true);

                            return 1;
                        } catch (NumberFormatException e) {
                            MainCommand.sendMessage(Component.literal("Invalid value ").withStyle(ChatFormatting.RED)
                                .append(Component.literal(value).withStyle(ChatFormatting.WHITE))
                                .append(" for setting ")
                                .append(Component.literal(settingName).withStyle(ChatFormatting.AQUA))
                                .append("."), true);

                            return 0;
                        }
                    })
                )
            )
        );
    }
}