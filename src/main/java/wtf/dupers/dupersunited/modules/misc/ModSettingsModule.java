package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;

/*
    This module is for shit that shouldn't be in config area (according to vinzy)
 */

public class ModSettingsModule extends Module {

    public final BooleanSetting toggleMessageSetting = register(new BooleanSetting("Send Toggle Msg", false));

    public ModSettingsModule() {
        super("Mod Settings", "Misc settings for the mod.", Category.misc);
    }
}