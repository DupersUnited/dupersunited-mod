package wtf.dupers.dupersunited.keybinds;

import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.screens.ClickGui;
import wtf.dupers.dupersunited.features.screens.mainmenu.KeybindScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.mixin.accessor.KeyBindingAccessor;

public class ClickGuiKeybind {

    public static KeyBinding keyBinding;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private ClickGuiKeybind() {}

    public static void register() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Click GUI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                SharedVariables.CATEGORY
        ));
    }

    public static void onKey(int key, int action) {
        if (action != GLFW.GLFW_PRESS) return;
        if (key != ((KeyBindingAccessor) keyBinding).dupersunited$getBoundKey().getCode()) return;
        if (mc.currentScreen instanceof ClickGui) return;
        if (mc.currentScreen instanceof KeybindScreen) return;
        if (isTextFieldFocused()) return;

        Screen parentScreen = mc.currentScreen;
        mc.execute(() -> mc.setScreen(new ClickGui(parentScreen)));
    }

    private static boolean isTextFieldFocused() {
        Screen screen = mc.currentScreen;
        if (screen == null) return false;
        if (screen.getFocused() != null) return true;
        for (net.minecraft.client.gui.Element child : screen.children()) {
            if (child instanceof net.minecraft.client.gui.widget.TextFieldWidget tf && tf.isFocused()) return true;
        }
        return false;
    }
}