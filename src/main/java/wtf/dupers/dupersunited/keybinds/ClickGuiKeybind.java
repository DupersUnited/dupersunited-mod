package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.screens.ClickGui;
import wtf.dupers.dupersunited.features.screens.mainmenu.KeybindScreen;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.mixin.accessor.KeyBindingAccessor;

import static wtf.dupers.dupersunited.MainClient.mc;

public class ClickGuiKeybind {
    public static KeyMapping keyBinding;

    private ClickGuiKeybind() {}

    public static void register() {
        keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "Click GUI",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                SharedVariables.CATEGORY
        ));
    }

    public static void onKey(int key, int action) {
        if (action != GLFW.GLFW_PRESS) return;
        if (key != ((KeyBindingAccessor) keyBinding).dupersunited$getBoundKey().getValue()) return;
        if (mc.gui.screen() instanceof ClickGui) return;
        if (mc.gui.screen() instanceof KeybindScreen) return;
        if (isTextFieldFocused()) return;

        Screen parentScreen = mc.gui.screen();
        mc.execute(() -> mc.gui.setScreen(new ClickGui(parentScreen)));
    }

    private static boolean isTextFieldFocused() {
        Screen screen = mc.gui.screen();
        if (screen == null) return false;
        if (screen.getFocused() instanceof EditBox) return true;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof EditBox tf && tf.isFocused()) return true;
        }
        return false;
    }
}