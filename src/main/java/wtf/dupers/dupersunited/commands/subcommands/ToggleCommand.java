package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.api.command.arguments.ModuleArgumentType;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.commands.MainCommand;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

public final class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Toggles any module");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(argument("module", ModuleArgumentType.module())
            .executes(context -> {
                Module module = ModuleArgumentType.get(context);

                if (module == null) {
                    MainCommand.sendMessage(
                        Component.literal("Module not found.").withStyle(ChatFormatting.RED),
                        true
                    );

                    return 0;
                }

                module.toggle();

                Component status = module.isEnabled()
                    ? Component.literal("enabled").withStyle(ChatFormatting.GREEN)
                    : Component.literal("disabled").withStyle(ChatFormatting.RED);

                MainCommand.sendMessage(Component.empty()
                    .append(Component.literal(module.getName()).withStyle(ChatFormatting.AQUA))
                    .append(" is now ")
                    .append(status)
                    .append("."), true);

                return 1;
            })
        );
    }
}