package wtf.dupers.dupersunited.modules.glitcha;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import com.mojang.blaze3d.platform.InputConstants;

public class TpsCounterModule extends Module {
    public TpsCounterModule() {
        super("TPSCounter","Displays an estimate of the servers TPS on your HUD.", Category.glitcha);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));

        //enable by default
        this.setEnabled(true);
    }
}
