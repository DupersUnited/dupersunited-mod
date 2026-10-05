package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.features.glitchutils.SetHand;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;


public final class SetHandCommand extends Command {
    public SetHandCommand() {
        super("set-hand", "Sets your hand client side to any item/block.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(argument("item", ItemArgument.item(registryAccess))
            .then(argument("amount", IntegerArgumentType.integer())
                .executes(context -> {
                    ItemInput itemInput = ItemArgument.getItem(context, "item");
                    int amount = IntegerArgumentType.getInteger(context, "amount");
                    SetHand.setHand(itemInput.createItemStack(amount));
                    return 1;
                })
            )
        );
    }
}