package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.glitchutils.GhostBlock;
import com.mojang.blaze3d.platform.InputConstants;

import static wtf.dupers.dupersunited.MainClient.mc;

public class RevertGhostBlockKeybind extends Keybind {
    public RevertGhostBlockKeybind() {
        super("Revert GB", InputConstants.UNKNOWN.getValue());
    }

    @Override
    public void onPress() {
        if (mc.player != null) {
            GhostBlock.restoreGhosts();
        }
    }
}