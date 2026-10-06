package wtf.dupers.dupersunited.features.screens.mainmenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.features.ModSettings;
import wtf.dupers.dupersunited.features.screens.ui.DuScreen;

import java.util.List;

import static wtf.dupers.dupersunited.features.screens.ui.Theme.*;

public class SettingsScreen extends DuScreen {
    private static final int ROW_HEIGHT = 26;
    private static final int START_Y = 66;
    private static final int BTN_WIDTH = 90;
    private static final int BTN_HEIGHT = 18;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Settings"), parent, "Settings");
    }

    private List<BooleanSetting> settings() {
        return List.of(ModSettings.sendToggleMsg, ModSettings.showModCapes, ModSettings.showDebugMessages, ModSettings.showBroadcasts, ModSettings.showServerInvites);
    }

    private int panelLeft() {
        return this.width / 2 - 160;
    }

    private int panelRight() {
        return this.width / 2 + 160;
    }

    @Override
    protected void init() {
        rebuildButtons();
    }

    private void rebuildButtons() {
        this.clearWidgets();

        List<BooleanSetting> all = settings();
        for (int i = 0; i < all.size(); i++) {
            BooleanSetting setting = all.get(i);
            int y = START_Y + i * ROW_HEIGHT + (ROW_HEIGHT - BTN_HEIGHT) / 2;
            this.addRenderableWidget(Button.builder(
                setting.getValue() ? Component.literal("ON").withStyle(ChatFormatting.GREEN) : Component.literal("OFF").withStyle(ChatFormatting.GRAY),
                _ -> {
                    setting.toggle();
                    ConfigManager.save();
                    rebuildButtons();
                }
            ).bounds(panelRight() - BTN_WIDTH - 6, y, BTN_WIDTH, BTN_HEIGHT).build());
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawStructure(graphics, mouseX, mouseY);

        graphics.centeredText(
            this.font,
            Component.literal("Mod Settings").withStyle(ChatFormatting.BOLD),
            this.width / 2,
            30,
            secondary
        );

        int panelBottom = START_Y + settings().size() * ROW_HEIGHT + 8;
        graphics.fill(panelLeft(), START_Y, panelRight(), panelBottom, header);

        List<BooleanSetting> all = settings();
        for (int i = 0; i < all.size(); i++) {
            BooleanSetting setting = all.get(i);
            int y = START_Y + i * ROW_HEIGHT;

            boolean hovered = mouseX >= panelLeft() && mouseX <= panelRight()
                && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, border);
            } else if (i % 2 == 0) {
                graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, 0x08FFFFFF);
            }

            graphics.text(this.font, Component.literal(setting.getName()),
                panelLeft() + 10, y + (ROW_HEIGHT - 9) / 2, text);
            graphics.fill(panelLeft() + 3, y + ROW_HEIGHT / 2 - 2,
                panelLeft() + 5, y + ROW_HEIGHT / 2 + 2,
                setting.getValue() ? accent : edge);
            graphics.fill(panelLeft(), y + ROW_HEIGHT - 1, panelRight(), y + ROW_HEIGHT, border);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}
