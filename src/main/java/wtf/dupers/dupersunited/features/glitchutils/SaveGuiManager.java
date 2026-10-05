package wtf.dupers.dupersunited.features.glitchutils;

import wtf.dupers.dupersunited.commands.MainCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;

import static wtf.dupers.dupersunited.MainClient.mc;

public class SaveGuiManager {
    public static String guiName;
    public static Screen savedScreen = null;
    public static AbstractContainerMenu savedScreenHandler = null;
    public static boolean deadGui;

    private SaveGuiManager() {}

    public static void saveAndCloseGui() {
        if (mc.player != null) {
            savedScreen = mc.gui.screen();
            savedScreenHandler = mc.player.containerMenu;
            if (savedScreen == null) {
                MainCommand.sendMessage(Component.literal("No GUI found to save.").withStyle(ChatFormatting.RED), true);
                return;
            }
            deadGui = false;
            mc.gui.setScreen(null);
            if (savedScreen instanceof AbstractContainerScreen<?> handled) {
                MainCommand.sendMessage(Component.literal("Saved ")
                    .append(handled.getTitle())
                    .append(" GUI."), true);
                guiName = handled.getTitle().getString();
            } else {
                String classSimpleName = savedScreen.getClass().getSimpleName();
                MainCommand.sendMessage("Saved " + classSimpleName + " GUI.", true);
                guiName = classSimpleName;
            }
            GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.saveAndCloseGui());
        }}

    public static void saveGui() {
        if (mc.player != null && mc.gui.screen() != null) {
            savedScreen = mc.gui.screen();
            savedScreenHandler = mc.player.containerMenu;
            deadGui = false;
            if (savedScreen instanceof AbstractContainerScreen<?> handled) {
                MainCommand.sendMessage(Component.literal("Saved ")
                    .append(handled.getTitle())
                    .append(" GUI."), true);
                guiName = handled.getTitle().getString();
            } else {
                String classSimpleName = savedScreen.getClass().getSimpleName();
                MainCommand.sendMessage("Saved " + classSimpleName + " GUI.", true);
                guiName = classSimpleName;
            }
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
            MainCommand.sendMessage(Component.literal("Restored ")
                .append(Component.literal(guiName).withStyle(ChatFormatting.AQUA))
                .append(" GUI."), true);
            GuiMacro.getInstance().recordAction(GuiMacro.MacroAction.restoreGui());
        } else {
            MainCommand.sendMessage(Component.literal("No saved GUI.").withStyle(ChatFormatting.RED), true);
        }
    }
}