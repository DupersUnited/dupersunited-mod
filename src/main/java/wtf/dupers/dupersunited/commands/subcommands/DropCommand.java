package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.commands.MainCommand;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;

public final class DropCommand extends Command {
    public DropCommand() {
        super("drop", "Drops the item in a specific inventory slot, or the currently held item if no slot is given.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(c -> {
                Minecraft client = Minecraft.getInstance();
                if (client.player == null || client.gameMode == null) return 0;

                client.execute(() -> {
                    int hotbarIndex = client.player.getInventory().getSelectedSlot();
                    int screenSlot = 36 + hotbarIndex;

                    client.gameMode.handleContainerInput(
                        client.player.inventoryMenu.containerId,
                        screenSlot,
                        1,
                        ContainerInput.THROW,
                        client.player
                    );
                });

                int hotbarIndex = client.player.getInventory().getSelectedSlot();
                MainCommand.sendMessage(Component.literal("Dropped held item in hotbar slot ")
                    .append(Component.literal(String.valueOf(hotbarIndex)).withStyle(ChatFormatting.GREEN))
                    .append("."), true);
                return 1;
            })

            .then(argument("slot", IntegerArgumentType.integer(0, 90))
                .executes(c -> {
                    int slot = IntegerArgumentType.getInteger(c, "slot");
                    Minecraft client = Minecraft.getInstance();
                    if (client.player == null || client.gameMode == null) return 0;
                    int screenSlot = slot < 9 ? 36 + slot : slot;

                    client.execute(() ->
                        client.gameMode.handleContainerInput(
                            client.player.inventoryMenu.containerId,
                            screenSlot,
                            1,
                            ContainerInput.THROW,
                            client.player
                        )
                    );

                    MainCommand.sendMessage(Component.literal("Dropped item in slot ")
                        .append(Component.literal(String.valueOf(slot)).withStyle(ChatFormatting.GREEN))
                        .append("."), true);
                    return 1;
                }));
    }
}