package wtf.dupers.dupersunited.features.proxies;

import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.AsyncConfigs;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class AccountProxyLinks {
    private static final Path FILE = SharedVariables.DIRECTORY.resolve("accountsproxies.json");

    public static Map<String, String> links = new HashMap<>();
    public static Set<String> bypassAccounts = new ObjectOpenHashSet<>();
    public static List<String> accountOrder = new ArrayList<>();

    public static void save() {
        ConfigData data = new ConfigData(links, bypassAccounts, accountOrder);
        AsyncConfigs.save(data, FILE, "account proxy links");
    }

    public static CompletableFuture<Void> load() {
        return AsyncConfigs.load(ConfigData.class, FILE, "account proxy links").thenAccept(data -> {
            links = data.links != null ? data.links : new HashMap<>();
            bypassAccounts = data.bypassAccounts != null ? data.bypassAccounts : new ObjectOpenHashSet<>();
            accountOrder = data.accountOrder != null ? data.accountOrder : new ArrayList<>();
        });
    }

    public static void link(String accountName, String proxyProfileName) {
        links.put(accountName, proxyProfileName);
        save();
    }

    public static void unlink(String accountName) {
        links.remove(accountName);
        save();
    }

    public static String getLinkedProxy(String accountName) {
        return links.get(accountName);
    }

    public static boolean hasLink(String accountName) {
        return links.containsKey(accountName);
    }

    public static boolean hasBypass(String accountName) {
        return bypassAccounts.contains(accountName);
    }

    public static void toggleBypass(String accountName) {
        if (bypassAccounts.contains(accountName)) {
            bypassAccounts.remove(accountName);
        } else {
            bypassAccounts.add(accountName);
        }
        save();
    }
    public static void saveOrder(List<String> order) {
        accountOrder = new ArrayList<>(order);
        save();
    }

    private record ConfigData(Map<String, String> links, Set<String> bypassAccounts, List<String> accountOrder) {}
}