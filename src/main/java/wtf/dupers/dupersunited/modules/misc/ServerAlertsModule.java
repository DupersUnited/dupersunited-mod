package wtf.dupers.dupersunited.modules.misc;

import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;

public class ServerAlertsModule extends Module {
    public ServerAlertsModule() {
        super("ServerAlerts", "Warns you about flagged servers before joining.", Category.misc);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));

        //enable by default
        this.setEnabled(true);
    }
}
