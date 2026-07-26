package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import org.spongepowered.asm.mixin.MixinEnvironment;
import wtf.dupers.dupersunited.api.command.Command;

public class MixinAuditCommand extends Command {
    public MixinAuditCommand() {
        super("audit-mixins", "Force applies all mixins.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandRegistryAccess registryAccess) {
        builder.executes(ctx -> {
            MixinEnvironment.getCurrentEnvironment().audit();
            return 1;
        });
    }
}
