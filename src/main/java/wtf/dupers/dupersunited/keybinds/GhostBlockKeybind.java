package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.glitchutils.GhostBlock;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class GhostBlockKeybind extends Keybind {
    public GhostBlockKeybind() {
        super("Ghost Blocks", GLFW.GLFW_KEY_UNKNOWN);
    }

    @Override
    public void onPress() {
        if (mc.player != null) {
            GhostBlock.deleteBlock();
        }
    }
}