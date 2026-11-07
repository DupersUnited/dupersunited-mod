package wtf.dupers.dupersunited.modules.misc;

import com.mojang.blaze3d.platform.InputConstants;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;

public class BrandSpoofModule extends Module {
    public BrandSpoofModule() {
        super("BrandSpoof", "Spoofs your client brand as vanilla.", Category.misc);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));
    }
}
