package wtf.dupers.dupersunited.mixin.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Shadow;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.commands.subcommands.DupeCommand;
import wtf.dupers.dupersunited.compat.MeteorCompat;
import wtf.dupers.dupersunited.features.ServerAlertConfig;
import wtf.dupers.dupersunited.features.proxies.ProxyConfigManager;
import wtf.dupers.dupersunited.features.proxies.ProxyProfiles;
import wtf.dupers.dupersunited.features.screens.mainmenu.alerts.HallOfFame;
import wtf.dupers.dupersunited.features.screens.mainmenu.alerts.HallOfShame;
import wtf.dupers.dupersunited.features.screens.mainmenu.alerts.NoProxyWarningScreen;
import wtf.dupers.dupersunited.features.screens.mainmenu.alerts.UnsafeModuleWarningScreen;
import wtf.dupers.dupersunited.features.account.AccountsScreen;
import wtf.dupers.dupersunited.features.screens.ClickGui;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.modules.misc.InvDropModule;
import wtf.dupers.dupersunited.modules.misc.NoFallModule;
import wtf.dupers.dupersunited.modules.misc.ServerAlertsModule;
import wtf.dupers.dupersunited.modules.misc.VanillaFlyModule;
import wtf.dupers.dupersunited.modules.misc.WarnUnsafeModule;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static wtf.dupers.dupersunited.MainClient.mc;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    @Shadow
    public abstract void join(ServerData data);

    protected MultiplayerScreenMixin(Component title) { super(title); }

    @Unique private Button dupersunited$accountsButton;
    @Unique private Button dupersunited$settingsButton;
    @Unique private int dupersunited$lastWidth = -1;
    @Unique private int dupersunited$lastHeight = -1;

    @Unique private boolean dupersunited$bypassHosCheck = false;

    @Unique
    private final List<Class<? extends Module>> UNSAFE_MODULES = List.of(VanillaFlyModule.class, InvDropModule.class, NoFallModule.class);

    @Unique
    private boolean hasUnsafeModulesEnabled() {
        return MainClient.MODULE_MANAGER.getEnabledModules().stream()
                .anyMatch(UNSAFE_MODULES::contains) || MeteorCompat.shouldWarnUnsafeModules();
    }

    @Unique
    private int dupersunited$accountsButtonX() {
        String currentUsername = mc.getUser().getName();
        int textWidth = this.font.width(
            Component.literal("IGN: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(currentUsername).withStyle(ChatFormatting.AQUA))
        );
        return Math.max(90, 5 + textWidth + 10);
    }

    @Unique
    private void dupersunited$updateButtonPositions() {
        if (dupersunited$accountsButton != null) {
            dupersunited$accountsButton.visible = this.width >= 600;
            dupersunited$accountsButton.setPosition(dupersunited$accountsButtonX(), 3);
        }
        if (dupersunited$settingsButton != null) {
            dupersunited$settingsButton.visible = this.width >= 600;
            dupersunited$settingsButton.setPosition(this.width - 85, this.height - 25);
        }
    }

    @Inject(at = @At("TAIL"), method = "init")
    private void dupersunited$addProxyButton(CallbackInfo ci) {
        dupersunited$accountsButton = this.addRenderableWidget(Button.builder(
                Component.literal("Accounts"),
                _ -> mc.gui.setScreen(new AccountsScreen(this))
        ).bounds(dupersunited$accountsButtonX(), 3, 80, 20).build());

        dupersunited$accountsButton.visible = this.width >= 600;

        dupersunited$settingsButton = this.addRenderableWidget(Button.builder(
            Component.literal("Settings"),
            _ -> mc.gui.setScreen(new ClickGui(this))
        ).bounds(this.width - 85, this.height - 25, 80, 20).build());

        dupersunited$settingsButton.visible = this.width >= 600;

        dupersunited$lastWidth = this.width;
        dupersunited$lastHeight = this.height;

        AccountsScreen.preloadAccounts();
    }

    @Inject(method = "join", at = @At("HEAD"), cancellable = true)
    private void dupersunited$checkProxy(ServerData serverData, CallbackInfo ci) {
        if (DupeCommand.amILarpingItUp) ClientTickEvents.END_CLIENT_TICK.register(_ -> {
            throw new RuntimeException("Failed to establish a connection with the Hygot backend!");//ouuu shii @vinzy-dev this is a cold error
        });

        if (MainClient.MODULE_MANAGER.isEnabled(WarnUnsafeModule.class) && hasUnsafeModulesEnabled()) {
            mc.gui.setScreen(new UnsafeModuleWarningScreen(this, serverData));
            ci.cancel();
            return;
        }

        if (MainClient.MODULE_MANAGER.isEnabled(ServerAlertsModule.class) && !dupersunited$bypassHosCheck) {
            if (!ServerAlertConfig.isDismissed(serverData.ip)) {
                if (HallOfShame.lookupCached(serverData.ip)) {
                    mc.gui.setScreen(new HallOfShame.WarningScreen(this, serverData));
                    ci.cancel();
                    return;
                }
                if (HallOfFame.lookupCached(serverData.ip)) {
                    mc.gui.setScreen(new HallOfFame.NoticeScreen(this, serverData));
                    ci.cancel();
                    return;
                }

                ci.cancel();
                HallOfShame.checkAsync(serverData.ip).thenAccept(flagged ->
                    mc.execute(() -> {
                        if (flagged && !ServerAlertConfig.isDismissed(serverData.ip)) {
                            mc.gui.setScreen(new HallOfShame.WarningScreen(this, serverData));
                        } else {
                            dupersunited$bypassHosCheck = true;
                            this.join(serverData);
                            dupersunited$bypassHosCheck = false;
                        }
                    })
                );
                return;
            }

            if (ProxyConfigManager.proxyWarningEnabled && ProxyConfigManager.shouldWarn()) {
                mc.gui.setScreen(new NoProxyWarningScreen(this, serverData));
                ci.cancel();
            }
        }
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (this.width != dupersunited$lastWidth || this.height != dupersunited$lastHeight) {
            dupersunited$lastWidth = this.width;
            dupersunited$lastHeight = this.height;
        }
        dupersunited$updateButtonPositions();

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (this.width < 600) return;

        String currentUsername = mc.getUser().getName();
        graphics.text(
            font,
            Component.literal("IGN: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(currentUsername).withStyle(ChatFormatting.AQUA)),
            5, 7, 0xFFFFFFFF
        );

        ProxyProfiles active = ProxyConfigManager.getActiveProfile();
        Component proxyComponent = Component.literal("none").withStyle(ChatFormatting.RED);
        if (ProxyConfigManager.globalEnabled && active != null) {
            proxyComponent = Component.literal(active.name).withStyle(ChatFormatting.GREEN);
        }

        graphics.text(
            font,
            Component.literal("Proxy: ").withStyle(ChatFormatting.GRAY)
                .append(proxyComponent),
            5, 18, 0xFFFFFFFF
        );
    }
}