package wtf.dupers.dupersunited.modules.render;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.ModeSetting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class FullBrightModule extends Module {
    public final ModeSetting mode = register(new ModeSetting("Mode", "Ambient", "Ambient", "Potion", "Gamma"));

    public FullBrightModule() {
        super("FullBright","Makes the bright full.", Category.render);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onTick() {
        if (mc.player != null) {
            if (mode.getValue().equals("Potion")) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 840));
            }
        }
    }
}
