package wtf.dupers.dupersunited.features.macros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
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
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;
import static wtf.dupers.dupersunited.MainClient.mc;

public class Macros {
    private static Macros instance;
    private final Logger LOGGER = LoggerFactory.getLogger("Macros");

    public static final int MACRO_VERSION = 1;

    public record Macro(String name, int key, List<MacroAction> actions, String description, int version) {}

    public enum ActionKind {
        CLICK,
        PAUSE_PACKETS,
        RESUME_PACKETS,
        SAVE_GUI,
        RESTORE_GUI,
        SEND_CHAT,
        SEND_COMMAND,
        WAIT_TICKS,
        WAIT_MS,
        LOOK,
        USE_ITEM,
        ATTACK,
        DISCONNECT,
        CONNECT,
        CLOSE_GUI,
        WAIT_FOR_GUI
    }

    public record MacroAction(ActionKind kind, int slotId, ContainerInput type, int button, int count, int delay, String guiTitle, long sinceLastMs, String text, float yaw, float pitch) {
        public static MacroAction click(int slotId, ContainerInput type, int button) {
            return new MacroAction(ActionKind.CLICK, slotId, type, button, 0, 0, null, 0, null, 0, 0);
        }

        public static MacroAction pausePackets() {
            String title = (mc.gui.screen() != null) ? mc.gui.screen().getTitle().getString() : null;
            return new MacroAction(ActionKind.PAUSE_PACKETS, -1, null, 0, 0, 0, title, 0, null, 0, 0);
        }

        public static MacroAction pausePackets(String guiTitle) {
            return new MacroAction(ActionKind.PAUSE_PACKETS, -1, null, 0, 0, 0, guiTitle, 0, null, 0, 0);
        }

        public static MacroAction resumePackets(String guiTitle) {
            return new MacroAction(ActionKind.RESUME_PACKETS, -1, null, 0, 0, 0, guiTitle, 0, null, 0, 0);
        }

        public static MacroAction saveGui() {
            return new MacroAction(ActionKind.SAVE_GUI, -1, null, 0, 0, 0, null, 0, null, 0, 0);
        }

        public static MacroAction restoreGui() {
            return new MacroAction(ActionKind.RESTORE_GUI, -1, null, 0, 0, 0, null, 0, null, 0, 0);
        }

        public static MacroAction sendChat(String text) {
            return new MacroAction(ActionKind.SEND_CHAT, -1, null, 0, 0, 0, null, 0, text, 0, 0);
        }

        public static MacroAction sendCommand(String text) {
            return new MacroAction(ActionKind.SEND_COMMAND, -1, null, 0, 0, 0, null, 0, text, 0, 0);
        }

        public static MacroAction waitTicks(int ticks) {
            return new MacroAction(ActionKind.WAIT_TICKS, -1, null, 0, Math.max(0, ticks), 0, null, 0, null, 0, 0);
        }

        public static MacroAction waitMs(int ms) {
            return new MacroAction(ActionKind.WAIT_MS, -1, null, 0, Math.max(0, ms), 0, null, 0, null, 0, 0);
        }

        public static MacroAction look(float yaw, float pitch) {
            return new MacroAction(ActionKind.LOOK, -1, null, 0, 0, 0, null, 0, null, yaw, pitch);
        }

        public static MacroAction useItem() {
            return new MacroAction(ActionKind.USE_ITEM, -1, null, 0, 0, 0, null, 0, null, 0, 0);
        }

        public static MacroAction attack() {
            return new MacroAction(ActionKind.ATTACK, -1, null, 0, 0, 0, null, 0, null, 0, 0);
        }

        public static MacroAction disconnect(String reason) {
            return new MacroAction(ActionKind.DISCONNECT, -1, null, 0, 0, 0, null, 0, reason, 0, 0);
        }

        public static MacroAction connect(String address) {
            return new MacroAction(ActionKind.CONNECT, -1, null, 0, 0, 0, null, 0, address, 0, 0);
        }

        public static MacroAction closeGui() {
            return new MacroAction(ActionKind.CLOSE_GUI, -1, null, 0, 0, 0, null, 0, null, 0, 0);
        }

        public static MacroAction waitForGui(String title) {
            return new MacroAction(ActionKind.WAIT_FOR_GUI, -1, null, 0, 0, 0, null, 0, title, 0, 0);
        }

        public MacroAction withSinceLastMs(long ms) {
            return new MacroAction(kind, slotId, type, button, count, delay, guiTitle, ms, text, yaw, pitch);
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
    private final Set<String> incompatibleMacros = new HashSet<>();

    private long lastActionTimestamp = 0;

    private long recordTickCount = 0;
    private boolean prevAttack = false;
    private boolean prevUse = false;
    private float prevYaw = 0;
    private float prevPitch = 0;
    private boolean lookInit = false;
    private long lastLookTick = 0;
    private boolean prevScreenWasContainer = false;

    private Macros() {
        PacketPipeline.getInstance().registerModifier(windowClickListener);
    }

    public static Macros getInstance() {
        if (instance == null) instance = new Macros();
        return instance;
    }

    public void prepareRecording(@Nullable String name, String guiName) {
        currentMacroName = (name == null || name.isBlank()) ? generateDefaultName() : name;
        targetGuiName = (guiName == null) ? "Any" : guiName;
        currentClicks.clear();
        lastActionTimestamp = 0;
        resetRecordTracker();

        isRecording = false;
        waitingForTargetGui = true;

        LOGGER.info("Recording will start when GUI opens: {}", targetGuiName);

        if (mc.gui.screen() != null) {
            checkScreen(mc.gui.screen());
        }
    }

    public String getCurrentMacroName() {
        return currentMacroName;
    }

    private String generateDefaultName() {
        int i = 1;
        String candidate;
        do {
            candidate = "macro" + i;
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

    public Set<String> getIncompatibleMacros() {
        return Collections.unmodifiableSet(incompatibleMacros);
    }

    public void loadMacros() {
        registeredMacros.clear();
        incompatibleMacros.clear();
        deserializeMacros().forEach(this::registerMacro);
        if (!incompatibleMacros.isEmpty()) {
            LOGGER.warn("Skipped incompatible macros (version {} required): {}", MACRO_VERSION, String.join(", ", incompatibleMacros));
            MainCommand.sendMessage(Component.literal("Skipped incompatible macros: ")
                .append(Component.literal(String.join(", ", incompatibleMacros)).withStyle(ChatFormatting.RED)), true);
        }
        if (importChatMacros()) archiveChatMacros();
        normalizeMacroFiles();
        syncMacroKeybinds();
    }

    public void appendAction(String name, MacroAction action) {
        Macro existing = registeredMacros.get(name);
        List<MacroAction> actions = existing == null ? new ArrayList<>() : new ArrayList<>(existing.actions());
        actions.add(action);
        int key = existing == null ? GLFW.GLFW_KEY_UNKNOWN : existing.key();
        String description = existing == null ? null : existing.description();
        Macro updated = new Macro(name, key, actions, description, MACRO_VERSION);
        registeredMacros.put(name, updated);
        saveMacroToDisk(updated);
        syncMacroKeybinds();
    }

    private boolean importChatMacros() {
        Path chatFile = SharedVariables.DIRECTORY.resolve("chatmacros.json");
        if (!Files.isRegularFile(chatFile)) return false;
        JsonElement root;
        try (Reader reader = Files.newBufferedReader(chatFile)) {
            root = GSON.fromJson(reader, JsonElement.class);
        } catch (IOException e) {
            LOGGER.error("Error importing chat macros", e);
            return false;
        }
        if (root == null || !root.isJsonArray()) return false;
        for (JsonElement el : root.getAsJsonArray()) {
            if (!el.isJsonObject()) continue;
            JsonObject chat = el.getAsJsonObject();
            if (!chat.has("name") || chat.get("name").isJsonNull()) continue;
            String name = chat.get("name").getAsString();
            int key = chat.has("keyCode") && !chat.get("keyCode").isJsonNull() ? chat.get("keyCode").getAsInt() : GLFW.GLFW_KEY_UNKNOWN;
            List<MacroAction> actions = new ArrayList<>();
            if (chat.has("messages") && chat.get("messages").isJsonArray()) {
                for (JsonElement msgEl : chat.getAsJsonArray("messages")) {
                    if (!msgEl.isJsonObject()) continue;
                    JsonObject msg = msgEl.getAsJsonObject();
                    if (!msg.has("text") || msg.get("text").isJsonNull()) continue;
                    String text = msg.get("text").getAsString();
                    int delay = msg.has("delayMs") && !msg.get("delayMs").isJsonNull() ? msg.get("delayMs").getAsInt() : 0;
                    if (text.isEmpty()) {
                        actions.add(MacroAction.waitMs(delay));
                    } else if (text.startsWith("/")) {
                        actions.add(MacroAction.sendCommand(text.substring(1)).withSinceLastMs(delay));
                    } else {
                        actions.add(MacroAction.sendChat(text).withSinceLastMs(delay));
                    }
                }
            }
            if (actions.isEmpty()) continue;
            String target = registeredMacros.containsKey(name) ? getUniqueMacroName(name) : name;
            Macro macro = new Macro(target, key, actions, null, MACRO_VERSION);
            registeredMacros.put(macro.name(), macro);
            saveMacroToDisk(macro);
        }
        return true;
    }

    private void archiveChatMacros() {
        Path chatFile = SharedVariables.DIRECTORY.resolve("chatmacros.json");
        try {
            Files.move(chatFile, chatFile.resolveSibling("chatmacros.json.migrated"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Error archiving chat macros", e);
        }
    }

    private void normalizeMacroFiles() {
        if (!Files.isDirectory(MACRO_DIR)) return;
        try (Stream<Path> files = Files.list(MACRO_DIR)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                String content = Files.readString(file);
                if (content.contains("\"yaw\"") && content.contains("\"pitch\"") && content.contains("\"version\"")) continue;
                String name = file.getFileName().toString().replaceFirst("\\.json$", "");
                Macro macro = registeredMacros.get(name);
                if (macro != null) saveMacroToDisk(macro);
            }
        } catch (IOException e) {
            LOGGER.error("Error normalizing macro files", e);
        }
    }

    public void syncMacroKeybinds() {
        List<String> toRemove = KeybindManager.getRegisteredKeybinds().keySet().stream()
            .filter(key -> key.startsWith("Macro: "))
            .toList();
        toRemove.forEach(KeybindManager::unregisterKeybind);

        for (Macro macro : registeredMacros.values()) {
            String keybindName = "Macro: " + macro.name();

            Keybind keybind = new Keybind(
                keybindName,
                macro.key()
            ) {
                @Override
                public void onPress() {
                    toggleMacro(macro);
                }

                @Override
                public void setKeyCode(int keyCode) {
                    super.setKeyCode(keyCode);
                    setMacroKey(macro, keyCode);
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

            mc.execute(() -> {
                if (mc.gui.screen() != null) {
                    mc.gui.setScreen(mc.gui.screen());
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
        if (action.kind() == ActionKind.WAIT_FOR_GUI && !currentClicks.isEmpty()
            && currentClicks.get(currentClicks.size() - 1).kind() == ActionKind.WAIT_FOR_GUI) return;
        currentClicks.add(stampElapsed(action));
    }

    private void resetRecordTracker() {
        recordTickCount = 0;
        prevAttack = prevUse = false;
        lookInit = false;
        lastLookTick = 0;
        prevScreenWasContainer = false;
    }

    public void recordTick() {
        if (!isRecording || waitingForTargetGui) return;
        if (mc.player == null || mc.options == null) return;
        recordTickCount++;

        boolean attack = mc.options.keyAttack.isDown();
        if (attack && !prevAttack) recordAction(MacroAction.attack());
        prevAttack = attack;

        boolean use = mc.options.keyUse.isDown();
        if (use && !prevUse) recordAction(MacroAction.useItem());
        prevUse = use;

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        if (!lookInit) {
            prevYaw = yaw;
            prevPitch = pitch;
            lookInit = true;
        } else if (recordTickCount - lastLookTick >= 5
            && (Math.abs(yaw - prevYaw) > 2.0f || Math.abs(pitch - prevPitch) > 2.0f)) {
            prevYaw = yaw;
            prevPitch = pitch;
            lastLookTick = recordTickCount;
            recordAction(MacroAction.look(yaw, pitch));
        }

        boolean container = mc.gui.screen() instanceof AbstractContainerScreen<?>;
        if (container && !prevScreenWasContainer) {
            recordAction(MacroAction.waitForGui(mc.gui.screen().getTitle().getString()));
        } else if (!container && prevScreenWasContainer) {
            recordAction(MacroAction.waitForGui(null));
        }
        prevScreenWasContainer = container;
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
        recordAction(MacroAction.waitForGui(null));
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
        String existingDescription = registeredMacros.containsKey(currentMacroName)
            ? registeredMacros.get(currentMacroName).description()
            : null;
        Macro macro = new Macro(currentMacroName, existingKey, new ArrayList<>(currentClicks), existingDescription, MACRO_VERSION);
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
        return setMacroKey(existing, keyCode);
    }

    public boolean setMacroKey(Macro existing, int keyCode) {
        Macro updated = new Macro(existing.name(), keyCode, existing.actions(), existing.description(), MACRO_VERSION);
        registeredMacros.put(existing.name(), updated);
        saveMacroToDisk(updated);
        return true;
    }

    public boolean setMacroDescription(String macroName, String description) {
        Macro existing = registeredMacros.get(macroName);
        if (existing == null) return false;

        Macro updated = new Macro(macroName, existing.key(), existing.actions(), description, MACRO_VERSION);
        registeredMacros.put(macroName, updated);
        saveMacroToDisk(updated);
        return true;
    }

    public List<String> validateMacro(String macroName) {
        List<String> problems = new ArrayList<>();
        Macro macro = registeredMacros.get(macroName);
        if (macro == null) {
            problems.add("Unknown macro: " + macroName);
            return problems;
        }
        if (macro.actions() == null || macro.actions().isEmpty()) {
            problems.add("Macro has no actions.");
            return problems;
        }
        int i = 0;
        for (MacroAction action : macro.actions()) {
            i++;
            if (action == null || action.kind() == null) {
                problems.add("Action " + i + " has no kind.");
                continue;
            }
            switch (action.kind()) {
                case SEND_CHAT, SEND_COMMAND, CONNECT -> {
                    if (action.text() == null || action.text().isBlank()) problems.add("Action " + i + " (" + action.kind().name().toLowerCase() + ") has no text.");
                }
                case WAIT_TICKS, WAIT_MS -> {
                    if (action.count() < 0 && action.sinceLastMs() < 0) problems.add("Action " + i + " (wait) has negative duration.");
                }
                default -> {}
            }
        }
        return problems;
    }

    public void createMacro(String name) {
        if (name == null || name.isBlank() || registeredMacros.containsKey(name)) return;
        Macro macro = new Macro(name, GLFW.GLFW_KEY_UNKNOWN, new ArrayList<>(), null, MACRO_VERSION);
        registeredMacros.put(macro.name(), macro);
        saveMacroToDisk(macro);
        syncMacroKeybinds();
    }

    public boolean renameMacro(String oldName, String newName) {
        Macro existing = registeredMacros.get(oldName);
        if (existing == null) return false;
        return renameMacro(existing, newName);
    }

    public boolean renameMacro(Macro existing, String newName) {
        if (newName == null || newName.isBlank() || registeredMacros.containsKey(newName)) return false;
        String oldName = existing.name();
        registeredMacros.remove(oldName);
        syncMacroKeybinds();
        try {
            Files.deleteIfExists(MACRO_DIR.resolve(oldName + ".json"));
        } catch (IOException e) {
            LOGGER.error("Failed to delete macro", e);
        }
        Macro renamed = new Macro(newName, existing.key(), existing.actions(), existing.description(), MACRO_VERSION);
        registeredMacros.put(renamed.name(), renamed);
        saveMacroToDisk(renamed);
        syncMacroKeybinds();
        return true;
    }

    public boolean setMacroActions(String macroName, List<MacroAction> actions) {
        Macro existing = registeredMacros.get(macroName);
        if (existing == null) return false;
        return setMacroActions(existing, actions);
    }

    public boolean setMacroActions(Macro existing, List<MacroAction> actions) {
        Macro updated = new Macro(existing.name(), existing.key(), new ArrayList<>(actions), existing.description(), MACRO_VERSION);
        registeredMacros.put(existing.name(), updated);
        saveMacroToDisk(updated);
        return true;
    }

    public void toggleMacro(String macroName) {
        Macro macro = getMacro(macroName);
        if (macro == null) return;
        toggleMacro(macro);
    }

    public void toggleMacro(Macro macro) {
        if (MacroManager.isRunning() && MacroManager.getRunningName().equals(macro.name())) {
            MacroManager.stop();
            MainCommand.sendMessage(Component.literal("Macro ")
                .append(Component.literal(macro.name()).withStyle(ChatFormatting.AQUA))
                .append(" stopped."), true);
        } else {
            runMacro(macro);
            MainCommand.sendMessage(Component.literal("Macro ")
                .append(Component.literal(macro.name()).withStyle(ChatFormatting.AQUA))
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
                .map(Macros.getInstance()::deserializeMacro)
                .filter(Objects::nonNull)
                .toList();
        } catch (IOException e) {
            LOGGER.error("Error reading macro directory", e);
            return List.of();
        }
    }

    private @Nullable Macro deserializeMacro(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("name")) return null;
            String name = root.get("name").getAsString();
            int fileVersion = root.has("version") && !root.get("version").isJsonNull() ? root.get("version").getAsInt() : 0;
            if (fileVersion > MACRO_VERSION) {
                incompatibleMacros.add(name);
                return null;
            }
            int key = root.has("key") ? root.get("key").getAsInt() : GLFW.GLFW_KEY_UNKNOWN;
            String description = root.has("description") && !root.get("description").isJsonNull() ? root.get("description").getAsString() : null;
            List<MacroAction> actions = new ArrayList<>();
            if (root.has("actions") && root.get("actions").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("actions")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    String kind = obj.has("kind") && !obj.get("kind").isJsonNull() ? obj.get("kind").getAsString() : null;
                    if ("CLICK_SLOT_SPAM".equals(kind)) {
                        int slot = obj.has("slotId") ? obj.get("slotId").getAsInt() : -1;
                        int count = obj.has("count") ? obj.get("count").getAsInt() : 0;
                        int delay = obj.has("delay") ? obj.get("delay").getAsInt() : 0;
                        for (int i = 0; i < Math.max(0, count); i++) {
                            actions.add(MacroAction.click(slot, ContainerInput.PICKUP, 0).withSinceLastMs(i == 0 ? 0 : Math.max(0, delay)));
                        }
                    } else if ("SAVE_AND_CLOSE_GUI".equals(kind)) {
                        actions.add(MacroAction.saveGui());
                        actions.add(MacroAction.closeGui());
                    } else if ("WAIT".equals(kind)) {
                        MacroAction action = GSON.fromJson(el, MacroAction.class);
                        actions.add(MacroAction.waitMs(Math.max(0, action.count())));
                    } else {
                        MacroAction action = GSON.fromJson(el, MacroAction.class);
                        if (action.kind() == null) {
                            action = MacroAction.click(action.slotId(), action.type() == null ? ContainerInput.PICKUP : action.type(), action.button());
                        } else if (action.kind() == ActionKind.CLICK && action.slotId() == -19) {
                            action = MacroAction.waitForGui(null);
                        }
                        actions.add(action);
                    }
                }
            }
            return new Macro(name, key, actions, description, MACRO_VERSION);
        } catch (IOException e) {
            LOGGER.error("Error deserializing macro", e);
            return null;
        }
    }

    public boolean deleteMacro(String name) {
        Macro existing = registeredMacros.get(name);
        if (existing == null) return false;
        return deleteMacro(existing);
    }

    public boolean deleteMacro(Macro macro) {
        String name = macro.name();
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