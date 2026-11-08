package wtf.dupers.dupersunited.api.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.api.command.CommandFeedback;

public class FloatSetting extends Setting<Float> {
    private final float min, max;

    public FloatSetting(String name, float defaultValue, float min, float max) {
        super(name, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean setValue(Float value) {
        if (value >= min && value <= max) {
            return super.setValue(value);
        } else {
            return false;
        }
    }

    public float getMin() { return min; }
    public float getMax() { return max; }

    @Override
    public JsonElement writeJson() {
        return new JsonPrimitive(this.getValue());
    }

    @Override
    public void readJson(JsonElement element) throws IllegalArgumentException {
        this.setValue(element.getAsFloat());
    }

    @Override
    public void buildCommandStub(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(ClientCommands.argument("value", FloatArgumentType.floatArg(this.min, this.max)).executes(ctx -> {
            float value = FloatArgumentType.getFloat(ctx, "value");
            return this.setValue(value) ? CommandFeedback.success(this) : CommandFeedback.failure(this, value);
        }));
    }
}
