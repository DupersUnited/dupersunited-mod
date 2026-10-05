package wtf.dupers.dupersunited.features.proxies;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.AsyncConfigs;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ProxyConfigManager {
    private static final Path FILE = SharedVariables.DIRECTORY.resolve("proxies.json");

    public static boolean globalEnabled = false;
    public static boolean proxyWarningEnabled = false;
    public static String activeProfileName = "";
    public static List<ProxyProfiles> profiles = new ArrayList<>();
    public static List<String> customAccountPaths = new ArrayList<>();

    public static void save() {
        ConfigData data = new ConfigData(globalEnabled, proxyWarningEnabled, activeProfileName, profiles, customAccountPaths);
        AsyncConfigs.save(data, FILE, "proxy configs");
    }

    public static CompletableFuture<Void> load() {
        return AsyncConfigs.load(ConfigData.class, FILE, "proxy configs").thenAccept(data -> {
            globalEnabled = data.globalEnabled;
            proxyWarningEnabled = data.proxyWarningEnabled;
            activeProfileName = data.activeProfileName;
            profiles = data.profiles != null ? data.profiles : new ArrayList<>();
            customAccountPaths = data.customAccountPaths != null ? data.customAccountPaths : new ArrayList<>();

            Map<String, ProxyProfiles> seen = new LinkedHashMap<>();
            for (ProxyProfiles p : profiles) {
                seen.putIfAbsent(p.name.toLowerCase(Locale.ROOT), p);
                MainClient.LOGGER.info("Removed {} as it's a duplicate proxy.", p.name);
            }
            profiles = new ArrayList<>(seen.values());
        });
    }

    public static ProxyProfiles getActiveProfile() {
        return profiles.stream().filter(p -> p.name.equals(activeProfileName)).findFirst().orElse(null);
    }

    public static String getWarningReason() {
        if (!globalEnabled)
            return ChatFormatting.GRAY + "You currently have proxies fully " + ChatFormatting.RED + "disabled" + ChatFormatting.GRAY + "!";
        if (getActiveProfile() == null)
            return ChatFormatting.GRAY + "You have no proxy profile " + ChatFormatting.RED + "selected" + ChatFormatting.GRAY + " for this account!";
        return "";
    }

    public static boolean shouldWarn() {
        return !globalEnabled || getActiveProfile() == null;
    }

    private record ConfigData(boolean globalEnabled, boolean proxyWarningEnabled, String activeProfileName, List<ProxyProfiles> profiles, List<String> customAccountPaths) {}


}