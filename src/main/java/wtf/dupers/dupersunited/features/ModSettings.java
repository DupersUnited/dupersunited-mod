package wtf.dupers.dupersunited.features;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;

// mod-wide settings, intentionally not a module
public final class ModSettings {
    public static final BooleanSetting sendToggleMsg = new BooleanSetting("Send Toggle Msg", false);
    public static final BooleanSetting showModCapes = new BooleanSetting("Show Mod Capes", true);
    public static final BooleanSetting showDebugMessages = new BooleanSetting("Show Debug Messages", false);
    public static final BooleanSetting showBroadcasts = new BooleanSetting("Show Broadcasts", true);
    public static final BooleanSetting showServerInvites = new BooleanSetting("Show Server Invites", true);

    private ModSettings() {}

    // there has to be a better way of doing these
    public static JsonObject writeJson() {
        JsonObject root = new JsonObject();
        root.add(sendToggleMsg.getName(), sendToggleMsg.writeJson());
        root.add(showModCapes.getName(), showModCapes.writeJson());
        root.add(showDebugMessages.getName(), showDebugMessages.writeJson());
        root.add(showBroadcasts.getName(), showBroadcasts.writeJson());
        root.add(showServerInvites.getName(), showServerInvites.writeJson());
        return root;
    }

    public static void readJson(JsonElement element) {
        if (!(element instanceof JsonObject root)) {
            return;
        }
        if (root.has(sendToggleMsg.getName())) {
            try {
                sendToggleMsg.readJson(root.get(sendToggleMsg.getName()));
            } catch (Exception ignored) {
            }
        }
        if (root.has(showModCapes.getName())) {
            try {
                showModCapes.readJson(root.get(showModCapes.getName()));
            } catch (Exception ignored) {
            }
        }
        if (root.has(showDebugMessages.getName())) {
            try {
                showDebugMessages.readJson(root.get(showDebugMessages.getName()));
            } catch (Exception ignored) {
            }
        }
        if (root.has(showBroadcasts.getName())) {
            try {
                showBroadcasts.readJson(root.get(showBroadcasts.getName()));
            } catch (Exception ignored) {
            }
        }
        if (root.has(showServerInvites.getName())) {
            try {
                showServerInvites.readJson(root.get(showServerInvites.getName()));
            } catch (Exception ignored) {
            }
        }
    }

    // picks up the value from the old Mod Settings module entry, if present
    public static void migrateFromModuleEntry(JsonObject moduleData) {
        if (moduleData.has("settings")) {
            readJson(moduleData.getAsJsonObject("settings"));
        }
    }
}