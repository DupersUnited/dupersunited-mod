package wtf.dupers.dupersunited.features.screens.mainmenu;

import wtf.dupers.dupersunited.utils.ColorUtil;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class WelcomeScreen extends Screen {
    private final Screen parent;
    private static final Identifier ICON = Identifier.of("dupersunited", "textures/meow/duicon.png");

    public WelcomeScreen(Screen parent) {
        super(Text.literal("Welcome New User"));
        this.parent = parent;
    }

    public Screen getParentScreen() {
        return parent;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, ColorUtil.MANTLE);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int iconSize = 48;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, ICON, centerX - (iconSize / 2), centerY - 80, 0f, 0f, iconSize, iconSize, iconSize, iconSize);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Thank you for using the ").withColor(ColorUtil.PALE_NAVY)
                        .append(Text.literal("DupersUnited").withColor(ColorUtil.MAUVE).formatted(Formatting.BOLD))
                        .append(Text.literal(" Mod!").withColor(ColorUtil.PALE_NAVY)),
                centerX, centerY - 20, -1);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("To customize your experience, edit keybinds, or adjust the HUD,").withColor(ColorUtil.SUBTEXT),
                centerX, centerY + 5, -1);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("press ").withColor(ColorUtil.SUBTEXT)
                        .append(Text.literal("K").withColor(ColorUtil.SAPPHIRE).formatted(Formatting.BOLD))
                        .append(Text.literal(" to access the Click GUI.").withColor(ColorUtil.SUBTEXT)),
                centerX, centerY + 18, -1);

        context.drawTextWithShadow(this.textRenderer,
                Text.literal("v1.0.0").withColor(ColorUtil.FADED_INDIGO).formatted(Formatting.ITALIC),
                5, 5, -1);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Press ").withColor(ColorUtil.FADED_NAVY)
                        .append(Text.literal("ESC").withColor(ColorUtil.RED))
                        .append(Text.literal(" to enter the Main Menu").withColor(ColorUtil.FADED_NAVY)),
                centerX, this.height - 30, -1);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent != null ? parent : new TitleScreen());
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}