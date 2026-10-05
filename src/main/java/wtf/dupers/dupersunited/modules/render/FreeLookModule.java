package wtf.dupers.dupersunited.modules.render;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.FloatSetting;
import net.minecraft.client.CameraType;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class FreeLookModule extends Module {
    public final FloatSetting sensitivity = register(new FloatSetting("Sensitivity", 8f, 0f, 20f));

    public float cameraYaw;
    public float smoothYaw;

    public FreeLookModule() {
        super("FreeLook", "Allows you to orbit your camera around your player without moving your head.", Category.render);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) return;
        cameraYaw = mc.player.getYRot();
        smoothYaw = cameraYaw;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    @Override
    protected void onDisable() {
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK)
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    public boolean isPlayerMode() {
        return isEnabled() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK;
    }
}