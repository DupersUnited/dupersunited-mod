package wtf.dupers.dupersunited.modules.render;

import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class NoTextureRotationsModule extends Module {

    public NoTextureRotationsModule() {
        super("NoTextureRotations", "Removes texture rotations based on position.", Category.render);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    @Override
    protected void onEnable() {
        refreshTerrain();
    }

    @Override
    protected void onDisable() {
        refreshTerrain();
    }

    private static void refreshTerrain() {
        if (mc.level == null) return;

        mc.levelRenderer.invalidateCompiledGeometry(
            mc.level,
            mc.options,
            mc.gameRenderer.mainCamera(),
            mc.getBlockColors()
        );
    }
}