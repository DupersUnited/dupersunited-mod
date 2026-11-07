package wtf.dupers.dupersunited.modules.glitcha;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import com.mojang.blaze3d.platform.InputConstants;

public class PacketLoggerModule extends Module {

    public final BooleanSetting logSigns = register(new BooleanSetting("Signs", true));
    public final BooleanSetting logGuiOpen = register(new BooleanSetting("GuiOpen", true));
    public final BooleanSetting logGuiClose = register(new BooleanSetting("GuiClose", true));
    public final BooleanSetting logGuiClick = register(new BooleanSetting("GuiClick", true));
    public final BooleanSetting logGuiUpdates = register(new BooleanSetting("GuiUpdates", true));

    public PacketLoggerModule() {
        super("PacketLogger", "Log inventory and GUI S2C and C2S packets (Can be very spammy!)", Category.glitcha);
        this.register(new BindSetting("Keybind", InputConstants.UNKNOWN.getValue()).linkedTo(this));
    }
}