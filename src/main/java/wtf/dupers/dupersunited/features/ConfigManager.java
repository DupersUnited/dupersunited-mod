package wtf.dupers.dupersunited.features;

import com.google.gson.*;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.screens.ClickGui;
import wtf.dupers.dupersunited.features.screens.hud.HudEditorScreen;
import wtf.dupers.dupersunited.features.screens.hud.HudElement;
import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.keybinds.KeybindManager;
import wtf.dupers.dupersunited.api.module.Module;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.joml.Vector2i;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {

    private static final Path CONFIG_FILE = SharedVariables.DIRECTORY.resolve("config.json");

    private static final List<HudElement> HUD_ELEMENTS = List.of(
        HudEditorScreen.WATERMARK,
        //HudEditorScreen.MACRO,
        HudEditorScreen.SAVED_GUI,
        HudEditorScreen.TPS,
        HudEditorScreen.HUD_LIST
    );

    public static boolean autoReconnectEnabled = false;
    public static boolean rpBypassEnabled = false;
    public static boolean brandSpoofEnabled = false;
    public static boolean serverAlertsEnabled = true;
    public static String addonToken = "";
    public static String addonApiBaseUrl = "https://dupersunited-server.dupers.wtf";
    public static String addonWsBaseUrl = "wss://dupersunited-server.dupers.wtf/ws/private";

    public static boolean firstLaunch = true;

    private static final JsonObject unregisteredData = new JsonObject();

    public static void save() {
        AsyncConfigs.save(serialize(), CONFIG_FILE, "config");
    }

    public static void saveBlocking() {
        AsyncConfigs.saveSync(serialize(), CONFIG_FILE, "config");
    }

    private static JsonObject serialize() {
        JsonObject root = new JsonObject();

        root.addProperty("firstLaunch", firstLaunch);
        root.addProperty("autoReconnectEnabled", autoReconnectEnabled);
        root.addProperty("rpBypassEnabled", rpBypassEnabled);
        root.addProperty("brandSpoofEnabled", brandSpoofEnabled);
        root.addProperty("serverAlertsEnabled", serverAlertsEnabled);
        root.addProperty("addonToken", addonToken);
        root.addProperty("addonApiBaseUrl", addonApiBaseUrl);
        root.addProperty("addonWsBaseUrl", addonWsBaseUrl);

        JsonObject hudObj = new JsonObject();
        for (HudElement el : HUD_ELEMENTS) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", el.x);
            pos.addProperty("y", el.y);
            pos.addProperty("scale", el.scale);
            hudObj.add(el.id, pos);
        }
        root.add("hud", hudObj);

        JsonObject moduleObj = new JsonObject();
        for (Module module : MainClient.MODULE_MANAGER.modules()) {
            JsonElement moduleData = module.writeJson();
            moduleObj.add(module.getIdentifier(), moduleData);
        }
        root.add("modules", moduleObj);

        JsonObject keybindsObj = new JsonObject();
        for (Keybind kb : KeybindManager.getRegisteredKeybinds().values()) {
            keybindsObj.addProperty(kb.getName(), kb.getKeyCode());
        }
        root.add("keybinds", keybindsObj);

        JsonObject clickguiCategories = new JsonObject();
        ClickGui.customCategories.forEach(clickguiCategories::addProperty);
        root.add("clickguiCategories", clickguiCategories);

        JsonObject clickguiCategoryPositions = new JsonObject();
        ClickGui.categoryPositions.forEach((category, pos) -> {
            JsonObject position = new JsonObject();
            position.addProperty("x", pos.x());
            position.addProperty("y", pos.y());
            clickguiCategoryPositions.add(category, position);
        });
        root.add("clickguiCategoryPositions", clickguiCategoryPositions);

        JsonObject clickguiCategoryExpandedModules = new JsonObject();
        ClickGui.categoryExpandedModules.forEach((category, modules) -> {
            JsonArray expandedModules = new JsonArray();
            modules.forEach(expandedModules::add);
            clickguiCategoryExpandedModules.add(category, expandedModules);
        });
        root.add("clickguiCategoryExpandedModules", clickguiCategoryExpandedModules);

        JsonObject clickguiCategoryCollapsed = new JsonObject();
        ClickGui.categoryCollapsed.forEach(clickguiCategoryCollapsed::addProperty);
        root.add("clickguiCategoryCollapsed", clickguiCategoryCollapsed);

        if (ClickGui.hudPanelPosition != null) {
            JsonObject clickguiHudPanelPosition = new JsonObject();
            clickguiHudPanelPosition.addProperty("x", ClickGui.hudPanelPosition.x());
            clickguiHudPanelPosition.addProperty("y", ClickGui.hudPanelPosition.y());
            root.add("clickguiHudPanelPosition", clickguiHudPanelPosition);
        }

        if (!unregisteredData.isEmpty()) {
            unregisteredData.asMap().forEach(root::add);
            unregisteredData.asMap().clear();
        }

        return root;
    }

    public static CompletableFuture<Void> load() {
        return AsyncConfigs.load(JsonObject.class, CONFIG_FILE, "config").thenAccept(root -> {
            if (root.has("firstLaunch")) {
                firstLaunch = root.get("firstLaunch").getAsBoolean();
            }
            if (root.has("autoReconnectEnabled")) {
                autoReconnectEnabled = root.get("autoReconnectEnabled").getAsBoolean();
            }
            if (root.has("rpBypassEnabled")) {
                rpBypassEnabled = root.get("rpBypassEnabled").getAsBoolean();
            }
            if (root.has("brandSpoofEnabled")) {
                brandSpoofEnabled = root.get("brandSpoofEnabled").getAsBoolean();
            }
            if (root.has("serverAlertsEnabled ")) {
                serverAlertsEnabled = root.get("serverAlertsEnabled ").getAsBoolean();
            }
            if (root.has("addonToken")) {
                addonToken = root.get("addonToken").getAsString().trim();
            }
            if (root.has("addonApiBaseUrl")) {
                addonApiBaseUrl = root.get("addonApiBaseUrl").getAsString().trim();
            }
            if (root.has("addonWsBaseUrl")) {
                addonWsBaseUrl = root.get("addonWsBaseUrl").getAsString().trim();
            }

            if (root.has("hud")) {
                JsonObject hudObj = root.getAsJsonObject("hud");
                for (HudElement el : HUD_ELEMENTS) {
                    if (!hudObj.has(el.id)) continue;
                    JsonObject pos = hudObj.remove(el.id).getAsJsonObject();
                    if (pos.has("x")) el.x = pos.get("x").getAsInt();
                    if (pos.has("y")) el.y = pos.get("y").getAsInt();
                    if (pos.has("scale")) el.scale = pos.get("scale").getAsFloat();
                }

                if (!hudObj.isEmpty()) {
                    unregisteredData.add("hud", hudObj.deepCopy());
                }
            }

            boolean backwardsCompat = !root.has("modules");
            JsonObject modulesObj = backwardsCompat ? root : root.getAsJsonObject("modules");

            for (Module module : MainClient.MODULE_MANAGER.modules()) {
                boolean useIdentifier = modulesObj.has(module.getIdentifier());
                if (!useIdentifier && !modulesObj.has(module.getName())) continue;

                JsonElement moduleData = modulesObj.remove(useIdentifier ? module.getIdentifier() : module.getName());
                module.readJson(moduleData);
            }

            if (!backwardsCompat) {
                unregisteredData.add("modules", modulesObj.deepCopy());
            }

            if (root.has("keybinds")) {
                JsonObject keybindsObj = root.getAsJsonObject("keybinds");
                for (Keybind kb : KeybindManager.getRegisteredKeybinds().values()) {
                    if (keybindsObj.has(kb.getName())) {
                        kb.setKeyCode(keybindsObj.remove(kb.getName()).getAsInt());
                    }
                }

                if (!keybindsObj.isEmpty()) {
                    unregisteredData.add("keybinds", keybindsObj.deepCopy());
                }
            }

            if (root.has("clickguiCategories")) {
                root.getAsJsonObject("clickguiCategories").asMap().forEach((module, category) -> {
                    ClickGui.customCategories.put(module, category.getAsString());
                });
            }

            if (root.has("clickguiCategoryPositions")) {
                root.getAsJsonObject("clickguiCategoryPositions").asMap().forEach((category, position) -> {
                    JsonObject positionObject = position.getAsJsonObject();
                    ClickGui.categoryPositions.computeIfAbsent(category, k -> new Vector2i())
                        .set(positionObject.get("x").getAsInt(), positionObject.get("y").getAsInt());
                });
            }

            if (root.has("clickguiCategoryExpandedModules")) {
                root.getAsJsonObject("clickguiCategoryExpandedModules").asMap().forEach((category, expandedModules) -> {
                    Set<String> set = ClickGui.categoryExpandedModules.computeIfAbsent(category, k -> new ObjectOpenHashSet<>());
                    expandedModules.getAsJsonArray().asList().forEach(el -> set.add(el.getAsString()));
                });
            }

            if (root.has("clickguiCategoryCollapsed")) {
                root.getAsJsonObject("clickguiCategoryCollapsed").asMap().forEach((category, collapsed) -> {
                    ClickGui.categoryCollapsed.put(category, collapsed.getAsBoolean());
                });
            }

            if (root.has("clickguiHudPanelPosition")) {
                JsonObject position = root.getAsJsonObject("clickguiHudPanelPosition");
                ClickGui.hudPanelPosition = new Vector2i(position.get("x").getAsInt(), position.get("y").getAsInt());
            }
        });
    }
}