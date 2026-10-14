package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.api.command.arguments.ModuleArgumentType;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.*;
import wtf.dupers.dupersunited.commands.MainCommand;

import java.util.Comparator;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class ModuleCommand extends Command {
    public ModuleCommand() {
        super("module", "Shows modules current binds & settings.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        var module = argument("module", ModuleArgumentType.module())
            .executes(ctx -> {
                showModule(ModuleArgumentType.get(ctx));
                return 1;
            });

        module.then(literal("toggle").executes(c -> {
                Module m = ModuleArgumentType.get(c);
                setEnabled(m, !m.isEnabled());
                return 1;
            }))
            .then(literal("enable").executes(c -> {
                setEnabled(ModuleArgumentType.get(c), true);
                return 1;
            }))
            .then(literal("on").executes(c -> {
                setEnabled(ModuleArgumentType.get(c), true);
                return 1;
            }))
            .then(literal("disable").executes(c -> {
                setEnabled(ModuleArgumentType.get(c), false);
                return 1;
            }))
            .then(literal("off").executes(c -> {
                setEnabled(ModuleArgumentType.get(c), false);
                return 1;
            }));

        module.then(argument("setting", StringArgumentType.word())
            .suggests((ctx, suggestions) -> {
                Module m = ModuleArgumentType.get(ctx);
                if (m == null) return suggestions.buildFuture();
                return SharedSuggestionProvider.suggest(m.getSettings().stream().map(Setting::getName), suggestions);
            })
            .then(argument("value", StringArgumentType.greedyString())
                .suggests((ctx, suggestions) -> {
                    Module m = ModuleArgumentType.get(ctx);
                    if (m == null) return suggestions.buildFuture();
                    Setting<?> setting;
                    try {
                        setting = m.getSettingByName(StringArgumentType.getString(ctx, "setting"));
                    } catch (Exception e) {
                        return suggestions.buildFuture();
                    }
                    if (setting == null) return suggestions.buildFuture();
                    return SharedSuggestionProvider.suggest(values(setting).stream(), suggestions);
                })
                .executes(ctx -> {
                    Module m = ModuleArgumentType.get(ctx);
                    String name = StringArgumentType.getString(ctx, "setting");
                    String value = StringArgumentType.getString(ctx, "value");

                    Setting<?> setting = m.getSettingByName(name);
                    if (setting == null) {
                        MainCommand.sendMessage(Component.literal("Setting ")
                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(" not found").withStyle(ChatFormatting.RED))
                            .append(" on ")
                            .append(Component.literal(m.getName()).withStyle(ChatFormatting.AQUA))
                            .append("."), true);
                        return 0;
                    }

                    boolean ok;
                    try {
                        ok = switch (setting) {
                            case FloatSetting s -> s.setValue(Float.parseFloat(value));
                            case IntSetting s -> s.setValue(Integer.parseInt(value));
                            case BooleanSetting s -> (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) && s.setValue(Boolean.parseBoolean(value));
                            case ModeSetting s -> s.setValue(value);
                            case EnumSetting<?> s -> s.setValue(value);
                            case StringSetting s -> s.setValue(value);
                            default -> false;
                        };
                    } catch (NumberFormatException e) {
                        ok = false;
                    }

                    if (!ok) {
                        MainCommand.sendMessage(Component.literal("Invalid value ").withStyle(ChatFormatting.RED)
                            .append(Component.literal(value).withStyle(ChatFormatting.WHITE))
                            .append(" for setting ")
                            .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                            .append(Component.literal(". Valid: " + String.join(", ", values(setting))).withStyle(ChatFormatting.GRAY)), true);
                        return 0;
                    }

                    MainCommand.sendMessage(Component.empty()
                        .append(Component.literal(m.getName()).withStyle(ChatFormatting.AQUA))
                        .append(" setting ")
                        .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                        .append(" set to ")
                        .append(Component.literal(String.valueOf(setting.getValue())).withStyle(ChatFormatting.AQUA))
                        .append("."), true);
                    return 1;
                })));

        builder.executes(_ -> {
            listModules();
            return 1;
        }).then(module);
    }

    private void listModules() {
        List<Module> modules = MainClient.MODULE_MANAGER.modules().stream()
            .sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        long enabled = modules.stream().filter(Module::isEnabled).count();
        MainCommand.sendMessage(Component.literal("Modules (" + enabled + "/" + modules.size() + " enabled):").withStyle(ChatFormatting.WHITE), true);

        for (Module m : modules) {
            MutableComponent text = Component.empty()
                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(m.getName()).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" [" + (m.isEnabled() ? "ON" : "OFF") + "]").withStyle(m.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED));

            text.withStyle(style -> style
                .withClickEvent(new ClickEvent.SuggestCommand("/du module " + m.getName() + " "))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(m.getDescription()).withStyle(ChatFormatting.GRAY))));

            MainCommand.sendMessage(text, true);
        }
    }

    private void showModule(Module module) {
        MainCommand.sendMessage(Component.empty()
            .append(Component.literal(module.getName()).withStyle(ChatFormatting.AQUA))
            .append(" is ")
            .append(status(module.isEnabled()))
            .append(Component.literal(" - " + module.getDescription()).withStyle(ChatFormatting.GRAY)), true);

        for (Setting<?> setting : module.getSettings()) {
            MainCommand.sendMessage(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(setting.getName()).withStyle(ChatFormatting.DARK_AQUA))
                .append(": ")
                .append(Component.literal(String.valueOf(setting.getValue())).withStyle(ChatFormatting.AQUA)), true);
        }
    }

    private void setEnabled(Module module, boolean state) {
        boolean already = module.isEnabled() == state;
        if (!already) module.setEnabled(state);

        MainCommand.sendMessage(Component.empty()
            .append(Component.literal(module.getName()).withStyle(ChatFormatting.AQUA))
            .append(already ? " is already " : " is now ")
            .append(status(state))
            .append("."), true);
    }

    private Component status(boolean enabled) {
        return enabled
            ? Component.literal("enabled").withStyle(ChatFormatting.GREEN)
            : Component.literal("disabled").withStyle(ChatFormatting.RED);
    }

    private List<String> values(Setting<?> setting) {
        return switch (setting) {
            case BooleanSetting ignored -> List.of("true", "false");
            case ModeSetting s -> s.getOptions();
            case EnumSetting<?> s -> s.getOptions().stream().map(Object::toString).toList();
            default -> List.of(String.valueOf(setting.getValue()));
        };
    }
}