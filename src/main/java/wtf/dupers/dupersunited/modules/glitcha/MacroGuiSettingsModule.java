package wtf.dupers.dupersunited.modules.glitcha;

import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.api.module.settings.FloatSetting;
import wtf.dupers.dupersunited.api.module.settings.StringSetting;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class MacroGuiSettingsModule extends Module {
    public final FloatSetting firstClickDelaySetting = register(new FloatSetting("FirstClickDelay", 500f, 1f, 1000f));
    public final FloatSetting clickIntervalSetting = register(new FloatSetting("ClickInterval", 250f, 1f, 1000f));
    public final BooleanSetting isLoopingSetting = register(new BooleanSetting("Looping", false));

    // GUI Controls
    public final StringSetting recordNameSetting = register(new StringSetting("Record Name", "macro"));
    public final StringSetting recordGuiSetting = register(new StringSetting("Target GUI", "Any"));
    public final BindSetting recordBindSetting = register(new BindSetting("Start Recording", GLFW.GLFW_KEY_UNKNOWN)
        .withCallback(key -> {
            if (key == null || key == GLFW.GLFW_KEY_UNKNOWN) return;
            startAutoRecord();
        }));

    // HUD
    public final BooleanSetting alwaysShowHudSetting = register(new BooleanSetting("Always Show HUD", false));

    private long lastRecordTime = 0;
    private static final long RECORD_COOLDOWN_MS = 1000;

    public MacroGuiSettingsModule() {
        super("MacroGuiSettings", "Allows you to change the settings of the Macro GUI command.", Category.glitcha);
    }

    private void startAutoRecord() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRecordTime < RECORD_COOLDOWN_MS) {
            return;
        }
        lastRecordTime = currentTime;

        String baseName = recordNameSetting.getValue();
        String targetGui = recordGuiSetting.getValue();
        String uniqueName = GuiMacro.getInstance().getUniqueMacroName(baseName);

        GuiMacro.getInstance().prepareRecording(uniqueName, targetGui);

        MainCommand.sendMessage(Component.literal("Recording macro ")
            .append(Component.literal(uniqueName).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" on GUI "))
            .append(Component.literal(targetGui.isBlank() ? "Any" : targetGui).withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal(".")), true);
    }

    @Override
    public void toggle() {
        setEnabled(!isEnabled());
    }
}