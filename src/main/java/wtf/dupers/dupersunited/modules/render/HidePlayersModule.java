package wtf.dupers.dupersunited.modules.render;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.FloatSetting;
import com.mojang.blaze3d.platform.InputConstants;

public class HidePlayersModule extends Module {
    public final BooleanSetting hideAll = register(new BooleanSetting("HideAll", true));
    public final FloatSetting distance = register(new FloatSetting("Distance", 5f, 3f, 100f));

    public HidePlayersModule() {
        super("HidePlayers", "Hides other players.", Category.render);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));
    }
}
