package wtf.dupers.dupersunited.features.screens.macroscreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.features.chatmacros.*;
import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.features.chatmacros.ChatMacro;
import wtf.dupers.dupersunited.features.chatmacros.ChatMacroManager;
import wtf.dupers.dupersunited.features.chatmacros.MacroMessage;

import java.util.*;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.utils.ColorUtil.*;

public class ChatMacroConfigScreen extends Screen {
    private static final int FOCUS_NONE = -2;
    private static final int FOCUS_NAME = -1;
    private static final int MOUSE_OFFSET = 100;

    private final Screen parent;
    private final ChatMacro macro;
    private String editName;
    private final List<MacroMessage> messages;

    private int focusIdx = FOCUS_NONE;
    private boolean focusDelay = false;
    private boolean rebinding = false;
    private long rebindStartTime = 0;
    private int keyCode;

    private int cursorPos = 0;
    private int selectionAnchor = -1;

    private double scrollOffset = 0;
    private final int itemHeight = 25;

    public ChatMacroConfigScreen(Screen parent, ChatMacro macro) {
        super(Component.literal("ChatMacro Editor"));
        this.parent = parent;
        this.macro = macro;
        this.editName = macro.getName();
        this.messages = new ArrayList<>(macro.getMessages());
        this.keyCode = macro.getKeyCode();
    }

    private boolean isCtrlDown() {
        long handle = mc.getWindow().handle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private boolean hasSelection() {
        return selectionAnchor != -1 && selectionAnchor != cursorPos;
    }

    private void clearSelection() {
        selectionAnchor = -1;
    }

    private int selStart() { return Math.min(cursorPos, selectionAnchor); }
    private int selEnd() { return Math.max(cursorPos, selectionAnchor); }

    private String getCurrentText() {
        if (focusIdx == FOCUS_NAME) return editName;
        if (focusIdx >= 0 && focusIdx < messages.size()) return messages.get(focusIdx).getText();
        return "";
    }

    private void updateCurrentText(String val) {
        if (focusIdx == FOCUS_NAME) editName = val;
        else if (focusIdx >= 0 && focusIdx < messages.size()) {
            MacroMessage m = messages.get(focusIdx);
            messages.set(focusIdx, new MacroMessage(val, m.getDelayMs()));
        }
    }

    private void deleteSelection() {
        String cur = getCurrentText();
        int s = selStart(), e = selEnd();
        updateCurrentText(cur.substring(0, s) + cur.substring(e));
        cursorPos = s;
        clearSelection();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractMenuBackground(graphics);
        int w = 340, x = (width - w) / 2;
        graphics.fill(x - 10, 5, x + w + 10, height - 5, 0xAA000000);

        graphics.centeredText(font,
            Component.literal("Editing ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(editName).withStyle(ChatFormatting.AQUA)),
            width / 2, 8, 0xFFFFFFFF);
        graphics.fill(x, 22, x + w, height - 5, 0xCC11111B);

        int headerY = 30;

        boolean hovAdd = mouseX >= x + 10 && mouseX <= x + 80 && mouseY >= headerY && mouseY <= headerY + 14;
        graphics.fill(x + 10, headerY, x + 80, headerY + 14, hovAdd ? BUTTON_GREEN : 0x4440A02B);
        graphics.centeredText(font, Component.literal("Add").withStyle(ChatFormatting.GREEN), x + 45, headerY + 3, 0xFFFFFFFF);

        boolean hovDel = mouseX >= x + 90 && mouseX <= x + 190 && mouseY >= headerY && mouseY <= headerY + 14;
        graphics.fill(x + 90, headerY, x + 190, headerY + 14, hovDel ? BUTTON_RED : 0x44D20F39);
        graphics.centeredText(font, Component.literal("Delete Macro").withStyle(ChatFormatting.RED), x + 140, headerY + 3, 0xFFFFFFFF);

        graphics.text(font, Component.literal("Bind:").withStyle(ChatFormatting.GRAY), x + 205, headerY + 3, 0xFFFFFFFF);
        String bindStr = rebinding ? "???" : getBindName(keyCode);
        boolean hovBind = mouseX >= x + 240 && mouseX <= x + 330 && mouseY >= headerY && mouseY <= headerY + 14;
        graphics.fill(x + 240, headerY, x + 330, headerY + 14, rebinding ? REBIND_ACTIVE : (hovBind ? 0x66CDD6F4 : 0x44000000));
        graphics.centeredText(font, Component.literal(bindStr), x + 285, headerY + 3, 0xFFFFFFFF);

        graphics.text(font, Component.literal("Name:").withStyle(ChatFormatting.GRAY), x + 10, headerY + 20, 0xFFAAAAAA);
        int nameX = x + 50, nameY = headerY + 18;
        graphics.fill(nameX, nameY, x + 330, headerY + 30, focusIdx == FOCUS_NAME ? FIELD_FOCUSED : 0x44000000);

        if (focusIdx == FOCUS_NAME) renderTextField(graphics, editName, nameX + 5, headerY + 20);
        else graphics.text(font, Component.literal(editName), nameX + 5, headerY + 20, 0xFFFFFFFF);

        graphics.fill(x + 10, headerY + 35, x + w - 10, headerY + 36, 0x33FFFFFF);

        int listStartY = headerY + 40;
        int listEndY = height - 20;

        graphics.enableScissor(x, listStartY, x + w, listEndY);
        for (int i = 0; i < messages.size(); i++) {
            int ry = (int) (listStartY + (i * itemHeight) - scrollOffset);
            if (ry > listEndY || ry + itemHeight < listStartY) continue;
            MacroMessage m = messages.get(i);

            graphics.fill(x + 10, ry, x + 240, ry + 16, (focusIdx == i && !focusDelay) ? FIELD_FOCUSED : 0x22000000);
            if (focusIdx == i && !focusDelay) renderTextField(graphics, m.getText(), x + 15, ry + 4);
            else graphics.text(font, m.getText(), x + 15, ry + 4, 0xFFFFFFFF);

            graphics.fill(x + 245, ry, x + 305, ry + 16, (focusIdx == i && focusDelay) ? FIELD_FOCUSED : 0x22000000);
            graphics.centeredText(font, m.getDelayMs() + "ms", x + 275, ry + 4, PEACH);

            boolean hovX = mouseX >= x + 310 && mouseX <= x + 330 && mouseY >= ry && mouseY <= ry + 16;
            graphics.text(font, hovX ? Component.literal("X").withStyle(ChatFormatting.WHITE) : Component.literal("x").withStyle(ChatFormatting.GRAY), x + 315, ry + 4, 0xFFFFFFFF);
        }
        graphics.disableScissor();

        graphics.centeredText(font, Component.literal("Press ESC to Save").withStyle(ChatFormatting.DARK_GRAY), width / 2, height - 15, 0xFFFFFFFF);
    }

    private void renderTextField(GuiGraphicsExtractor graphics, String text, int tx, int ty) {
        if (hasSelection()) {
            int s1 = tx + font.width(text.substring(0, selStart()));
            int s2 = tx + font.width(text.substring(0, selEnd()));
            graphics.fill(s1, ty - 1, s2, ty + 9, 0x6689b4fa);
        }
        graphics.text(font, text, tx, ty, 0xFFFFFFFF);
        if (!hasSelection() && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = tx + font.width(text.substring(0, Math.min(cursorPos, text.length())));
            graphics.fill(cx, ty - 1, cx + 1, ty + 9, GREEN);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.input();
        if (rebinding) {
            this.keyCode = (key == GLFW.GLFW_KEY_ESCAPE) ? GLFW.GLFW_KEY_UNKNOWN : key;
            rebinding = false;
            return true;
        }

        if (focusIdx >= 0 && focusDelay) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                MacroMessage m = messages.get(focusIdx);
                String s = String.valueOf(m.getDelayMs());
                int newDelay = (s.length() > 1) ? Integer.parseInt(s.substring(0, s.length() - 1)) : 0;
                messages.set(focusIdx, new MacroMessage(m.getText(), newDelay));
                return true;
            }
        }

        if (focusIdx >= FOCUS_NAME && !focusDelay) {
            String cur = getCurrentText();
            boolean ctrl = isCtrlDown();

            if (ctrl && key == GLFW.GLFW_KEY_A) {
                selectionAnchor = 0;
                cursorPos = cur.length();
                return true;
            }
            if (ctrl && key == GLFW.GLFW_KEY_C) {
                if (hasSelection()) mc.keyboardHandler.setClipboard(cur.substring(selStart(), selEnd()));
                return true;
            }
            if (ctrl && key == GLFW.GLFW_KEY_V) {
                String cb = mc.keyboardHandler.getClipboard();
                if (cb != null && !cb.isEmpty()) {
                    if (hasSelection()) deleteSelection();
                    cur = getCurrentText();
                    cursorPos = Math.min(cursorPos, cur.length());
                    updateCurrentText(cur.substring(0, cursorPos) + cb + cur.substring(cursorPos));
                    cursorPos += cb.length();
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (hasSelection()) deleteSelection();
                else if (cursorPos > 0) {
                    updateCurrentText(cur.substring(0, cursorPos - 1) + cur.substring(cursorPos));
                    cursorPos--;
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_LEFT) {
                if (hasSelection()) { cursorPos = selStart(); clearSelection(); }
                else if (cursorPos > 0) cursorPos--;
                return true;
            }
            if (key == GLFW.GLFW_KEY_RIGHT) {
                if (hasSelection()) { cursorPos = selEnd(); clearSelection(); }
                else if (cursorPos < cur.length()) cursorPos++;
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) { save(); mc.gui.setScreen(parent); return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (isCtrlDown() || focusIdx < FOCUS_NAME) return false;

        if (focusDelay && focusIdx >= 0) {
            String typed = input.codepointAsString();
            if (!typed.isEmpty() && Character.isDigit(typed.charAt(0))) {
                char c = typed.charAt(0);
                MacroMessage m = messages.get(focusIdx);
                String next = String.valueOf(m.getDelayMs()) + c;
                try {
                    long val = Long.parseLong(next);
                    if (val <= 99999) messages.set(focusIdx, new MacroMessage(m.getText(), (int)val));
                } catch (Exception ignored) {}
                return true;
            }
        } else if (!focusDelay) {
            if (input.isAllowedChatCharacter()) {
                if (hasSelection()) deleteSelection();
                String cur = getCurrentText();
                updateCurrentText(cur.substring(0, cursorPos) + input.codepointAsString() + cur.substring(cursorPos));
                cursorPos++;
                return true;
            }
        }
        return false;
    }

    private String getBindName(int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN || code == 0) return "NONE";
        if (code <= -MOUSE_OFFSET) return "MB" + (code + MOUSE_OFFSET);
        String n = GLFW.glfwGetKeyName(code, 0);
        return (n != null) ? n.toUpperCase() : "K" + code;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean secondary) {
        int w = 340, x = (width - w) / 2;
        int headerY = 30;
        int mx = (int) click.x(), my = (int) click.y();

        if (rebinding) {
            if (System.currentTimeMillis() - rebindStartTime > 100) {
                this.keyCode = -(click.button() + MOUSE_OFFSET);
                rebinding = false;
                return true;
            }
            return false;
        }

        if (mx >= x + 10 && mx <= x + 80 && my >= headerY && my <= headerY + 14) {
            messages.add(new MacroMessage("/", 0));
            return true;
        }

        if (mx >= x + 90 && mx <= x + 190 && my >= headerY && my <= headerY + 14) {
            ChatMacroManager.removeMacro(macro.getName(), false);
            mc.gui.setScreen(parent);
            return true;
        }

        if (mx >= x + 240 && mx <= x + 330 && my >= headerY && my <= headerY + 14) {
            rebinding = true;
            rebindStartTime = System.currentTimeMillis();
            return true;
        }

        if (mx >= x + 50 && mx <= x + 330 && my >= headerY + 18 && my <= headerY + 30) {
            focusIdx = FOCUS_NAME; focusDelay = false; clearSelection(); cursorPos = editName.length();
            return true;
        }

        int listStartY = headerY + 40;
        for (int i = 0; i < messages.size(); i++) {
            int ry = (int) (listStartY + (i * itemHeight) - scrollOffset);
            if (mx >= x + 10 && mx <= x + 240 && my >= ry && my <= ry + 16) {
                focusIdx = i; focusDelay = false; clearSelection(); cursorPos = messages.get(i).getText().length();
                return true;
            }
            if (mx >= x + 245 && mx <= x + 305 && my >= ry && my <= ry + 16) {
                focusIdx = i; focusDelay = true;
                return true;
            }
            if (mx >= x + 310 && mx <= x + 330 && my >= ry && my <= ry + 16) {
                messages.remove(i); return true;
            }
        }

        focusIdx = FOCUS_NONE;
        return super.mouseClicked(click, secondary);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= verticalAmount * 15;
        double maxScroll = Math.max(0, (messages.size() * itemHeight) - (height - 120));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    private void save() {
        if (editName.isEmpty()) return;
        ChatMacroManager.removeMacro(macro.getName(), true);
        ChatMacroManager.addMacro(editName, messages, keyCode, true);
        ChatMacroManager.save();
    }
}