package wtf.dupers.dupersunited.features.screens.macroscreen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import wtf.dupers.dupersunited.features.macros.Macros;
import wtf.dupers.dupersunited.features.macros.Macros.MacroAction;

import java.util.*;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.utils.ColorUtil.*;

public class MacroConfigScreen extends Screen {
    private static final int FOCUS_NONE = -2;
    private static final int FOCUS_NAME = -1;
    private static final int MOUSE_OFFSET = 100;

    private final Screen parent;
    private Macros.Macro macro;
    private String editName;
    private final List<MacroAction> actions;

    private int focusIdx = FOCUS_NONE;
    private boolean focusDelay = false;
    private boolean rebinding = false;
    private long rebindStartTime = 0;
    private int keyCode;

    private int cursorPos = 0;
    private int selectionAnchor = -1;

    private double scrollOffset = 0;
    private final int itemHeight = 25;

    public MacroConfigScreen(Screen parent, Macros.Macro macro) {
        super(Component.literal("Macro Editor"));
        this.parent = parent;
        this.macro = macro;
        this.editName = macro.name();
        this.actions = new ArrayList<>(macro.actions());
        this.keyCode = macro.key();
    }

    private static String rowText(MacroAction action) {
        return switch (action.kind()) {
            case SEND_CHAT -> action.text() == null ? "" : action.text();
            case SEND_COMMAND -> "/" + (action.text() == null ? "" : action.text());
            case WAIT_TICKS, WAIT_MS -> "(wait)";
            default -> action.kind().name().toLowerCase();
        };
    }

    private static long rowDelay(MacroAction action) {
        return switch (action.kind()) {
            case WAIT_TICKS, WAIT_MS -> action.count();
            case SEND_CHAT, SEND_COMMAND -> action.sinceLastMs();
            default -> -1;
        };
    }

    private static boolean rowEditable(MacroAction action) {
        return action.kind() == Macros.ActionKind.SEND_CHAT || action.kind() == Macros.ActionKind.SEND_COMMAND;
    }

    private static boolean rowDelayEditable(MacroAction action) {
        return action.kind() == Macros.ActionKind.WAIT_TICKS || action.kind() == Macros.ActionKind.WAIT_MS || action.kind() == Macros.ActionKind.SEND_CHAT || action.kind() == Macros.ActionKind.SEND_COMMAND;
    }

    private static MacroAction withText(MacroAction action, String val) {
        return switch (action.kind()) {
            case SEND_COMMAND -> MacroAction.sendCommand(val.startsWith("/") ? val.substring(1) : val).withSinceLastMs(action.sinceLastMs());
            default -> MacroAction.sendChat(val).withSinceLastMs(action.sinceLastMs());
        };
    }

    private static MacroAction withDelay(MacroAction action, int delay) {
        return switch (action.kind()) {
            case WAIT_TICKS -> MacroAction.waitTicks(delay);
            case WAIT_MS -> MacroAction.waitMs(delay);
            case SEND_CHAT, SEND_COMMAND -> withText(action, action.text() == null ? "" : (action.kind() == Macros.ActionKind.SEND_COMMAND ? "/" + action.text() : action.text())).withSinceLastMs(delay);
            default -> action;
        };
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
        if (focusIdx >= 0 && focusIdx < actions.size() && rowEditable(actions.get(focusIdx))) return rowText(actions.get(focusIdx));
        return "";
    }

    private void updateCurrentText(String val) {
        if (focusIdx == FOCUS_NAME) editName = val;
        else if (focusIdx >= 0 && focusIdx < actions.size() && rowEditable(actions.get(focusIdx))) {
            actions.set(focusIdx, withText(actions.get(focusIdx), val));
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
        for (int i = 0; i < actions.size(); i++) {
            int ry = (int) (listStartY + (i * itemHeight) - scrollOffset);
            if (ry > listEndY || ry + itemHeight < listStartY) continue;
            MacroAction m = actions.get(i);
            boolean editable = rowEditable(m);
            boolean delayEditable = rowDelayEditable(m);

            graphics.fill(x + 10, ry, x + 240, ry + 16, (focusIdx == i && !focusDelay && editable) ? FIELD_FOCUSED : 0x22000000);
            if (focusIdx == i && !focusDelay && editable) renderTextField(graphics, rowText(m), x + 15, ry + 4);
            else graphics.text(font, rowText(m), x + 15, ry + 4, editable ? 0xFFFFFFFF : 0xFF888888);

            graphics.fill(x + 245, ry, x + 305, ry + 16, (focusIdx == i && focusDelay && delayEditable) ? FIELD_FOCUSED : 0x22000000);
            graphics.centeredText(font, delayEditable ? rowDelay(m) + (m.kind() == Macros.ActionKind.WAIT_TICKS ? "t" : "ms") : "--", x + 275, ry + 4, PEACH);

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

        if (focusIdx >= 0 && focusDelay && focusIdx < actions.size() && rowDelayEditable(actions.get(focusIdx))) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String s = String.valueOf(rowDelay(actions.get(focusIdx)));
                int newDelay = (s.length() > 1) ? Integer.parseInt(s.substring(0, s.length() - 1)) : 0;
                actions.set(focusIdx, withDelay(actions.get(focusIdx), newDelay));
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

        if (focusDelay && focusIdx >= 0 && focusIdx < actions.size() && rowDelayEditable(actions.get(focusIdx))) {
            String typed = input.codepointAsString();
            if (!typed.isEmpty() && Character.isDigit(typed.charAt(0))) {
                char c = typed.charAt(0);
                String next = rowDelay(actions.get(focusIdx)) + String.valueOf(c);
                try {
                    long val = Long.parseLong(next);
                    if (val <= 99999) actions.set(focusIdx, withDelay(actions.get(focusIdx), (int) val));
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
            actions.add(MacroAction.sendChat(""));
            return true;
        }

        if (mx >= x + 90 && mx <= x + 190 && my >= headerY && my <= headerY + 14) {
            Macros.getInstance().deleteMacro(macro);
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
        for (int i = 0; i < actions.size(); i++) {
            int ry = (int) (listStartY + (i * itemHeight) - scrollOffset);
            if (mx >= x + 10 && mx <= x + 240 && my >= ry && my <= ry + 16) {
                if (!rowEditable(actions.get(i))) { focusIdx = FOCUS_NONE; return true; }
                focusIdx = i; focusDelay = false; clearSelection(); cursorPos = rowText(actions.get(i)).length();
                return true;
            }
            if (mx >= x + 245 && mx <= x + 305 && my >= ry && my <= ry + 16) {
                focusIdx = i; focusDelay = true;
                return true;
            }
            if (mx >= x + 310 && mx <= x + 330 && my >= ry && my <= ry + 16) {
                actions.remove(i); return true;
            }
        }

        focusIdx = FOCUS_NONE;
        return super.mouseClicked(click, secondary);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= verticalAmount * 15;
        double maxScroll = Math.max(0, (actions.size() * itemHeight) - (height - 120));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    private void save() {
        if (editName.isEmpty()) return;
        if (!editName.equals(macro.name())) {
            if (Macros.getInstance().renameMacro(macro, editName)) {
                macro = Macros.getInstance().getMacro(editName);
            } else {
                editName = macro.name();
            }
        }
        if (macro == null) return;
        Macros.getInstance().setMacroActions(macro, actions);
        Macros.getInstance().setMacroKey(macro, keyCode);
    }
}