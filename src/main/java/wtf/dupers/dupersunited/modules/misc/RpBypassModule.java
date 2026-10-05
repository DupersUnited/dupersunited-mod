package wtf.dupers.dupersunited.modules.misc;

import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;

public class RpBypassModule extends Module {
    public RpBypassModule() {
        super("RpBypass", "Auto accepts server resource packs.", Category.misc);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }
}
