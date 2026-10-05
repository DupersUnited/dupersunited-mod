package wtf.dupers.dupersunited.features.screens.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager;
import wtf.dupers.dupersunited.features.macrogui.MacroManager;
import wtf.dupers.dupersunited.features.screens.TPSDisplay;
import wtf.dupers.dupersunited.modules.glitcha.TpsCounterModule;
import wtf.dupers.dupersunited.modules.render.HudModule;
import wtf.dupers.dupersunited.modules.render.WatermarkModule;

import java.util.List;

public class HudEditorScreen extends Screen {

    public static final HudElement WATERMARK = new HudElement("Watermark", 4, 4);
    public static final HudElement MACRO = new HudElement("Macro", 4, 21);
    public static final HudElement SAVED_GUI = new HudElement("Saved GUI", 4, 69);
    public static final HudElement TPS = new HudElement("TPS Counter", 4, 93);
    public static final HudElement HUD_LIST = new HudElement("HUD", 4, 117);

    private static final List<HudElement> ELEMENTS = List.of(WATERMARK, SAVED_GUI, TPS, HUD_LIST, MACRO);
    private static final int GRID = 4;
    private static final float SCALE_STEP = 0.25f;
    private static final float SCALE_MIN  = 0.5f;
    private static final float SCALE_MAX  = 4.0f;
    private static final int RESET_W = 80;
    private static final int RESET_H = 12;

    private HudElement dragging = null;
    private int offX, offY;

    public HudEditorScreen() {
        super(Component.literal("HUD Editor"));
    }

    private int snap(int value) {
        return Math.round((float) value / GRID) * GRID;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x80000000);

        for (HudElement el : ELEMENTS) {
            int sx = el.getScreenX(width);
            int w = el.getW();
            int h = el.getH();
            boolean hovered = el.isHovered(mouseX, mouseY, width);
            int border = hovered ? 0xFFDA70D6 : 0xFF888888;

            graphics.fill(sx, el.y, sx + w, el.y + h, 0x55000000);
            graphics.fill(sx, el.y, sx + w, el.y + 1, border);
            graphics.fill(sx, el.y + h - 1, sx + w, el.y + h, border);
            graphics.fill(sx, el.y, sx + 1, el.y + h, border);
            graphics.fill(sx + w - 1, el.y, sx + w, el.y + h, border);

            var matrices = graphics.pose();
            matrices.pushMatrix();
            matrices.translate((float) sx + 2.0f, (float) el.y + 1.0f);
            matrices.scale(el.scale, el.scale);
            graphics.text(font, getPreview(el), 0, 0, 0xFFFFFFFF, true);
            matrices.popMatrix();

            if (hovered) {
                String alignIcon = el.rightAligned ? "◀" : "▶";
                String scaleLabel = String.format("%.1fx %s", el.scale, alignIcon);
                int labelX = sx + w - font.width(scaleLabel) - 2;
                graphics.text(font, scaleLabel, labelX, el.y + 1, 0xFFCBA6F7, true);
            }
        }

        int btnX = width / 2 - RESET_W / 2;
        int btnY = height - 28;
        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + RESET_W && mouseY >= btnY && mouseY <= btnY + RESET_H;
        graphics.fill(btnX, btnY, btnX + RESET_W, btnY + RESET_H, btnHovered ? 0xFFAA0000 : 0xFF880000);
        graphics.centeredText(font, Component.literal("Reset HUD").withStyle(ChatFormatting.RED), btnX + RESET_W / 2, btnY + 2, 0xFFFFFFFF);

        graphics.centeredText(font,
            Component.literal("Drag to move  |  ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Scroll").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal(" to resize  |  ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("ESC").withStyle(ChatFormatting.RED))
                .append(Component.literal(" to close").withStyle(ChatFormatting.GRAY)),
            width / 2, height - 14, 0xFFFFFFFF);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (HudElement el : ELEMENTS) {
            if (el.isHovered(mouseX, mouseY, width)) {
                el.scale = Math.max(SCALE_MIN, Math.min(SCALE_MAX, el.scale + (float) verticalAmount * SCALE_STEP));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // IGNORE THE HORRIBLE CODE IT DOES THE JOB FUCK YOU VINZY
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        if (click.button() == 0) {
            int btnX = width / 2 - RESET_W / 2;
            int btnY = height - 28;
            if (click.x() >= btnX && click.x() <= btnX + RESET_W && click.y() >= btnY && click.y() <= btnY + RESET_H) {
                resetAll();
                return true;
            }

            for (HudElement el : ELEMENTS) {
                if (el.isHovered(click.x(), click.y(), width)) {
                    dragging = el;
                    int screenX = el.getScreenX(width);
                    el.rightAligned = false;
                    el.x = screenX;
                    offX = (int) click.x() - screenX;
                    offY = (int) click.y() - el.y;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (dragging != null) {
            int rawX = (int) click.x() - offX;
            int rawY = (int) click.y() - offY;

            dragging.x = snap(Math.max(0, Math.min(rawX, width - dragging.getW())));
            dragging.y = snap(Math.max(0, Math.min(rawY, height - dragging.getH())));
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging != null) {
            int centerX = dragging.x + dragging.getW() / 2;
            if (centerX > width / 2) {
                dragging.rightAligned = true;
                dragging.x = width - (dragging.x + dragging.getW());
            } else {
                dragging.rightAligned = false;
            }
            dragging = null;
        }
        return super.mouseReleased(click);
    }

    private void resetAll() {
        for (HudElement el : ELEMENTS) {
            el.scale = 1.0f;
            el.rightAligned = false;
        }
        WATERMARK.x = 4; WATERMARK.y = 4;
        MACRO.x = 4; MACRO.y = 21;
        SAVED_GUI.x = 4; SAVED_GUI.y = 69;
        TPS.x = 4; TPS.y = 93;
        HUD_LIST.x = 4; HUD_LIST.y = 117;
    }

    private String getPreview(HudElement el) {
        switch (el.id) {
            case "Watermark": {
                WatermarkModule wm = MainClient.MODULE_MANAGER.getModule(WatermarkModule.class);
                return (wm != null && wm.isEnabled()) ? wm.watermarkText.getValue().replace('&', '§') : ChatFormatting.GRAY + "Watermark " + ChatFormatting.DARK_GRAY + "(disabled)";
            }
            case "Macro": {
                return MacroManager.isRunning() ? ChatFormatting.LIGHT_PURPLE + "Active Macro: " + ChatFormatting.DARK_PURPLE + MacroManager.getRunningName() : ChatFormatting.LIGHT_PURPLE + "Active GUI Macro";
            }
            case "Saved GUI": {
                return (SaveGuiManager.savedScreen != null) ? ChatFormatting.LIGHT_PURPLE + "Saved: " + (SaveGuiManager.deadGui ? ChatFormatting.RED : ChatFormatting.DARK_AQUA) + SaveGuiManager.guiName : ChatFormatting.LIGHT_PURPLE + "Saved GUI";
            }
            case "TPS Counter": {
                if (!MainClient.MODULE_MANAGER.isEnabled(TpsCounterModule.class)) return ChatFormatting.GRAY + "TPS Counter " + ChatFormatting.DARK_GRAY + "(disabled)";
                if (TPSDisplay.lastPacketTime == -1) return ChatFormatting.LIGHT_PURPLE + "Server TPS: " + ChatFormatting.GRAY + "--";
                return String.format("%sServer TPS: %%s%%.1f", ChatFormatting.LIGHT_PURPLE, TPSDisplay.getTpsColorCode(TPSDisplay.tps), TPSDisplay.tps);
            }
            case "HUD": {
                return !MainClient.MODULE_MANAGER.isEnabled(HudModule.class) ? ChatFormatting.GRAY + "Module List " + ChatFormatting.DARK_GRAY + "(disabled)" : ChatFormatting.LIGHT_PURPLE + "Module List";
            }
            default: return el.id;
        }
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}