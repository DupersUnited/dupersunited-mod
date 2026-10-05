package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.glitchutils.GhostBlock;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class RevertGhostBlockKeybind extends Keybind {
    public RevertGhostBlockKeybind() {
        super("Revert GB", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onPress() {
        if (mc.player != null) {
            GhostBlock.restoreGhosts();
        }
    }
}