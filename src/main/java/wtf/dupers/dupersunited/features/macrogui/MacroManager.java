package wtf.dupers.dupersunited.features.macrogui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.features.ModSettings;
import wtf.dupers.dupersunited.features.glitchutils.*;
import wtf.dupers.dupersunited.modules.glitcha.MacroGuiSettingsModule;

import java.util.ArrayList;
import java.util.List;

public class MacroManager {

    private static List<GuiMacro.MacroAction> activeSequence = new ArrayList<>();
    private static int currentIndex = -1;
    private static String runningMacroName = "";

    private static long lastActionTime = 0;
    private static int lastSyncId = -1;
    private static boolean waitingForFirstClick = false;
    private static boolean waitingForClickSpam = false;
    private static int lastLoggedIndex = -1;

    private static MacroGuiSettingsModule getSettings() {
        return MainClient.MODULE_MANAGER.getModule(MacroGuiSettingsModule.class);
    }

    private static long firstClickDelay() {
        return getSettings().firstClickDelaySetting.getValue().longValue();
    }

    private static long clickInterval() {
        return getSettings().clickIntervalSetting.getValue().longValue();
    }

    private static boolean isLooping() {
        return getSettings().isLoopingSetting.getValue();
    }

    public static void startMacro(String name, List<GuiMacro.MacroAction> steps) {
        runningMacroName = name;
        activeSequence = new ArrayList<>(steps);
        currentIndex = 0;
        lastLoggedIndex = -1;
        waitingForFirstClick = true;
        waitingForClickSpam = false;
        lastActionTime = System.currentTimeMillis();
    }

    public static void stop() {
        currentIndex = -1;
        lastLoggedIndex = -1;
        activeSequence.clear();
        runningMacroName = "";
        waitingForClickSpam = false;
    }

    public static boolean isRunning() {
        return currentIndex != -1;
    }

    public static String getRunningName() {
        return runningMacroName;
    }

    public static void tick() {
        if (currentIndex == -1) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gameMode == null) return;

        if (waitingForClickSpam) {
            if (ClickSlotManager.isRunning()) return;
            waitingForClickSpam = false;
            lastActionTime = System.currentTimeMillis();
            return;
        }

        if (currentIndex >= activeSequence.size()) {
            long currentTime = System.currentTimeMillis();
            long delay = waitingForFirstClick ? firstClickDelay() : clickInterval();
            if (currentTime - lastActionTime < delay) return;

            if (isLooping()) {
                currentIndex = 0;
                lastLoggedIndex = -1;
                lastActionTime = currentTime;
            } else {
                MainCommand.sendMessage(Component.literal("Macro ")
                    .append(Component.literal(runningMacroName).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" has "))
                    .append(Component.literal("completed").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("!")), true);
                stop();
            }
            return;
        }

        GuiMacro.MacroAction action = activeSequence.get(currentIndex);

        long currentTime = System.currentTimeMillis();
        long delay = action.sinceLastMs() > 0
            ? action.sinceLastMs()
            : (waitingForFirstClick ? firstClickDelay() : clickInterval());
        if (currentTime - lastActionTime < delay) return;

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
                if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
                    return;
                }

                    int slotId = action.slotId();
                    int maxSlots = screen.getMenu().slots.size();

                if (slotId == -19) {
                    if (screen.getMenu().containerId > lastSyncId) {
                        MainCommand.sendMessage(Component.literal("Found new GUI, continuing macro."), true);
                        currentIndex++;
                    }
                    return;
                }
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

                client.gameMode.handleContainerInput(
                    screen.getMenu().containerId,
                    action.slotId(),
                    action.button(),
                    action.type(),
                    client.player
                );

                advance(currentTime);
            }
            case PAUSE_PACKETS -> {
                String expectedTitle = action.guiTitle();

                if (expectedTitle != null && !expectedTitle.isEmpty()) {
                    if (client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
                        String currentTitle = screen.getTitle().getString();
                        if (currentTitle.toLowerCase().contains(expectedTitle.toLowerCase())) {
                            GuiPacketDelayManager.pause(expectedTitle);
                            advance(currentTime);
                        }
                    } else {
                        GuiPacketDelayManager.pause(expectedTitle);
                        advance(currentTime);
                    }
                } else {
                    GuiPacketDelayManager.pause();
                    advance(currentTime);
                }
            }
            case RESUME_PACKETS -> {
                GuiPacketDelayManager.resume();
                advance(currentTime);
            }
            case CLICK_SLOT_SPAM -> {
                ClickSlotManager.start(action.slotId(), action.count(), action.delay());
                waitingForClickSpam = true;
                currentIndex++;
            }
            case SAVE_GUI -> {
                SaveGuiManager.saveGui();
                advance(currentTime);
            }
            case SAVE_AND_CLOSE_GUI -> {
                SaveGuiManager.saveAndCloseGui();
                advance(currentTime);
            }
            case RESTORE_GUI -> {
                SaveGuiManager.restoreGui();
                advance(currentTime);
            }
            case SEND_CHAT -> {
                if (client.getConnection() != null && action.text() != null) {
                    client.getConnection().sendChat(action.text());
                }
                advance(currentTime);
            }
            case SEND_COMMAND -> {
                if (client.getConnection() != null && action.text() != null) {
                    client.getConnection().sendCommand(action.text());
                }
                advance(currentTime);
            }
        }
    }

    private static void advance(long currentTime) {
        currentIndex++;
        waitingForFirstClick = false;
        lastActionTime = currentTime;
    }
}