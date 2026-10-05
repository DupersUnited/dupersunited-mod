package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.screens.mainmenu.KeybindScreen;

public final class KeybindCommand extends Command {
    public KeybindCommand() {
        super("keybinds", "Opens keybind GUI.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(c -> {
            var client = Minecraft.getInstance();
            if (client.player == null) return 0;

            MainCommand.sendMessage("Opening Keybinds Menu...", true);
            SharedVariables.screenToOpen = new KeybindScreen(client.gui.screen());
            return 1;
        });
    }
}