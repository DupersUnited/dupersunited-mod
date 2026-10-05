package wtf.dupers.dupersunited.features.macrogui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.inventory.ContainerInput;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.keybinds.KeybindManager;
import wtf.dupers.dupersunited.utils.packets.PacketModifier;
import wtf.dupers.dupersunited.utils.packets.PacketPipeline;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class GuiMacro {
    private static GuiMacro instance;
    private final Logger LOGGER = LoggerFactory.getLogger("GUIMacros");

    public record Macro(String name, int key, List<MacroAction> actions) {}

    public enum ActionKind {
        CLICK,
        PAUSE_PACKETS,
        RESUME_PACKETS,
        CLICK_SLOT_SPAM,
        SAVE_GUI,
        SAVE_AND_CLOSE_GUI,
        RESTORE_GUI,
        SEND_CHAT,
        SEND_COMMAND
    }

    public record MacroAction(ActionKind kind, int slotId, ContainerInput type, int button, int count, int delay, String guiTitle, long sinceLastMs, String text) {
        public static MacroAction click(int slotId, ContainerInput type, int button) {
            return new MacroAction(ActionKind.CLICK, slotId, type, button, 0, 0, null, 0, null);
        }

        public static MacroAction pausePackets() {
            Minecraft client = Minecraft.getInstance();
            String title = (client.gui.screen() != null) ? client.gui.screen().getTitle().getString() : null;
            return new MacroAction(ActionKind.PAUSE_PACKETS, -1, null, 0, 0, 0, title, 0, null);
        }

        public static MacroAction pausePackets(String guiTitle) {
            return new MacroAction(ActionKind.PAUSE_PACKETS, -1, null, 0, 0, 0, guiTitle, 0, null);
        }

        public static MacroAction resumePackets(String guiTitle) {
            return new MacroAction(ActionKind.RESUME_PACKETS, -1, null, 0, 0, 0, guiTitle, 0, null);
        }

        public static MacroAction clickSlotSpam(int slotId, int count, int delay) {
            return new MacroAction(ActionKind.CLICK_SLOT_SPAM, slotId, null, 0, count, delay, null, 0, null);
        }

        public static MacroAction saveGui() {
            return new MacroAction(ActionKind.SAVE_GUI, -1, null, 0, 0, 0, null, 0, null);
        }

        public static MacroAction saveAndCloseGui() {
            return new MacroAction(ActionKind.SAVE_AND_CLOSE_GUI, -1, null, 0, 0, 0, null, 0, null);
        }

        public static MacroAction restoreGui() {
            return new MacroAction(ActionKind.RESTORE_GUI, -1, null, 0, 0, 0, null, 0, null);
        }

        public static MacroAction sendChat(String text) {
            return new MacroAction(ActionKind.SEND_CHAT, -1, null, 0, 0, 0, null, 0, text);
        }

        public static MacroAction sendCommand(String text) {
            return new MacroAction(ActionKind.SEND_COMMAND, -1, null, 0, 0, 0, null, 0, text);
        }

        public MacroAction withSinceLastMs(long ms) {
            return new MacroAction(kind, slotId, type, button, count, delay, guiTitle, ms, text);
        }
    }

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path MACRO_DIR = SharedVariables.DIRECTORY.resolve("macros");

    public boolean isRecording = false;
    private boolean waitingForTargetGui = false;

    private String currentMacroName = "";
    private String targetGuiName = "Any";

    private final List<MacroAction> currentClicks = new ArrayList<>();
    private final Map<String, Macro> registeredMacros = new HashMap<>();

    private long lastActionTimestamp = 0;

    private GuiMacro() {
        PacketPipeline.getInstance().registerModifier(windowClickListener);
    }

    public static GuiMacro getInstance() {
        if (instance == null) instance = new GuiMacro();
        return instance;
    }

    public void prepareRecording(@Nullable String name, String guiName) {
        currentMacroName = (name == null || name.isBlank()) ? generateDefaultName() : name;
        targetGuiName = (guiName == null) ? "Any" : guiName;
        currentClicks.clear();
        lastActionTimestamp = 0;

        isRecording = false;
        waitingForTargetGui = true;

        LOGGER.info("Recording will start when GUI opens: {}", targetGuiName);

        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null) {
            checkScreen(client.gui.screen());
        }
    }

    public String getCurrentMacroName() {
        return currentMacroName;
    }

    private String generateDefaultName() {
        int i = 1;
        String candidate;
        do {
            candidate = "guimacro" + i;
            i++;
        } while (registeredMacros.containsKey(candidate));
        return candidate;
    }

    public String getUniqueMacroName(String baseName) {
        if (baseName == null || baseName.isBlank()) {
            return generateDefaultName();
        }

        String candidate = baseName.trim();
        if (!registeredMacros.containsKey(candidate)) {
            return candidate;
        }

        int counter = 1;
        while (registeredMacros.containsKey(candidate + counter)) {
            counter++;
        }
        return candidate + counter;
    }

    public Map<String, Macro> getRegisteredMacros() {
        return registeredMacros;
    }

    public Collection<String> getMacroNames() {
        return registeredMacros.keySet();
    }

    public void loadMacros() {
        registeredMacros.clear();
        deserializeMacros().forEach(this::registerMacro);
        syncMacroKeybinds();
    }

    public void syncMacroKeybinds() {
        List<String> toRemove = KeybindManager.getRegisteredKeybinds().keySet().stream()
            .filter(key -> key.startsWith("GUI Macro: "))
            .toList();
        toRemove.forEach(KeybindManager::unregisterKeybind);

        for (Macro macro : registeredMacros.values()) {
            String keybindName = "GUI Macro: " + macro.name();

            Keybind keybind = new Keybind(
                keybindName,
                macro.key()
            ) {
                @Override
                public void onPress() {
                    toggleMacro(macro.name());
                }

                @Override
                public void setKeyCode(int keyCode) {
                    super.setKeyCode(keyCode);
                    setMacroKey(macro.name(), keyCode);
                }
            };

            KeybindManager.registerKeybind(keybind);
        }
    }

    public void checkScreen(Screen screen) {
        if (!waitingForTargetGui || screen == null) return;

        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return;
        }

        String title = screen.getTitle().getString();

        if (targetGuiName.equalsIgnoreCase("Any") ||
            title.toLowerCase().contains(targetGuiName.toLowerCase())) {

            isRecording = true;
            waitingForTargetGui = false;

            LOGGER.info("Started recording macro: {}", currentMacroName);

            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.gui.screen() != null) {
                    client.gui.setScreen(client.gui.screen());
                }
            });
        }
    }

    public final PacketModifier windowClickListener = new PacketModifier() {
        @Override
        public Set<Class<? extends Packet<?>>> getPacketClasses() {
            return Set.of(ServerboundContainerClickPacket.class);
        }

        @Override
        public Packet<?> modifyPacket(Packet<?> basePacket) {
            ServerboundContainerClickPacket packet = (ServerboundContainerClickPacket) basePacket;
            recordClick(packet.slotNum(), packet.containerInput(), packet.buttonNum());
            return packet;
        }
    };

    public void recordClick(int slotId, ContainerInput type, int button) {
        if (!isRecording) return;
        if (slotId == -19 && currentClicks.isEmpty()) return;
        currentClicks.add(stampElapsed(MacroAction.click(slotId, type, button)));
    }

    public void recordAction(MacroAction action) {
        if (!isRecording) return;
        currentClicks.add(stampElapsed(action));
    }

    private MacroAction stampElapsed(MacroAction action) {
        long now = System.currentTimeMillis();
        long sinceLast = (lastActionTimestamp == 0) ? 0 : (now - lastActionTimestamp);
        lastActionTimestamp = now;
        return action.withSinceLastMs(sinceLast);
    }

    public void registerMacro(Macro macro) {
        if (registeredMacros.containsKey(macro.name)) return;
        registeredMacros.put(macro.name, macro);
    }

    public void registerCloseGui() {
        if (!isRecording) return;

        MainCommand.sendMessage(Component.literal("Recording closed GUI."), true);
        recordClick(-19, ContainerInput.PICKUP, -19);
    }

    public void finalizeRecording() {
        if (!isRecording) return;
        MainCommand.sendMessage(Component.literal("")
            .append(Component.literal("Ended").withStyle(ChatFormatting.RED))
            .append(Component.literal(" recording.")), true);
        isRecording = false;
        waitingForTargetGui = false;

        if (currentClicks.isEmpty()) {
            LOGGER.info("No clicks recorded.");
            MainCommand.sendMessage(Component.literal("No actions recorded, macro was not saved."), true);
            return;
        }

        int existingKey = registeredMacros.containsKey(currentMacroName)
            ? registeredMacros.get(currentMacroName).key()
            : GLFW.GLFW_KEY_UNKNOWN;

        Macro macro = new Macro(currentMacroName, existingKey, new ArrayList<>(currentClicks));
        registeredMacros.put(macro.name(), macro);
        saveMacroToDisk(macro);
        syncMacroKeybinds();
    }

    private void saveMacroToDisk(Macro macro) {
        if (!Files.isDirectory(MACRO_DIR)) {
            try {
                Files.createDirectories(MACRO_DIR);
            } catch (IOException e) {
                LOGGER.error("Error creating macro directory.", e);
                return;
            }
        }

        try (Writer writer = Files.newBufferedWriter(MACRO_DIR.resolve(macro.name() + ".json"))) {
            GSON.toJson(macro, writer);
            LOGGER.info("Saved macro {}", macro.name());
        } catch (IOException e) {
            LOGGER.error("Error saving macro", e);
        }
    }

    public boolean setMacroKey(String macroName, int keyCode) {
        Macro existing = registeredMacros.get(macroName);
        if (existing == null) return false;

        Macro updated = new Macro(macroName, keyCode, existing.actions());
        registeredMacros.put(macroName, updated);
        saveMacroToDisk(updated);
        return true;
    }

    public void toggleMacro(String macroName) {
        Macro macro = getMacro(macroName);
        if (macro == null) return;

        if (MacroManager.isRunning() && MacroManager.getRunningName().equals(macroName)) {
            MacroManager.stop();
            MainCommand.sendMessage(Component.literal("GUIMacro ")
                .append(Component.literal(macroName).withStyle(ChatFormatting.AQUA))
                .append(" stopped."), true);
        } else {
            runMacro(macro);
            MainCommand.sendMessage(Component.literal("GUIMacro ")
                .append(Component.literal(macroName).withStyle(ChatFormatting.AQUA))
                .append(" started."), true);
        }
    }

    public void runMacro(String name) {
        Macro macro = getMacro(name);
        if (macro == null) {
            LOGGER.warn("Macro not found.");
            return;
        }
        runMacro(macro);
    }

    public void runMacro(Macro macro) {
        List<MacroAction> actions = macro.actions;
        if (actions.isEmpty()) {
            LOGGER.warn("Empty macro.");
            return;
        }
        MacroManager.startMacro(macro.name, actions);
    }

    public @Nullable Macro getMacro(String name) {
        return registeredMacros.get(name);
    }

    // File ops
    private List<Macro> deserializeMacros() {
        if (!Files.isDirectory(MACRO_DIR)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(MACRO_DIR)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".json"))
                .map(GuiMacro.getInstance()::deserializeMacro)
                .filter(Objects::nonNull)
                .toList();
        } catch (IOException e) {
            LOGGER.error("Error reading macro directory", e);
            return List.of();
        }
    }

    private @Nullable Macro deserializeMacro(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            Macro macro = GSON.fromJson(reader, Macro.class);
            if (macro == null) return null;

            List<MacroAction> normalized = macro.actions().stream()
                .map(a -> a.kind() == null ? MacroAction.click(a.slotId(), a.type(), a.button()) : a)
                .toList();

            return new Macro(macro.name(), macro.key(), normalized);
        } catch (IOException e) {
            LOGGER.error("Error deserializing macro", e);
            return null;
        }
    }

    public boolean deleteMacro(String name) {
        registeredMacros.remove(name);
        syncMacroKeybinds();

        Path file = MACRO_DIR.resolve(name + ".json");
        try {
            if (Files.deleteIfExists(file)) {
                LOGGER.info("Deleted macro: {}", name);
                return true;
            } else {
                LOGGER.warn("Macro not found: {}", name);
                return false;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to delete macro", e);
            return false;
        }
    }
}