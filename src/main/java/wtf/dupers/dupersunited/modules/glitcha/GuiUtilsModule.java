package wtf.dupers.dupersunited.modules.glitcha;

import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import static wtf.dupers.dupersunited.MainClient.mc;

public class GuiUtilsModule extends Module {

    public final BooleanSetting saveGuiSetting = register(new BooleanSetting("SaveGui", true));
    public final BooleanSetting clearGuiSetting = register(new BooleanSetting("ClearGui", true));
    public final BooleanSetting disconnectAndSendSetting = register(new BooleanSetting("Disconnect", true));
    public final BooleanSetting desyncSetting = register(new BooleanSetting("Desync", true));
    public final BooleanSetting ShowFabricatePackets = register(new BooleanSetting("FabriPackets", true));
    public final BooleanSetting delayPackets = register(new BooleanSetting("DelayPackets", true));
    public final BooleanSetting saveGuiButtonSetting = register(new BooleanSetting("SaveGUI", true));
    public final BooleanSetting commandBoxSetting = (register(new BooleanSetting("CommandBox", true)));
    public final BooleanSetting copyGuiInfo = register(new BooleanSetting("CopyGUIInfo", true));
    public final BooleanSetting invTweaksSetting = register (new BooleanSetting("InvTweaks", true));
    public final BooleanSetting drawSlotIds = register(new BooleanSetting("SlotIds", false));

    public void drawSlotId(GuiGraphicsExtractor graphics, Slot slot) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(0.5f, 0.5f);
        graphics.text(
                mc.font,
                Component.literal(String.valueOf(slot.index)),
                slot.x * 2,
                slot.y * 2,
                0xFFFF00FF,
                true
        );
        pose.popMatrix();
    }

    public GuiUtilsModule() {
        super("GUIUtils", "Allows you to toggle buttons in GUIs.", Category.glitcha);
        this.register(new BindSetting("Keybind", GLFW.GLFW_KEY_UNKNOWN).linkedTo(this));

        //enable by default
        this.setEnabled(true);
    }

}
