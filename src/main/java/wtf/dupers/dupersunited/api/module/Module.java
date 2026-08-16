package wtf.dupers.dupersunited.api.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.modules.misc.ModSettingsModule;
import wtf.dupers.dupersunited.modules.render.HudModule;
import wtf.dupers.dupersunited.api.module.settings.Setting;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.network.packet.Packet;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Module {
    private final String name;
    private final String category;
    private final String description;
    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<>();
    public String namespace;

    public Module(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <T extends Setting<?>> T register(T setting) {
        settings.add(setting);
        return setting;
    }

    public String getDescription() {
        return description;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public Setting<?> getSettingByName(String name) {
        return settings.stream()
                .filter(s -> s.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return The module's display name. Multiple modules can have the same display name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return The identifier for the module, guaranteed to be unique.
     */
    public String getIdentifier() {
        return namespace + ":" + name;
    }

    public String getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean state) {
        if (enabled == state) return;
        enabled = state;
        if (enabled) onEnable();
        else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);

        ModSettingsModule hud = MainClient.getModule(ModSettingsModule.class);
        if (hud != null && hud.toggleMessageSetting.getValue()) return;

        Text status = enabled
            ? Text.literal("enabled").formatted(Formatting.GREEN)
            : Text.literal("disabled").formatted(Formatting.RED);

        MainCommand.sendMessage(Text.empty()
            .append(Text.literal(name).formatted(Formatting.AQUA))
            .append(" is now ")
            .append(status)
            .append("."), true);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick() {
    }

    public void onPacketSend(Packet<?> packet) {
    }

    public void onPacketReceive(Packet<?> packet) {
    }


    private int keybind = GLFW.GLFW_KEY_UNKNOWN;

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int key) {
        this.keybind = key;
    }

    public JsonElement writeJson() {
        JsonObject root = new JsonObject();

        root.addProperty("keybind", this.getKeybind());
        root.addProperty("enabled", this.isEnabled());

        if (!this.getSettings().isEmpty()) {
            JsonObject settingsObj = new JsonObject();
            for (Setting<?> s : this.getSettings()) {
                if (!s.shouldSaveConfig()) continue;
                settingsObj.add(s.getName(), s.writeJson());
            }
            root.add("settings", settingsObj);
        }

        return root;
    }

    public void readJson(JsonElement element) {
        if (element instanceof JsonObject root) {
            if (root.has("keybind")) {
                this.setKeybind(root.get("keybind").getAsInt());
            }
            if (root.has("enabled")) {
                this.setEnabled(root.get("enabled").getAsBoolean());
            }
            if (root.has("settings")) {
                JsonObject settingsObj = root.getAsJsonObject("settings");
                for (Setting<?> s : this.getSettings()) {
                    if (!s.shouldSaveConfig() || !settingsObj.has(s.getName())) continue;
                    JsonElement el = settingsObj.get(s.getName());
                    try {
                        s.readJson(el);
                    } catch (Exception e) {
                        MainClient.LOGGER.error("Bad value for {}.{}: '{}'", this.getName(), s.getName(), el, e);
                    }
                }
            }
        }
    }
}