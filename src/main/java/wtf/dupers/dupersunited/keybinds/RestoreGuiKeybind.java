package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class RestoreGuiKeybind extends Keybind {
    public RestoreGuiKeybind() {
        super("Restore GUI", GLFW.GLFW_KEY_V);
    }

    @Override
    public void onPress() {
        if (mc.player != null) {
            SaveGuiManager.restoreGui();
        }
    }
}