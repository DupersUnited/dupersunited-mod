package wtf.dupers.dupersunited.api.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import wtf.dupers.dupersunited.api.command.CommandFeedback;

public class StringSetting extends Setting<String> {

    private final int maxLength;

    public StringSetting(String name, String defaultValue) {
        super(name, defaultValue);
        this.maxLength = Integer.MAX_VALUE;
    }

    public StringSetting(String name, String defaultValue, int maxLength) {
        super(name, defaultValue);
        this.maxLength = maxLength;
    }

    @Override
    public boolean setValue(String value) {
        if (value == null) return false;
        if (value.length() > maxLength) {
            super.setValue(value.substring(0, maxLength));
        } else {
            super.setValue(value);
        }
        return true;
    }

    public int getMaxLength() { return maxLength; }

    @Override
    public JsonElement writeJson() {
        return new JsonPrimitive(this.getValue());
    }

    @Override
    public void readJson(JsonElement element) throws IllegalArgumentException {
        this.setValue(element.getAsString());
    }

    @Override
    public void buildCommandStub(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(ClientCommands.literal("set").then(ClientCommands.argument("value", StringArgumentType.greedyString()).executes(ctx -> {
            String value = StringArgumentType.getString(ctx, "value");
            return this.setValue(value) ? CommandFeedback.success(this) : CommandFeedback.failure(this, value);
        })));
    }
}
