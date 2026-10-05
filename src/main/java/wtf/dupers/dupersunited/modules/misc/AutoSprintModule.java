package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class AutoSprintModule extends Module {
    private boolean wasSprinting = false;

    private final BooleanSetting waterCheck = register(new BooleanSetting("WaterCheck", true));

    public AutoSprintModule() {
        super("AutoSprint", "Automatically enables sprint.", Category.misc);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    protected void onDisable() {
        wasSprinting = false;
    }

    @Override
    public void onTick() {
        if (isEnabled()) {
            if (mc.player != null) {
                if (waterCheck.getValue() && mc.player.isInWater()) {
                    if (wasSprinting) {
                        mc.options.keySprint.setDown(false);
                        wasSprinting = false;
                    }
                    return;
                }
                mc.options.keySprint.setDown(true);
                wasSprinting = true;
            }
        }
    }
}