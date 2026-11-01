package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.DupersUnitedMod;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.ButtonSetting;
import wtf.dupers.dupersunited.api.module.settings.Setting;
import wtf.dupers.dupersunited.commands.MainCommand;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class ModuleCommand extends Command {
    public ModuleCommand() {
        super("module", "Shows modules current binds & settings.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        for (Module module : DupersUnitedMod.getModules()) {
            var moduleBuilder = literal(asLiteralArg(module.getName()))
                .executes(_ -> showModule(module))
                .then(literal("toggle")
                    .executes(_ -> setEnabled(module, !module.isEnabled())))
                .then(literal("enable")
                    .executes(_ -> setEnabled(module, true)))
                .then(literal("on")
                    .executes(_ -> setEnabled(module, true)))
                .then(literal("disable")
                    .executes(_ -> setEnabled(module, false)))
                .then(literal("off")
                    .executes(_ -> setEnabled(module, false)));

            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof ButtonSetting) continue;
                var settingBuilder = literal(asLiteralArg(setting.getName()));

                settingBuilder.executes(_ -> showSetting(setting));

                setting.buildCommandStub(settingBuilder, registryAccess);
                moduleBuilder.then(settingBuilder);
            }

            builder.then(moduleBuilder);
        }

        builder.executes(_ -> listModules());
    }

    private int listModules() {
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

        return 1;
    }

    private int showModule(Module module) {
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

        return 1;
    }

    private int setEnabled(Module module, boolean state) {
        boolean already = module.isEnabled() == state;
        if (!already) module.setEnabled(state);

        MainCommand.sendMessage(Component.empty()
            .append(Component.literal(module.getName()).withStyle(ChatFormatting.AQUA))
            .append(already ? " is already " : " is now ")
            .append(status(state))
            .append("."), true);

        return 1;
    }

    private <T> int showSetting(Setting<T> setting) {
        MainCommand.sendMessage(Component.empty()
            .append(Component.literal(setting.getName()).withStyle(ChatFormatting.AQUA))
            .append(" is ")
            .append(Component.literal(String.valueOf(setting.getValue())).withStyle(ChatFormatting.WHITE))
            .append("."), true);

        return 1;
    }

    private Component status(boolean enabled) {
        return enabled
            ? Component.literal("enabled").withStyle(ChatFormatting.GREEN)
            : Component.literal("disabled").withStyle(ChatFormatting.RED);
    }

    private String asLiteralArg(String s) {
        return s.toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}