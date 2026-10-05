package wtf.dupers.dupersunited.mixin.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.account.SessionAPI;
import wtf.dupers.dupersunited.features.account.SessionManager;
import wtf.dupers.dupersunited.features.proxies.ProxyConfigManager;
import wtf.dupers.dupersunited.features.proxies.ProxyProfiles;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Shadow
    private SplashRenderer splash;

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.splash = new SplashRenderer(Component.literal(SharedVariables.randomQuote()));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        String username = SessionManager.getUsername();

        if (SessionManager.isSessionValid == null && !SessionManager.hasValidationStarted) {
            SessionManager.hasValidationStarted = true;

            new Thread(() -> {
                SessionManager.isSessionValid = SessionAPI.validateSession(this.minecraft.getUser().getAccessToken());
                SessionManager.hasValidationStarted = false;
            }, "SessionValidationThread").start();
        }

        Component playerDisplay = Component.literal("IGN: ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(username).withStyle(ChatFormatting.AQUA));
        graphics.text(this.font, playerDisplay, 5, 7, -1, true);

        ProxyProfiles active = ProxyConfigManager.getActiveProfile();
        Component proxyComponent = Component.literal("none").withStyle(ChatFormatting.RED);
        if (ProxyConfigManager.globalEnabled && active != null) {
            proxyComponent = Component.literal(active.name).withStyle(ChatFormatting.GREEN);
        }

        graphics.text(this.font,
            Component.literal("Proxy: ").withStyle(ChatFormatting.GRAY)
                .append(proxyComponent),
            5, 18, -1, true
        );
        }
}