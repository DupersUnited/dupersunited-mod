package wtf.dupers.dupersunited.modules.misc;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.FloatSetting;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class VanillaFlyModule extends Module {
    private final FloatSetting speed = register(new FloatSetting("Speed", 1.0f, 0.1f, 10.0f));

    public VanillaFlyModule() {
        super("VanillaFly", "Allows you to fly.", Category.misc);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) return;
        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().setFlyingSpeed(speed.getValue() / 20f);
        mc.player.onUpdateAbilities();
    }

    @Override
    protected void onDisable() {
        if (mc.player == null) return;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().mayfly = false;
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().setFlyingSpeed(0.05f);
            mc.player.onUpdateAbilities();
        }
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        mc.player.getAbilities().mayfly = true;
        mc.player.getAbilities().setFlyingSpeed(speed.getValue() / 20f);
        mc.player.onUpdateAbilities();
    }
}
