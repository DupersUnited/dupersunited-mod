package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager;
import com.mojang.blaze3d.platform.InputConstants;

import static wtf.dupers.dupersunited.MainClient.mc;

public class RestoreGuiKeybind extends Keybind {
    public RestoreGuiKeybind() {
        super("Restore GUI", InputConstants.KEY_V);
    }

    @Override
    public void onPress() {
        if (mc.player != null) {
            SaveGuiManager.restoreGui();
        }
    }
}