package wtf.dupers.dupersunited.api.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.keybinds.KeybindManager;
import wtf.dupers.dupersunited.api.module.Module;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Consumer;

public class BindSetting extends Setting<Integer> {
    private Consumer<Integer> callback;
    private String linkedKeybindId;
    private Module linkedModule;

    public BindSetting(String name, int defaultKey) {
        super(name, defaultKey);
    }

    public BindSetting linkedTo(Module module) {
        this.linkedModule = module;
        if (module.getKeybind() != InputConstants.UNKNOWN.getValue()) {
            this.value = module.getKeybind();
        }
        return this;
    }

    public BindSetting linkedTo(String keybindId) {
        this.linkedKeybindId = keybindId;
        return this;
    }

    public BindSetting withCallback(Consumer<Integer> callback) {
        this.callback = callback;
        if (callback != null) callback.accept(this.value);
        return this;
    }

    public void setKeyCode(int keyCode) {
        this.value = keyCode;
        if (linkedModule != null) {
            linkedModule.setKeybind(keyCode);
        }

        if (linkedKeybindId != null) {
            Keybind kb = KeybindManager.getRegisteredKeybinds().get(linkedKeybindId);
            if (kb != null) {
                kb.setKeyCode(keyCode);
            }
        }

        if (callback != null) callback.accept(keyCode);
    }

    public void firePress() {
        if (callback != null) callback.accept(getValue());
    }

    @Override
    public Integer getValue() {
        // always return the actual value to prevent desync
        if (linkedModule != null) return linkedModule.getKeybind();

        if (linkedKeybindId != null) {
            Keybind kb = KeybindManager.getRegisteredKeybinds().get(linkedKeybindId);
            if (kb != null) return kb.getKeyCode();
        }

        return value;
    }

    public String getKeyName() {
        return KeybindManager.name(getValue());
    }

    @Override
    public JsonElement writeJson() {
        return new JsonPrimitive(this.getValue());
    }

    @Override
    public void readJson(JsonElement element) throws IllegalArgumentException {
        int keyCode = element.getAsInt();

        this.value = keyCode;

        if (linkedModule != null) {
            linkedModule.setKeybind(keyCode);
        }

        if (linkedKeybindId != null) {
            Keybind kb = KeybindManager.getRegisteredKeybinds().get(linkedKeybindId);
            if (kb != null) {
                kb.setKeyCode(keyCode);
            }
        }
    }
}