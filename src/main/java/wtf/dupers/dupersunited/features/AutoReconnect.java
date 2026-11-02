package wtf.dupers.dupersunited.features;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.misc.AutoReconnectModule;

public class AutoReconnect {

    private static final int RECONNECT_DELAY_SECONDS = 5;

    private static int ticksRemaining = -1;
    private static String cachedAddress = null;
    private static Button cancelButton = null;

    public static void cacheAddress(String address) {
        cachedAddress = address;
        //    MainClient.LOGGER.info("cached address {}", cachedAddress);
    }

    public static void startCountdown() {
        if (cachedAddress == null) {
            //     MainClient.LOGGER.warn("no cached server address, can't reconnect!!");
            return;
        }

        if (!MainClient.MODULE_MANAGER.isEnabled(AutoReconnectModule.class)) {
            //    MainClient.LOGGER.info("autoreconnect is disabled, skipping");
            return;
        }

        ticksRemaining = RECONNECT_DELAY_SECONDS * 20;
    }

    public static void setCancelButton(Button btn) {
        cancelButton = btn;
    }

    public static void cancel() {
        ticksRemaining = -1;

        if (cancelButton != null) {
            cancelButton.setMessage(Component.literal("Reconnect Cancelled").withStyle(ChatFormatting.RED));
            cancelButton.active = false;
            cancelButton = null;
        }
    }

    public static void reconnectNow() {
        cancel();
        reconnect();
    }

    public static void tick() {
        if (ticksRemaining <= 0) return;

        ticksRemaining--;

        if (cancelButton != null && cancelButton.active) {
            cancelButton.setMessage(Component.literal("Reconnecting in " + getSecondsRemaining() + "s"));
        }

        if (ticksRemaining == 0) {
            ticksRemaining = -1;
            cancelButton = null;
            reconnect();
        }
    }

    public static int getSecondsRemaining() {
        return (int) Math.ceil(ticksRemaining / 20.0);
    }

    public static boolean isCountingDown() {
        return ticksRemaining > 0;
    }

    private static void reconnect() {
        Minecraft client = Minecraft.getInstance();

        if (cachedAddress == null) {
            //   MainClient.LOGGER.warn("reconnect called but no cached address!");
            return;
        }

        //  MainClient.LOGGER.info("reconnecting to {}", cachedAddress);

        ServerData info = new ServerData("AutoReconnect", cachedAddress, ServerData.Type.OTHER);
        ServerAddress address = ServerAddress.parseString(cachedAddress);

        ConnectScreen.startConnecting(
            new JoinMultiplayerScreen(new TitleScreen()),
            client,
            address,
            info,
            false,
            null
        );
    }
}