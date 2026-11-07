package wtf.dupers.dupersunited.modules.misc;

import com.mojang.blaze3d.platform.InputConstants;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;

public class RpBypassModule extends Module {
    public RpBypassModule() {
        super("RpBypass", "Auto accepts server resource packs.", Category.misc);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));
    }
}
