package wtf.dupers.dupersunited.mixin.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import wtf.dupers.dupersunited.features.AutoReconnect;
import wtf.dupers.dupersunited.features.account.OfflineAccountManager;
import wtf.dupers.dupersunited.features.account.OfflineAccountManager.OfflineAccount;
import wtf.dupers.dupersunited.features.account.SessionManager;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static wtf.dupers.dupersunited.MainClient.mc;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

    protected DisconnectedScreenMixin() {
        super(Component.empty());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dupersunited$startReconnect(CallbackInfo ci) {
        AutoReconnect.startCountdown();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        if (AutoReconnect.isCountingDown()) {
            Button btn = Button.builder(
                    Component.literal("Reconnecting in " + AutoReconnect.getSecondsRemaining() + "s"),
                    b -> {
                        AutoReconnect.cancel();
                        b.setMessage(Component.literal("Reconnect Cancelled").withStyle(ChatFormatting.RED));
                        b.active = false;
                    }
                ).bounds(screenWidth / 2 - 100, screenHeight / 2 + 55, 200, 20)
                .tooltip(Tooltip.create(Component.literal("Click this button to cancel auto reconnect!")))
                .build();

            AutoReconnect.setCancelButton((Button) btn);
            this.addRenderableWidget(btn);
        }

        Button randomBtn = Button.builder(
                Component.literal("Reconnect with random name"),
                _ -> {
                    OfflineAccount account = OfflineAccountManager.createRandom();
                    SessionManager.setSession(SessionManager.createSession(account.username, account.uuid, ""));
                    AutoReconnect.reconnectNow();
                }
            ).bounds(screenWidth / 2 - 100, screenHeight / 2 + (AutoReconnect.isCountingDown() ? 79 : 55), 200, 20)
            .tooltip(Tooltip.create(Component.literal("Logs in with a fresh random offline name and rejoins instantly")))
            .build();

        this.addRenderableWidget(randomBtn);
    }
}