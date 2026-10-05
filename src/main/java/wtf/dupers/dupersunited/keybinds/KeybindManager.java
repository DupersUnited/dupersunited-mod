package wtf.dupers.dupersunited.keybinds;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import wtf.dupers.dupersunited.features.macrogui.MacroManager;
import wtf.dupers.dupersunited.features.screens.ClickGui;
import wtf.dupers.dupersunited.features.screens.mainmenu.KeybindScreen;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.BindSetting;
import wtf.dupers.dupersunited.api.module.settings.Setting;
import wtf.dupers.dupersunited.mixin.accessor.KeyBindingAccessor;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class KeybindManager {

    private static final Map<KeyMapping, KeybindAction> keybinds = new HashMap<>();
    private static final Map<String, Keybind> registeredKeybinds = new HashMap<>();

    private static final IntSet heldVanilla = new IntOpenHashSet();
    private static final IntSet heldModules = new IntOpenHashSet();
    private static final IntSet heldRegistered = new IntOpenHashSet();
    private static final IntSet heldMacros = new IntOpenHashSet();
    private static final Set<BindSetting> heldBindSettings = Collections.newSetFromMap(new IdentityHashMap<>());

    private KeybindManager() {}

    // @vinzy-dev please just make this cleaner i really cant be asked to fix it thank you
    public static void onTick() {
        Screen screen = mc.gui.screen();
        if (screen != null) {
            if (screen instanceof KeybindScreen) {
                heldVanilla.clear();
                heldModules.clear();
                heldRegistered.clear();
                heldMacros.clear();
                heldBindSettings.clear();
                return;
            }

            if (screen instanceof KeybindScreen || screen instanceof ClickGui) {
                return;
            }

            boolean isAllowedScreen = screen instanceof AbstractContainerScreen;
            if (!isAllowedScreen) {
                return;
            }

            if (screen.getFocused() instanceof EditBox) return;

            for (GuiEventListener child : screen.children()) {
                if (child instanceof EditBox tf && tf.isFocused()) return;
            }

            for (KeyMapping kb : keybinds.keySet()) {
                if (kb.isDown()) return;
            }
        }

        long window = mc.getWindow().handle();

        for (Map.Entry<KeyMapping, KeybindAction> entry : keybinds.entrySet()) {
            int glfwKey = ((KeyBindingAccessor) entry.getKey()).dupersunited$getBoundKey().getValue();
            if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) continue;
            int keyState = getInputState(window, glfwKey);
            if (keyState == GLFW.GLFW_PRESS && heldVanilla.add(glfwKey)) {
                entry.getValue().run();
            } else if (keyState == GLFW.GLFW_RELEASE) {
                heldVanilla.remove(glfwKey);
            }
        }

        for (Module m : MainClient.MODULE_MANAGER.modules()) {
            int glfwKey = m.getKeybind();
            if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) continue;
            int keyState = getInputState(window, glfwKey);
            if (keyState == GLFW.GLFW_PRESS && heldModules.add(glfwKey)) {
                m.toggle();
            } else if (keyState == GLFW.GLFW_RELEASE) {
                heldModules.remove(glfwKey);
            }
        }

        for (Keybind keybind : registeredKeybinds.values()) {
            int glfwKey = keybind.getKeyCode();
            if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) continue;
            int keyState = getInputState(window, glfwKey);
            if (keyState == GLFW.GLFW_PRESS && heldRegistered.add(glfwKey)) {
                keybind.onPress();
            } else if (keyState == GLFW.GLFW_RELEASE) {
                heldRegistered.remove(glfwKey);
            }
        }
        //this should hopefully fix a bug that fucks the game
        for (Module m : MainClient.MODULE_MANAGER.modules()) {
            for (Setting<?> s : m.getSettings()) {
                if (!(s instanceof BindSetting bs)) continue;
                int glfwKey = bs.getValue();
                if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
                    heldBindSettings.remove(bs);
                    continue;
                }
                int keyState = getInputState(window, glfwKey);
                if (keyState == GLFW.GLFW_PRESS && heldBindSettings.add(bs)) {
                    bs.firePress();
                } else if (keyState == GLFW.GLFW_RELEASE) {
                    heldBindSettings.remove(bs);
                }
            }
        }
    }

    private static int getInputState(long window, int code) {
        if (code < 0) {
            int mouseButton = (-code) - 100;
            if (mouseButton >= GLFW.GLFW_MOUSE_BUTTON_1 && mouseButton <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                return GLFW.glfwGetMouseButton(window, mouseButton);
            }
            return GLFW.GLFW_RELEASE;
        }
        if (code >= GLFW.GLFW_MOUSE_BUTTON_1 && code <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            return GLFW.glfwGetMouseButton(window, code);
        }
        return GLFW.glfwGetKey(window, code);
    }

    public static void addKeybind(KeyMapping key, Runnable onPress) {
        if (onPress != null) keybinds.put(key, onPress::run);
    }

    public static void registerKeybind(Keybind keybind) {
        registeredKeybinds.put(keybind.getName(), keybind);
    }

    public static void unregisterKeybind(String id) {
        registeredKeybinds.remove(id);
    }

    public static Map<KeyMapping, KeybindAction> getKeybinds() {
        return keybinds;
    }

    public static Map<String, Keybind> getRegisteredKeybinds() {
        return registeredKeybinds;
    }

    @FunctionalInterface
    public interface KeybindAction {
        void run();
    }
}