package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class SaveGuiKeybind extends Keybind {
    public SaveGuiKeybind() {
        super("Save GUI", GLFW.GLFW_KEY_F6);
    }

    @Override
    public void onPress() {
        if (mc.player != null) {
            SaveGuiManager.saveAndCloseGui();
        }
    }
}