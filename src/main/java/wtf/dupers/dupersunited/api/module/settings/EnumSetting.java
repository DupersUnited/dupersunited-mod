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

public class EnumSetting<T extends Enum<T>> extends Setting<T> {
    private final List<T> options;

    public EnumSetting(String name, T defaultValue) {
        super(name, defaultValue);
        this.options = Arrays.asList(defaultValue.getDeclaringClass().getEnumConstants());
    }

    public boolean setValue(String stringifiedValue) {
        for (T option : this.options) {
            if (option.toString().equals(stringifiedValue)) {
                return this.setValue(option);
            }
        }
        return false;
    }

    public void cycle(boolean forwards) {
        int idx = this.options.indexOf(this.getValue());
        if (forwards) idx = (idx + 1) % this.options.size();
        else idx = (idx - 1 + this.options.size()) % this.options.size();
        this.setValue(this.options.get(idx));
    }

    public List<T> getOptions() {
        return this.options;
    }

    @Override
    public JsonElement writeJson() {
        return new JsonPrimitive(this.getValue().toString());
    }

    @Override
    public void readJson(JsonElement element) throws IllegalArgumentException {
        this.setValue(element.getAsString());
    }

    @Override
    public void buildCommandStub(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {
        builder.then(ClientCommands.literal("set").then(ClientCommands.argument("value", StringArgumentType.greedyString())
            .suggests((_, suggestions) -> SharedSuggestionProvider.suggest(this.getOptions().stream().map(Object::toString), suggestions))
            .executes(ctx -> {
                String value = StringArgumentType.getString(ctx, "value");
                return this.setValue(value) ? CommandFeedback.success(this) : CommandFeedback.failure(this, value);
            })
        ));
    }
}
