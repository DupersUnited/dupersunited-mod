package wtf.dupers.dupersunited.keybinds;

import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager;

import static wtf.dupers.dupersunited.MainClient.mc;

public class RestoreDialogKeybind extends Keybind {
    public RestoreDialogKeybind() {
        super("Restore Dialog", GLFW.GLFW_KEY_B);
    }

    @Override
    public void onPress() {
        if (mc.player != null) {
            SaveGuiManager.restoreDialog();
        }
    }
}