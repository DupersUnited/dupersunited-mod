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
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.system.MemoryStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
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

    public static final int MOUSE_OFFSET = 100;

    private KeybindManager() {}

    public static int mouse(int button) {
        return -(button + MOUSE_OFFSET);
    }

    public static boolean isMouse(int code) {
        return code <= -MOUSE_OFFSET;
    }

    public static int mouseButton(int code) {
        return -code - MOUSE_OFFSET;
    }

    public static boolean isDown(int code) {
        if (code == InputConstants.UNKNOWN.getValue()) return false;
        if (isMouse(code)) return isMouseDown(mouseButton(code));
        if (code < 0) return false;
        return InputConstants.isKeyDown(code);
    }

    private static boolean isMouseDown(int button) {
        if (button < 1) return false;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int mask = SDLMouse.SDL_GetMouseState(stack.mallocFloat(1), stack.mallocFloat(1));
            return (mask & (1 << (button - 1))) != 0;
        }
    }

    public static String name(int code) {
        if (code == InputConstants.UNKNOWN.getValue()) return "NONE";
        if (isMouse(code)) {
            int button = mouseButton(code);
            return switch (button) {
                case InputConstants.MOUSE_BUTTON_LEFT -> "MOUSE LEFT";
                case InputConstants.MOUSE_BUTTON_MIDDLE -> "MOUSE MIDDLE";
                case InputConstants.MOUSE_BUTTON_RIGHT -> "MOUSE RIGHT";
                default -> "MOUSE " + button;
            };
        }
        if (code < 0) return "NONE";
        return InputConstants.Type.KEYBOARD.getOrCreate(code).getDisplayName().getString().toUpperCase(Locale.ROOT);
    }

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

        for (Map.Entry<KeyMapping, KeybindAction> entry : keybinds.entrySet()) {
            int code = ((KeyBindingAccessor) entry.getKey()).dupersunited$getBoundKey().getValue();
            if (isDown(code)) {
                if (heldVanilla.add(code)) entry.getValue().run();
            } else {
                heldVanilla.remove(code);
            }
        }

        for (Module m : MainClient.MODULE_MANAGER.modules()) {
            int code = m.getKeybind();
            if (isDown(code)) {
                if (heldModules.add(code)) m.toggle();
            } else {
                heldModules.remove(code);
            }
        }

        for (Keybind keybind : registeredKeybinds.values()) {
            int code = keybind.getKeyCode();
            if (isDown(code)) {
                if (heldRegistered.add(code)) keybind.onPress();
            } else {
                heldRegistered.remove(code);
            }
        }
        //this should hopefully fix a bug that fucks the game
        for (Module m : MainClient.MODULE_MANAGER.modules()) {
            for (Setting<?> s : m.getSettings()) {
                if (!(s instanceof BindSetting bs)) continue;
                int code = bs.getValue();
                if (code == InputConstants.UNKNOWN.getValue()) {
                    heldBindSettings.remove(bs);
                    continue;
                }
                if (isDown(code)) {
                    if (heldBindSettings.add(bs)) bs.firePress();
                } else {
                    heldBindSettings.remove(bs);
                }
            }
        }
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