package wtf.dupers.dupersunited.modules.misc;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class ChatStackerModule extends Module {
    public ChatStackerModule() {
        super("ChatStacker", "Stacks multiple of the same chat into 1 line.", Category.misc);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));
    }

    public record RepeatingMessage(Component originalMessage, ArrayList<GuiMessage.Line> instances, MutableInt count) {}
}