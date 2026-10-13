package wtf.dupers.dupersunited.features.glitchutils;

import net.minecraft.client.gui.screens.dialog.DialogScreen;
import wtf.dupers.dupersunited.commands.MainCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import wtf.dupers.dupersunited.features.ModSettings;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;

import static wtf.dupers.dupersunited.MainClient.mc;

public class SaveGuiManager {
    public static String guiName;
    public static Screen savedScreen = null;
    public static DialogScreen<?> savedDialog = null;
    public static AbstractContainerMenu savedScreenHandler = null;
    public static boolean deadGui;

    private SaveGuiManager() {}

    private static void saveNotifyMessage(Screen screen) {
        Component title = screen instanceof AbstractContainerScreen<?> handled
            ? handled.getTitle()
            : Component.literal(screen.getClass().getSimpleName());
        guiName = title.getString();

        if (ModSettings.showGuiName.getValue()) {
            MainCommand.sendMessage(Component.literal("Saved ")
                .append(title)
                .append(" GUI."), true);
        } else {
            MainCommand.sendMessage("Saved GUI.", true);
        }
    }

    public static void saveAndCloseGui() {
        if (mc.player != null) {
            if (mc.gui.screen() == null) {
                MainCommand.sendMessage(Component.literal("No GUI found to save.").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (mc.gui.screen() instanceof DialogScreen<?>) {
                savedDialog = (DialogScreen<?>)mc.gui.screen();
                MainCommand.sendMessage("Saved Dialog Menu.", true);
                mc.gui.setScreen(null);
                return;
            }
            //TODO: add macrogui support to saving Dialog Menus, not touching this as khaos redoing macro systems or smthing

            savedScreen = mc.gui.screen();
            savedScreenHandler = mc.player.containerMenu;
            deadGui = false;
            mc.gui.setScreen(null);
            saveNotifyMessage(savedScreen);
            GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.saveAndCloseGui());
        }}

    public static void saveGui() {
        if (mc.player != null && mc.gui.screen() != null) {
            if (mc.gui.screen() instanceof DialogScreen<?>) {
                savedDialog = (DialogScreen<?>)mc.gui.screen();
                MainCommand.sendMessage("Saved Dialog.", true);
                return;
            }
            savedScreen = mc.gui.screen();
            savedScreenHandler = mc.player.containerMenu;
            deadGui = false;
            saveNotifyMessage(savedScreen);
            GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.saveGui());
        } else {
            MainCommand.sendMessage(Component.literal("No GUI found to save.").withStyle(ChatFormatting.RED), true);
        }
    }

    public static void restoreGui() {
        if (savedScreen != null && savedScreenHandler != null && mc.player != null) {
            deadGui = false;
            mc.gui.setScreen(savedScreen);
            mc.player.containerMenu = savedScreenHandler;
            savedScreen = null;
            savedScreenHandler = null;
            if (ModSettings.showGuiName.getValue()) {
                MainCommand.sendMessage(Component.literal("Restored ")
                    .append(Component.literal(guiName).withStyle(ChatFormatting.AQUA))
                    .append(" GUI."), true);
            } else {
                MainCommand.sendMessage("Restored GUI.", true);
            }
            GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.restoreGui());
        } else {
            MainCommand.sendMessage(Component.literal("No saved GUI.").withStyle(ChatFormatting.RED), true);
        }
    }

    public static void restoreDialog() {
        if (savedDialog != null && mc.player != null) {
            mc.gui.setScreen(savedDialog);
            savedDialog = null;
            MainCommand.sendMessage(Component.literal("Restored ")
                .append(Component.literal("Dialog").withStyle(ChatFormatting.AQUA))
                .append(" GUI."), true);
        } else {
            MainCommand.sendMessage(Component.literal("No saved Dialog.").withStyle(ChatFormatting.RED), true);
        }
    }
}