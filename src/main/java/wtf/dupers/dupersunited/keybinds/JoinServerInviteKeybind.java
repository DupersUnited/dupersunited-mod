package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.features.ServerInviteManager;
import com.mojang.blaze3d.platform.InputConstants;

public class JoinServerInviteKeybind extends Keybind {
    public static final JoinServerInviteKeybind INSTANCE = new JoinServerInviteKeybind();

    private JoinServerInviteKeybind() {
        super("Join Server Invite", InputConstants.KEY_Y);
    }

    @Override
    public void onPress() {
        ServerInviteManager.joinActiveInvite();
    }
}