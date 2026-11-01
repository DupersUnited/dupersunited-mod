package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager;
import com.mojang.blaze3d.platform.InputConstants;

import static wtf.dupers.dupersunited.MainClient.mc;

public class SaveGuiKeybind extends Keybind {
    public SaveGuiKeybind() {
        super("Save GUI", InputConstants.KEY_F6);
    }

    @Override
    public void onPress() {
        if (mc.player != null) {
            SaveGuiManager.saveAndCloseGui();
        }
    }
}