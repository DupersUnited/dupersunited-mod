package wtf.dupers.dupersunited.modules.misc;

import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;

public class AutoReconnectModule extends Module {
    public AutoReconnectModule() {
        super("AutoReconnect", "Automatically reconnects after getting disconnected.", Category.misc);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }
}
