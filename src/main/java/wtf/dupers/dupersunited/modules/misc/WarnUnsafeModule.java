package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import com.mojang.blaze3d.platform.InputConstants;

public class WarnUnsafeModule extends Module {
    public WarnUnsafeModule() {
        super("WarnUnsafeMods", "Warns you when joining a server if you have unsafe modules enabled.", Category.misc);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));

        //enable by default
        this.setEnabled(true);
    }
}
