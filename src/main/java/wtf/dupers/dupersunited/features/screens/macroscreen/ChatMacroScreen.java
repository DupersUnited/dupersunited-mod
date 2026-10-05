package wtf.dupers.dupersunited.features.screens.macroscreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.features.chatmacros.ChatMacro;
import wtf.dupers.dupersunited.features.chatmacros.ChatMacroManager;
import wtf.dupers.dupersunited.utils.ColorUtil;
import org.lwjgl.glfw.GLFW;
import java.util.*;

import static wtf.dupers.dupersunited.MainClient.mc;

public class ChatMacroScreen extends Screen {
    private final Screen parent;
    private static final int CELL = 30, PAD = 10;

    private double scrollOffset = 0;
    private static final int LIST_TOP = 70;
    private static final int LIST_BOTTOM_MARGIN = 10;

    public ChatMacroScreen(Screen parent) {
        super(Component.literal("Macro Manager"));
        this.parent = parent;
    }

    private int getListBottom() {
        return height - LIST_BOTTOM_MARGIN;
    }

    private double getMaxScroll(int macroCount) {
        int totalHeight = macroCount * CELL;
        int visibleHeight = getListBottom() - LIST_TOP;
        return Math.max(0, totalHeight - visibleHeight);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractMenuBackground(graphics);
        int w = 300, x = (width - w) / 2;

        graphics.fill(x, 10, x + w, height - 10, ColorUtil.DEEP_SAPPHIRE);
        graphics.centeredText(font, Component.literal("CHATMACRO MANAGER").withStyle(ChatFormatting.BOLD), x + w/2, 20, 0xFFFFFFFF);

        boolean hovNew = mouseX >= x + PAD && mouseX <= x + w - PAD && mouseY >= 40 && mouseY <= 60;
        graphics.fill(x + PAD, 40, x + w - PAD, 60, hovNew ? 0x66CDD6F4 : ColorUtil.FADED_INDIGO);
        graphics.centeredText(font, Component.literal("Create New Macro").withStyle(ChatFormatting.AQUA), x + w/2, 45, 0xFFFFFFFF);

        List<ChatMacro> macros = new ArrayList<>(ChatMacroManager.getMacros().values());

        graphics.enableScissor(x + PAD, LIST_TOP, x + w - PAD, getListBottom());

        for (int i = 0; i < macros.size(); i++) {
            int ry = (int) (LIST_TOP + (i * CELL) - scrollOffset);

            if (ry + CELL - 5 < LIST_TOP || ry > getListBottom()) continue;

            ChatMacro m = macros.get(i);
            boolean hov = mouseX >= x + PAD && mouseX <= x + w - PAD && mouseY >= ry && mouseY <= ry + CELL - 5;

            graphics.fill(x + PAD, ry, x + w - PAD, ry + CELL - 5, hov ? 0x44CDD6F4 : ColorUtil.DEEP_INDIGO);
            graphics.text(font, m.getName(), x + PAD + 10, ry + 6, 0xFFFFFFFF);
            graphics.text(font, Component.literal(getBindName(m.getKeyCode())).withStyle(ChatFormatting.GRAY), x + w - 70, ry + 6, 0xFFFFFFFF);
        }

        graphics.disableScissor();
    }

    private String getBindName(int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN || code == 0) return "NONE";
        if (code < 0) return "MOUSE " + (code + 100);
        String n = GLFW.glfwGetKeyName(code, 0);
        return (n != null) ? n.toUpperCase() : "KEY " + code;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<ChatMacro> macros = new ArrayList<>(ChatMacroManager.getMacros().values());
        double maxScroll = getMaxScroll(macros.size());
        scrollOffset = Math.max(0, Math.min(scrollOffset - verticalAmount * CELL, maxScroll));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean secondary) {
        int w = 300, x = (width - w) / 2;
        double mx = click.x(), my = click.y();

        if (mx >= x + PAD && mx <= x + w - PAD && my >= 40 && my <= 60) {
            String baseName = "New Macro";
            String finalName = baseName;
            int count = 1;

            while (ChatMacroManager.getMacros().containsKey(finalName.toLowerCase(Locale.ROOT))) {
                finalName = baseName + " " + count;
                count++;
            }

            ChatMacro newMacro = new ChatMacro(finalName, new ArrayList<>(), GLFW.GLFW_KEY_UNKNOWN);
            ChatMacroManager.addMacro(newMacro.getName(), newMacro.getMessages(), newMacro.getKeyCode(), true);
            mc.gui.setScreen(new ChatMacroConfigScreen(this, newMacro));
            return true;
        }

        List<ChatMacro> macros = new ArrayList<>(ChatMacroManager.getMacros().values());
        for (int i = 0; i < macros.size(); i++) {
            int ry = (int) (LIST_TOP + (i * CELL) - scrollOffset);
            if (ry + CELL - 5 < LIST_TOP || ry > getListBottom()) continue;

            if (mx >= x + PAD && mx <= x + w - PAD && my >= ry && my <= ry + CELL - 5) {
                mc.gui.setScreen(new ChatMacroConfigScreen(this, macros.get(i)));
                return true;
            }
        }
        return super.mouseClicked(click, secondary);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            mc.gui.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }
}