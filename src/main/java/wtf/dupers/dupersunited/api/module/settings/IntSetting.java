package wtf.dupers.dupersunited.api.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.api.command.CommandFeedback;

public class IntSetting extends Setting<Integer> {
    private final int min, max;

    public IntSetting(String name, int defaultValue, int min, int max) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean setValue(Integer value) {
        if (value >= min && value <= max) {
            return super.setValue(value);
        } else {
            return false;
        }
    }

    public int getMin() { return min; }
    public int getMax() { return max; }

    @Override
    public JsonElement writeJson() {
        return new JsonPrimitive(this.getValue());
    }

    @Override
    public void readJson(JsonElement element) throws IllegalArgumentException {
        this.setValue(element.getAsInt());
    }

    @Override
    public void buildCommandStub(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(ClientCommands.argument("value", IntegerArgumentType.integer(this.min, this.max)).executes(ctx -> {
            int value = IntegerArgumentType.getInteger(ctx, "value");
            return this.setValue(value) ? CommandFeedback.success(this) : CommandFeedback.failure(this, value);
        }));
    }
}