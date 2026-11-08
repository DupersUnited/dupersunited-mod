package wtf.dupers.dupersunited.features.macros;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.ModSettings;
import wtf.dupers.dupersunited.features.glitchutils.*;
import wtf.dupers.dupersunited.modules.glitcha.MacroSettingsModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static wtf.dupers.dupersunited.MainClient.mc;

public class MacroManager {

    private static List<Macros.MacroAction> activeSequence = new ArrayList<>();
    private static int currentIndex = -1;
    private static String runningMacroName = "";

    private static long lastActionTick = 0;
    private static long tickCount = 0;
    private static int lastSyncId = -1;
    private static boolean waitingForFirstClick = false;
    private static boolean paused = false;
    private static boolean stepOnce = false;
    private static int lastLoggedIndex = -1;

    private static MacroSettingsModule getSettings() {
        return MainClient.MODULE_MANAGER.getModule(MacroSettingsModule.class);
    }

    private static long firstClickDelay() {
        return getSettings().firstClickDelaySetting.getValue().longValue();
    }

    private static long clickInterval() {
        return getSettings().clickIntervalSetting.getValue().longValue();
    }

    private static long firstClickDelayTicks() {
        return msToTicks(firstClickDelay());
    }

    private static long clickIntervalTicks() {
        return msToTicks((long) clickInterval());
    }

    private static long msToTicks(long ms) {
        return Math.max(1, (ms + 49) / 50);
    }

    public static void startMacro(String name, List<Macros.MacroAction> steps) {
        runningMacroName = name;
        activeSequence = new ArrayList<>(steps);
        currentIndex = 0;
        lastLoggedIndex = -1;
        waitingForFirstClick = true;
        paused = false;
        stepOnce = false;
        lastActionTick = tickCount;
    }

    public static void stop() {
        currentIndex = -1;
        lastLoggedIndex = -1;
        activeSequence.clear();
        runningMacroName = "";
        paused = false;
        stepOnce = false;
    }

    public static void pause() {
        if (currentIndex != -1) paused = true;
    }

    public static void resume() {
        paused = false;
        stepOnce = false;
    }

    public static void step() {
        if (currentIndex != -1) {
            paused = true;
            stepOnce = true;
        }
    }

    public static boolean isPaused() {
        return paused && currentIndex != -1;
    }

    public static boolean isRunning() {
        return currentIndex != -1;
    }

    public static String getRunningName() {
        return runningMacroName;
    }

    public static void tick() {
        tickCount++;
        if (currentIndex == -1) return;

        if (mc.player == null || mc.gameMode == null) return;

        if (paused && !stepOnce) return;
        stepOnce = false;

        if (currentIndex >= activeSequence.size()) {
            long delay = waitingForFirstClick ? firstClickDelayTicks() : clickIntervalTicks();
            if (tickCount - lastActionTick < delay) return;

            {
                MainCommand.sendMessage(Component.literal("Macro ")
                    .append(Component.literal(runningMacroName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" has "))
                    .append(Component.literal("completed").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("!")), true);
                stop();
            }
            return;
        }

        Macros.MacroAction action = activeSequence.get(currentIndex);

        long delay = action.kind() == Macros.ActionKind.WAIT_TICKS
            ? Math.max(msToTicks(action.sinceLastMs()), action.count())
            : action.kind() == Macros.ActionKind.WAIT_MS
            ? Math.max(msToTicks(action.sinceLastMs()), msToTicks(action.count()))
            : (action.sinceLastMs() > 0
                ? msToTicks(action.sinceLastMs())
                : (waitingForFirstClick ? firstClickDelayTicks() : clickIntervalTicks()));
        if (tickCount - lastActionTick < delay) return;

        //DEBUG TOOLS
        if (lastLoggedIndex != currentIndex) {
            lastLoggedIndex = currentIndex;

            if (ModSettings.showDebugMessages.getValue()) {
                int stepNumber = currentIndex + 1;
                int total = activeSequence.size();

                MainCommand.sendMessage(
                    Component.literal("[Debug] ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(runningMacroName + " ").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal("[" + stepNumber + "/" + total + "] ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(action.kind().name()).withStyle(ChatFormatting.GREEN))
                        .append(action.slotId() != -1 ? Component.literal(" (Slot: " + action.slotId() + ")").withStyle(ChatFormatting.GRAY) : Component.empty())
                        .append(action.text() != null && !action.text().isEmpty() ? Component.literal(" \"" + action.text() + "\"").withStyle(ChatFormatting.GRAY) : Component.empty()),
                    true
                );
            }
        }

        switch (action.kind()) {
            case CLICK -> {
                if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                    return;
                }

                    int slotId = action.slotId();
                    int maxSlots = screen.getMenu().slots.size();

                lastSyncId = screen.getMenu().containerId;

                if ((slotId < 0 && slotId != -999) || slotId >= maxSlots) {
                    if (ModSettings.showDebugMessages.getValue()) {
                        MainCommand.sendMessage(Component.literal("[DEBUG ERR] ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal("Invalid Slot ID " + slotId + " for menu size " + maxSlots + ". Skipping.")), true);
                    }
                    currentIndex++;
                    return;
                }

                mc.gameMode.handleContainerInput(
                    screen.getMenu().containerId,
                    action.slotId(),
                    action.button(),
                    action.type(),
                    mc.player
                );

                advance();
            }
            case PAUSE_PACKETS -> {
                String expectedTitle = action.guiTitle();

                if (expectedTitle != null && !expectedTitle.isEmpty()) {
                    if (mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
                        String currentTitle = screen.getTitle().getString();
                        if (currentTitle.toLowerCase().contains(expectedTitle.toLowerCase())) {
                            GuiPacketDelayManager.pause(expectedTitle);
                            advance();
                        }
                    } else {
                        GuiPacketDelayManager.pause(expectedTitle);
                        advance();
                    }
                } else {
                    GuiPacketDelayManager.pause();
                    advance();
                }
            }
            case RESUME_PACKETS -> {
                GuiPacketDelayManager.resume();
                advance();
            }
            case SAVE_GUI -> {
                SaveGuiManager.saveGui();
                advance();
            }
            case RESTORE_GUI -> {
                SaveGuiManager.restoreGui();
                advance();
            }
            case SEND_CHAT -> {
                if (mc.getConnection() != null && action.text() != null) {
                    mc.getConnection().sendChat(action.text());
                }
                advance();
            }
            case SEND_COMMAND -> {
                if (mc.getConnection() != null && action.text() != null) {
                    mc.getConnection().sendCommand(action.text());
                }
                advance();
            }
            case WAIT_TICKS, WAIT_MS -> advance();
            case LOOK -> {
                mc.player.setYRot(action.yaw());
                mc.player.setXRot(action.pitch());
                advance();
            }
            case USE_ITEM -> {
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                advance();
            }
            case ATTACK -> {
                if (mc.crosshairPickEntity instanceof Entity target) {
                    mc.gameMode.attack(mc.player, target);
                } else {
                    mc.options.keyAttack.setDown(true);
                    mc.options.keyAttack.setDown(false);
                }
                advance();
            }
            case DISCONNECT -> {
                if (mc.getConnection() != null) {
                    String reason = action.text() == null || action.text().isBlank() ? "Macro disconnect" : action.text();
                    mc.getConnection().getConnection().disconnect(Component.literal(reason));
                }
                advance();
            }
            case CONNECT -> {
                if (action.text() != null && !action.text().isBlank()) {
                    String address = action.text().trim();
                    mc.execute(() -> {
                        try {
                            ServerData info = new ServerData("Macro", address, ServerData.Type.OTHER);
                            ServerAddress parsed = ServerAddress.parseString(address);
                            ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc, parsed, info, false, null);
                        } catch (Exception ignored) {}
                    });
                }
                advance();
            }
            case CLOSE_GUI -> {
                mc.gui.setScreen(null);
                advance();
            }
            case WAIT_FOR_GUI -> {
                if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
                if (screen.getMenu().containerId <= lastSyncId) return;
                String want = action.text();
                if (want != null && !want.isBlank() && !screen.getTitle().getString().toLowerCase(Locale.ROOT).contains(want.toLowerCase(Locale.ROOT))) return;
                MainCommand.sendMessage(Component.literal("Found new GUI, continuing macro."), true);
                currentIndex++;
            }
        }
    }

    private static void advance() {
        currentIndex++;
        waitingForFirstClick = false;
        lastActionTick = tickCount;
    }
}