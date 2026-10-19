package wtf.dupers.dupersunited.modules.render;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import com.mojang.blaze3d.platform.InputConstants;

public class HudModule extends Module {
    public final BooleanSetting renderSetting = register(new BooleanSetting("Render", true));
    public final BooleanSetting miscSetting = register(new BooleanSetting("Misc", true));
    public final BooleanSetting glitchaSetting = register(new BooleanSetting("Glitcha", true));
 // public final BooleanSetting exploitSetting = register(new BooleanSetting("Exploit", true));

    public HudModule() {
        super("HUD", "Displays your enabled modules.", Category.render);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));
    }
}