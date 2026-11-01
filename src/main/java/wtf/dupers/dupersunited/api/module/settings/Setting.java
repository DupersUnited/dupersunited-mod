package wtf.dupers.dupersunited.api.module.settings;


import com.google.gson.JsonElement;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String name;
    public BooleanSupplier visible = () -> true;
    protected T value;

    public Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
    }

    public String getName() { return name; }
    public T getValue() { return value; }

    /**
     * @return Whether the value was successfully changed.
     */
    public boolean setValue(T value) {
        this.value = value;
        return true;
    }

    // Config Serialization
    public boolean shouldSaveConfig() { return true; }
    public abstract JsonElement writeJson();
    public abstract void readJson(JsonElement element) throws IllegalArgumentException;

    // Commands
    public void buildCommandStub(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext registryAccess) {}
}
