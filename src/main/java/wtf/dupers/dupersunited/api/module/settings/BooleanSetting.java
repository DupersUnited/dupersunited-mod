package wtf.dupers.dupersunited.api.module.settings;


import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.api.command.CommandFeedback;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean defaultValue) {
        super(name, defaultValue);
    }

    public void toggle() {
        setValue(!value);
    }

    @Override
    public JsonElement writeJson() {
        return new JsonPrimitive(this.getValue());
    }

    @Override
    public void readJson(JsonElement element) throws IllegalArgumentException {
        this.setValue(element.getAsBoolean());
    }

    @Override
    public void buildCommandStub(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(ClientCommands.literal("toggle").executes(_ -> {
            this.toggle();
            return CommandFeedback.success(this);
        })).then(ClientCommands.argument("value", BoolArgumentType.bool()).executes(ctx -> {
            this.setValue(BoolArgumentType.getBool(ctx, "value"));
            return CommandFeedback.success(this);
        }));
    }
}
