package wtf.dupers.dupersunited.features;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.keybinds.JoinServerInviteKeybind;
import wtf.dupers.dupersunited.utils.ColorUtil;

import java.util.Locale;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class ServerInviteManager {

    private static final long INVITE_DURATION_MS = 15_000L;

    private static volatile Invite activeInvite;

    private ServerInviteManager() {}

    public static void receiveInvite(String ip, String inviter, String sentAt) {
        if (ip == null || ip.isBlank() || inviter == null || inviter.isBlank()) return;
        if (isCurrentServer(ip)) return;
        activeInvite = new Invite(ip.trim(), inviter.trim(), sentAt, System.currentTimeMillis() + INVITE_DURATION_MS);
    }

    public static void joinActiveInvite() {
        Invite invite = getActiveInvite();
        if (invite == null) return;

        mc.execute(() -> {
            try {
                ServerData info = new ServerData("Server Invite", invite.ip(), ServerData.Type.OTHER);
                ServerAddress address = ServerAddress.parseString(invite.ip());
                activeInvite = null;
                ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc, address, info, false, null);
            } catch (Exception exception) {
                MainCommand.sendMessage(Component.literal("Failed to join invited server: ")
                        .append(Component.literal(exception.getMessage() == null ? "invalid address" : exception.getMessage()).withStyle(ChatFormatting.RED)), true);
            }
        });
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Invite invite = getActiveInvite();
        if (invite == null) return;

        String keybindText = getJoinKeybindText();
        if (keybindText == null) return;

        Component message = Component.literal(invite.inviter()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
            .append(Component.literal(" has invited you to join ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(invite.ip()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(", click ").withStyle(ChatFormatting.WHITE))
            .append(Component.literal(keybindText).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(" to join!").withStyle(ChatFormatting.WHITE));

        int width = mc.getWindow().getGuiScaledWidth();
        int height = 22;
        int textWidth = mc.font.width(message);
        int x = Math.max(6, (width - textWidth) / 2);

        graphics.fill(0, 0, width, height, ColorUtil.FADED_INDIGO);
        graphics.fill(0, height - 1, width, height, ColorUtil.DEEP_INDIGO);
        graphics.text(mc.font, message, x, 7, 0xFFFFFFFF, true);
    }

    private static Invite getActiveInvite() {
        Invite invite = activeInvite;
        if (invite == null) return null;
        if (isCurrentServer(invite.ip())) {
            activeInvite = null;
            return null;
        }
        if (System.currentTimeMillis() > invite.expiresAtMs()) {
            activeInvite = null;
            return null;
        }
        return invite;
    }

    private static String getJoinKeybindText() {
        int keyCode = JoinServerInviteKeybind.INSTANCE.getKeyCode();
        if (keyCode == -1) return null;
        if (keyCode == InputConstants.UNKNOWN.getValue()) return null;
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString().toUpperCase(Locale.ROOT);
    }

    private static boolean isCurrentServer(String ip) {

        if (mc.getCurrentServer() == null) return false;

        String current = normalizeAddress(mc.getCurrentServer().ip);
        String invited = normalizeAddress(ip);
        return current != null && current.equals(invited);
    }

    private static String normalizeAddress(String address) {
        if (address == null || address.isBlank()) return null;
        try {
            ServerAddress parsed = ServerAddress.parseString(address.trim());
            return (parsed.getHost() + ":" + parsed.getPort()).toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return address.trim().toLowerCase(Locale.ROOT);
        }
    }

    private record Invite(String ip, String inviter, String sentAt, long expiresAtMs) {}
}