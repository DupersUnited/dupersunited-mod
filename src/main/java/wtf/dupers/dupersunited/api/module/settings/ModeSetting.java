package wtf.dupers.dupersunited.api.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import wtf.dupers.dupersunited.api.command.CommandFeedback;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private List<String> options;

    public ModeSetting(String name, String defaultValue, String... options) {
        super(name, defaultValue);
        this.options = Arrays.asList(options);
    }

    @Override
    public boolean setValue(String value) {
        if (options.contains(value)) {
            return super.setValue(value);
        } else {
            return false;
        }
    }

    public void setOptions(String... newOptions) {
        options = Arrays.asList(newOptions);
        if (!options.contains(getValue())) {
            super.setValue(options.getFirst());
        }
    }

    public List<String> getOptions() { return options; }

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
        builder.then(ClientCommands.literal("set").then(ClientCommands.argument("value", StringArgumentType.greedyString())
            .suggests((_, suggestions) -> SharedSuggestionProvider.suggest(this.options, suggestions))
            .executes(ctx -> {
                String value = StringArgumentType.getString(ctx, "value");
                return this.setValue(value) ? CommandFeedback.success(this) : CommandFeedback.failure(this, value);
            })
        ));
    }
}