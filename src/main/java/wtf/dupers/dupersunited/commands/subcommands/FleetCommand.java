package wtf.dupers.dupersunited.commands.subcommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.features.fleet.FleetNet;
import wtf.dupers.dupersunited.modules.ModuleManager;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class FleetCommand extends Command {
    public FleetCommand() {
        super("fleet", "Control multiple Minecraft clients with one client.");
    }

    @Override
    public void build(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.executes(_ -> {
            String msg = switch (FleetNet.state()) {
                case HOSTING -> "hosting with " + FleetNet.workers().size() + " worker" + (FleetNet.workers().size() == 1 ? "" : "s");
                case CONNECTED -> "currently connected.";
                case IDLE -> "currently idle.";
            };
            ModuleManager.moduleLog("Fleet", msg);
            return 1;
        })
        .then(literal("connect")
            .executes(_ -> {
                FleetNet.connect(18718);
                return 1;
            })
            .then(argument("port", IntegerArgumentType.integer(1024, 65535))
                .executes(c -> {
                    FleetNet.connect(IntegerArgumentType.getInteger(c, "port"));
                    return 1;
                })))
        .then(literal("disconnect")
            .executes(_ -> {
                if (FleetNet.state() == FleetNet.State.IDLE) {
                    ModuleManager.moduleLog("Fleet", "not running.");
                } else {
                    FleetNet.stop();
                    ModuleManager.moduleLog("Fleet", "stopped.");
                }
                return 1;
            }))
        .then(literal("say")
            .then(argument("message", StringArgumentType.greedyString())
                .executes(c -> {
                    FleetNet.say(StringArgumentType.getString(c, "message"));
                    return 1;
                })))
        .then(literal("list")
            .executes(_ -> {
                var workers = FleetNet.workers();
                if (workers.isEmpty()) ModuleManager.moduleLog("Fleet", "no workers.");
                else ModuleManager.moduleLog("Fleet", "workers (" + workers.size() + "): " + FleetNet.workerNames());
                return 1;
            }));
    }
}