package wtf.dupers.dupersunited.features.glitchutils;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.modules.glitcha.PayAllSettingsModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.regex.Pattern;

import static wtf.dupers.dupersunited.MainClient.mc;

public class PayAllManager {
    private static final Deque<String> queue = new ArrayDeque<>();
    private static final int MAX_PLAYER_NAME_LENGTH = 16;
    private static final int MIN_PLAYER_NAME_LENGTH = 3;
    private static boolean running = false;
    private static int tickCounter = 0;
    private static int TICK_DELAY;
    private static Pattern MROW_NYA = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");
    private static Pattern NYA_MROW = Pattern.compile("^\\.[a-zA-Z0-9_]{2,16}$");
    private static final Set<String> HARDCODED_EXCLUSIONS = Set.of("*", "**", "***", "all", "everyone", "@a", "@p", "@r", "@s");

    private static boolean isValidPlayerName(String name) {
        if (name == null || name.isEmpty()) return false;
        String trimmed = name.trim();
        if (HARDCODED_EXCLUSIONS.contains(trimmed.toLowerCase(Locale.ROOT))) return false;

        boolean isGeyserName = trimmed.startsWith(".");
        int minLength = isGeyserName ? 4 : MIN_PLAYER_NAME_LENGTH;
        int maxLength = isGeyserName ? 17 : MAX_PLAYER_NAME_LENGTH;

        if (trimmed.length() < minLength || trimmed.length() > maxLength) return false;

        if (isGeyserName) {
            return NYA_MROW.matcher(trimmed).matches();
        }
        return MROW_NYA.matcher(trimmed).matches();
    }

    private static PayAllSettingsModule getSettings() {
        return MainClient.MODULE_MANAGER.getModule(PayAllSettingsModule.class);
    }

    public static Collection<PlayerInfo> getPlayerList() {
        return mc.getConnection().getOnlinePlayers();
    }

    public static void startPayAll() {
        if (mc.getConnection() == null) return;

        PayAllSettingsModule settings = getSettings();
        String rawAmount = settings.payAmount.getValue().trim();

        try {
            Double.parseDouble(rawAmount);
        } catch (NumberFormatException e) {
            MainCommand.sendMessage(Component.literal("Invalid amount: ").withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(rawAmount).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.WHITE), true);
            return;
        }

        TICK_DELAY = settings.delayBetweenPay.getValue();
        int playerCount = mc.getConnection().getOnlinePlayers().size() - 1;

        String amount = rawAmount;
        if (settings.autoDivideAmt.getValue() && playerCount > 0) {
            try {
                double total = Double.parseDouble(rawAmount);
                amount = String.valueOf((int)(total / playerCount));
            } catch (NumberFormatException ignored) {}
        }

        queue.clear();
        for (PlayerInfo entry : getPlayerList()) {
            String name = entry.getProfile().name();
            if (name == null || name.equals(mc.getUser().getName())) continue;
            if (!isValidPlayerName(name)) {
                //ChatLib.message("Couldn't pay" + name + " (name isn't supposed to be possible?)"); whatever it's technically fixed i see the issue but i don't wanna look into it more because that's work
                continue;
            }
            queue.add(name + ";" + amount);
        }

        MainCommand.sendMessage(Component.literal("Added ")
                .append(Component.literal(String.valueOf(queue.size())).withStyle(ChatFormatting.AQUA))
                .append(" players to the pay all queue.")
                .withStyle(ChatFormatting.WHITE), true);

        running = true;
        tickCounter = 0;
    }

    public static void stopPayAll() {
        running = false;
        queue.clear();
        tickCounter = 0;

        MainCommand.sendMessage(Component.literal("Payall ")
                .append(Component.literal("stopped").withStyle(ChatFormatting.AQUA))
                .append(".")
                .withStyle(ChatFormatting.WHITE), true);
    }

    public static void onTick() {
        if (!running || mc.player == null) return;

        tickCounter++;
        if (tickCounter < TICK_DELAY) return;
        tickCounter = 0;

        if (queue.isEmpty()) {
            running = false;
            return;
        }

        PayAllSettingsModule settings = getSettings();
        String[] entry = queue.poll().split(";", 3);
        String name = entry[0];
        String amount = entry.length > 1 ? entry[1] : "0";
        boolean isDouble = entry.length > 2 && entry[2].equals("double");

        String cmdTemplate = settings.commandSetting.getValue();
        String cmd = cmdTemplate
                .replace("<p>", name)
                .replace("<a>", amount)
                .replaceFirst("^/", "");

        mc.player.connection.sendCommand(cmd);

        MainCommand.sendMessage(Component.literal("Sent ")
                .append(Component.literal(name).withStyle(ChatFormatting.AQUA))
                .append(" ")
                .append(Component.literal(amount).withStyle(ChatFormatting.AQUA))
                .append(".")
                .withStyle(ChatFormatting.WHITE), true);

        if (!isDouble && settings.doubleSend.getValue()) {
            tickCounter = 0;
            queue.addFirst(name + ";" + amount + ";double");
        }
    }

    public static boolean isRunning() {
        return running;
    }
}