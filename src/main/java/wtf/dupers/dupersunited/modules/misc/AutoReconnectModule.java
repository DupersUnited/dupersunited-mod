package wtf.dupers.dupersunited.modules.misc;

import com.mojang.blaze3d.platform.InputConstants;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;

public class AutoReconnectModule extends Module {
    public AutoReconnectModule() {
        super("AutoReconnect", "Automatically reconnects after getting disconnected.", Category.misc);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));
    }
}
