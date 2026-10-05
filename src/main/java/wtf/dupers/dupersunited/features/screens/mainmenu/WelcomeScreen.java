package wtf.dupers.dupersunited.features.screens.mainmenu;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.util.Util;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.utils.ColorUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static wtf.dupers.dupersunited.utils.ColorUtil.MAUVE;

public class WelcomeScreen extends Screen {
    private final Screen parent;
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("dupersunited", "textures/meow/duicon.png");

    private Checkbox agreeCheckbox;
    private Button continueButton;

    public WelcomeScreen(Screen parent) {
        super(Component.literal("Welcome New User"));
        this.parent = parent;
    }

    public Screen getParentScreen() {
        return parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        //tos button link
        this.addRenderableWidget(
            Button.builder(
                    Component.literal("Terms of Service").withStyle(s -> s.withColor(MAUVE)),
                    button -> Util.getPlatform().openUri("https://dupers.wtf/terms")
                )
                .bounds(centerX - 155, this.height - 50, 100, 20)
                .tooltip(Tooltip.create(Component.literal("Click this to view our terms of service.")))
                .build()
        );

        //privacy policy link
        this.addRenderableWidget(
            Button.builder(
                    Component.literal("Privacy Policy").withStyle(s -> s.withColor(MAUVE)),
                    button -> Util.getPlatform().openUri("https://dupers.wtf/privacy")
                )
                .bounds(centerX - 50, this.height - 50, 100, 20)
                .tooltip(Tooltip.create(Component.literal("Click this to view our privacy policy.")))
                .build()
        );

        //continue button (which is disabled unless user agrees to policy)
        this.continueButton = Button.builder(
                Component.literal("Continue"),
                button -> this.onClose()
            )
            .bounds(centerX + 55, this.height - 50, 100, 20)
            .build();
        this.continueButton.active = false;
        this.addRenderableWidget(this.continueButton);

        //checkmark box
        Component checkboxText = Component.literal("I ").withColor(ColorUtil.SUBTEXT)
            .append(Component.literal("agree").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN))
            .append(Component.literal(" to DupersUnited's Terms of Service and Privacy Policy").withColor(ColorUtil.SUBTEXT));
        int checkboxWidth = this.font.width(checkboxText) + 24;

        this.agreeCheckbox = Checkbox.builder(checkboxText, this.font)
            .pos(centerX - (checkboxWidth / 2), this.height - 75)
            .onValueChange((checkbox, checked) -> {
                this.continueButton.active = checked;
            })
            .build();

        this.addRenderableWidget(this.agreeCheckbox);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, ColorUtil.MANTLE);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int iconSize = 48;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON, centerX - (iconSize / 2), centerY - 80, 0f, 0f, iconSize, iconSize, iconSize, iconSize);

        guiGraphics.centeredText(this.font,
            Component.literal("Thank you for using the ").withColor(ColorUtil.PALE_NAVY)
                .append(Component.literal("DupersUnited").withColor(ColorUtil.MAUVE).withStyle(ChatFormatting.BOLD))
                .append(Component.literal(" Mod!").withColor(ColorUtil.PALE_NAVY)),
            centerX, centerY - 20, -1);

        guiGraphics.centeredText(this.font,
            Component.literal("To customize your experience, edit keybinds, or adjust the HUD,").withColor(ColorUtil.SUBTEXT),
            centerX, centerY + 5, -1);

        guiGraphics.centeredText(this.font,
            Component.literal("press ").withColor(ColorUtil.SUBTEXT)
                .append(Component.literal("K").withColor(ColorUtil.SAPPHIRE).withStyle(ChatFormatting.BOLD))
                .append(Component.literal(" to access the Click GUI.").withColor(ColorUtil.SUBTEXT)),
            centerX, centerY + 18, -1);

//        guiGraphics.centeredText(this.font,
//            Component.literal("By using this mod, you agree to our ").withColor(ColorUtil.SUBTEXT)
//                .append(Component.literal("Terms of Service").withColor(ColorUtil.PALE_NAVY))
//                .append(Component.literal(" and ").withColor(ColorUtil.SUBTEXT))
//                .append(Component.literal("Privacy Policy").withColor(ColorUtil.PALE_NAVY))
//                .append(Component.literal(".").withColor(ColorUtil.SUBTEXT)),
//            centerX, this.height - 68, -1);

        guiGraphics.text(this.font,
            Component.literal("v" + FabricLoader.getInstance().getModContainer(MainClient.MOD_ID).orElseThrow().getMetadata().getVersion().getFriendlyString()).withColor(ColorUtil.FADED_INDIGO).withStyle(ChatFormatting.ITALIC),
            5, 5, -1, true);

        //footer status
        if (agreeCheckbox != null && agreeCheckbox.selected()) {
            guiGraphics.centeredText(this.font,
                Component.literal("Press ").withColor(ColorUtil.FADED_NAVY)
                    .append(Component.literal("ESC").withColor(ColorUtil.RED))
                    .append(Component.literal(" or click Continue to proceed!").withColor(ColorUtil.FADED_NAVY)),
                centerX, this.height - 20, -1);
        } else {
            guiGraphics.centeredText(this.font,
                Component.literal("You must accept the terms to continue.").withColor(ColorUtil.RED),
                centerX, this.height - 20, -1);
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (agreeCheckbox != null && agreeCheckbox.selected()) {
            ConfigManager.agreedToPolicy = true;
            ConfigManager.save();

            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(parent != null ? parent : new TitleScreen());
            }
        }
    }

    //make sure users have to actually accept the policy before beign able to leave the welcomescreen
    //99.99% of them won't even read it anyways which is crazy
    @Override
    public boolean shouldCloseOnEsc() {
        return agreeCheckbox != null && agreeCheckbox.selected();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}