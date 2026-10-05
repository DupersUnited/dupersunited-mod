package wtf.dupers.dupersunited.features.screens.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.api.module.Category;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.features.glitchutils.SaveGuiManager;
import wtf.dupers.dupersunited.features.ServerInviteManager;
import wtf.dupers.dupersunited.features.macrogui.MacroManager;
import wtf.dupers.dupersunited.modules.glitcha.MacroGuiSettingsModule;
import wtf.dupers.dupersunited.modules.glitcha.TpsCounterModule;
import wtf.dupers.dupersunited.modules.render.HudModule;
import wtf.dupers.dupersunited.modules.render.WatermarkModule;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.features.screens.TPSDisplay.*;

public final class HudOverlay {

    private HudOverlay() {}

    private static void drawScaled(GuiGraphicsExtractor graphics, HudElement el, Runnable draw) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int actualX = el.getScreenX(screenWidth);

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) actualX, (float) el.y);
        pose.scale(el.scale, el.scale);
        draw.run();
        pose.popMatrix();
    }

    public static void init() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("dupersunited", "overlay"), (graphics, deltaTracker) -> {
            if (mc.player == null || mc.gui.hud.isHidden()) return;
            ServerInviteManager.render(graphics);

            WatermarkModule watermarkMod = MainClient.MODULE_MANAGER.getModule(WatermarkModule.class);
            if (watermarkMod != null && watermarkMod.isEnabled()) {
                drawScaled(graphics, HudEditorScreen.WATERMARK, () -> {
                    String text = watermarkMod.watermarkText.getValue().replace("&", "§");
                    int xOff = HudEditorScreen.WATERMARK.rightAligned ? HudEditorScreen.WATERMARK.getW() - mc.font.width(text) : 0;
                    graphics.text(mc.font, text, xOff, 0, 0xFFFFFFFF, true);
                });
            }

            MacroGuiSettingsModule macroSettings = MainClient.MODULE_MANAGER.getModule(MacroGuiSettingsModule.class);
            boolean macroRunning = MacroManager.isRunning();
            if (macroRunning || (macroSettings != null && macroSettings.alwaysShowHudSetting.getValue())) {
                drawScaled(graphics, HudEditorScreen.MACRO, () -> {
                    Component l1 = Component.literal("Active GUI Macro").withStyle(ChatFormatting.LIGHT_PURPLE);
                    Component l2 = macroRunning
                        ? Component.literal("> ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(MacroManager.getRunningName()).withStyle(ChatFormatting.DARK_PURPLE))
                        : Component.literal("> None").withStyle(ChatFormatting.DARK_GRAY);

                    int xO1 = HudEditorScreen.MACRO.rightAligned ? HudEditorScreen.MACRO.getW() - mc.font.width(l1) : 0;
                    int xO2 = HudEditorScreen.MACRO.rightAligned ? HudEditorScreen.MACRO.getW() - mc.font.width(l2) : 0;

                    graphics.text(mc.font, l1, xO1, 0, 0xFFFFFFFF, true);
                    graphics.text(mc.font, l2, xO2, 12, 0xFFFFFFFF, true);
                });
            }

            if (SaveGuiManager.savedScreen != null) {
                drawScaled(graphics, HudEditorScreen.SAVED_GUI, () -> {
                    Component line1 = Component.literal("Saved GUI").withStyle(ChatFormatting.LIGHT_PURPLE);

                    Component line2 = Component.literal("> ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(SaveGuiManager.guiName)
                            .withStyle(SaveGuiManager.deadGui ? ChatFormatting.RED : ChatFormatting.DARK_GREEN))
                        .append(SaveGuiManager.deadGui
                            ? Component.literal(" (clientside only)").withStyle(ChatFormatting.RED)
                            : Component.empty());

                    int xOff1 = HudEditorScreen.SAVED_GUI.rightAligned ? HudEditorScreen.SAVED_GUI.getW() - mc.font.width(line1) : 0;
                    int xOff2 = HudEditorScreen.SAVED_GUI.rightAligned ? HudEditorScreen.SAVED_GUI.getW() - mc.font.width(line2) : 0;

                    graphics.text(mc.font, line1, xOff1, 0, 0xFFFFFFFF, true);
                    graphics.text(mc.font, line2, xOff2, 12, 0xFFFFFFFF, true);
                });
            }

            if (MainClient.MODULE_MANAGER.isEnabled(TpsCounterModule.class)) {
                if (lastPacketTime == -1) return;
                long timeSinceUpdate = System.currentTimeMillis() - lastPacketTime;
                double seconds = timeSinceUpdate / 1000.0;

                Component text = (seconds > 10.0)
                    ? Component.literal("Server TPS: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal("FROZEN").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                    .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format("%.1fs", seconds)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY))
                    : Component.literal("Server TPS: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(String.format("%.1f", tps)).withStyle(getTpsColorCode(tps)));

                drawScaled(graphics, HudEditorScreen.TPS, () -> {
                    int xOff = HudEditorScreen.TPS.rightAligned ? HudEditorScreen.TPS.getW() - mc.font.width(text) : 0;
                    graphics.text(mc.font, text, xOff, 0, 0xFFFFFFFF, true);
                });
            }

            HudModule hudModule = MainClient.MODULE_MANAGER.getModule(HudModule.class);
            if (hudModule != null && hudModule.isEnabled()) {
                drawScaled(graphics, HudEditorScreen.HUD_LIST, () -> {
                    int i = 0;
                    for (Module module : MainClient.MODULE_MANAGER.modules()) {
                        //yo if you see this fuck you litten this is the correct way to do it bro trust me
                        if (!hudModule.renderSetting.getValue() && module.getCategory().equals(Category.render)) continue;
                        if (!hudModule.miscSetting.getValue() && module.getCategory().equals(Category.misc)) continue;
                        if (!hudModule.glitchaSetting.getValue() && module.getCategory().equals(Category.glitcha)) continue;
                        //   if (!hudModule.exploitSetting.getValue() && module.getCategory().equals(Category.exploit)) continue;

                        if (module.isEnabled()) {
                            Component name = Component.literal(module.getName()).withStyle(ChatFormatting.LIGHT_PURPLE);//TODO: MAKE THIS CUSTOMIZABLE BY USER
                            int xOff = HudEditorScreen.HUD_LIST.rightAligned ? HudEditorScreen.HUD_LIST.getW() - mc.font.width(name) : 0;
                            graphics.text(mc.font, name, xOff, i * 12, 0xFFFFFFFF, true);
                            i++;
                        }
                    }
                });
            }
        });
    }
}