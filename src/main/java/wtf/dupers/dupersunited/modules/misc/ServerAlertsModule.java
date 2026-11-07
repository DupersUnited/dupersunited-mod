package wtf.dupers.dupersunited.modules.misc;

import com.mojang.blaze3d.platform.InputConstants;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;

public class ServerAlertsModule extends Module {
    public ServerAlertsModule() {
        super("ServerAlerts", "Warns you about flagged servers before joining.", Category.misc);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));

        //enable by default
        this.setEnabled(true);
    }
}
