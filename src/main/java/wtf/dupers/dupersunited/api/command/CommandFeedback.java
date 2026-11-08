package wtf.dupers.dupersunited.api.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.api.module.settings.Setting;
import wtf.dupers.dupersunited.commands.MainCommand;

public interface CommandFeedback {
    static int success(Setting<?> setting) {
        MainCommand.sendMessage(Component.empty()
            .append(Component.literal(setting.getName()).withStyle(ChatFormatting.AQUA))
            .append(" set to ")
            .append(Component.literal(String.valueOf(setting.getValue())).withStyle(ChatFormatting.AQUA))
            .append("."), true);

        return 1;
    }

    static int failure(Setting<?> setting, Object input) {
        MainCommand.sendMessage(Component.literal("Invalid value ").withStyle(ChatFormatting.RED)
            .append(Component.literal(String.valueOf(input)).withStyle(ChatFormatting.WHITE))
            .append(" for setting ")
            .append(Component.literal(setting.getName()).withStyle(ChatFormatting.AQUA))
            .append("."), true);

        return 0;
    }
}
