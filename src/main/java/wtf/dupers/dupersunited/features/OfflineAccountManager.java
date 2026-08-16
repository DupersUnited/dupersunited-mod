package wtf.dupers.dupersunited.features;

import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.AsyncConfigs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class OfflineAccountManager {
    private static final Path FILE = SharedVariables.DIRECTORY.resolve("offlineaccounts.json");

    public static List<OfflineAccount> accounts = new ArrayList<>();

    public static void save() {
        ConfigData data = new ConfigData(accounts);
        AsyncConfigs.save(data, FILE, "offline accounts");
    }

    public static CompletableFuture<Void> load() {
        return AsyncConfigs.load(ConfigData.class, FILE, "offline accounts").thenAccept(data -> {
            accounts = data.accounts != null ? data.accounts : new ArrayList<>();
        });
    }

    public static OfflineAccount create(String username) {
        UUID uuid = offlineUuid(username);
        OfflineAccount account = new OfflineAccount(username, uuid.toString());
        accounts.add(account);
        save();
        return account;
    }

    public static void delete(String username) {
        accounts.removeIf(a -> a.username.equalsIgnoreCase(username));
        save();
    }

    public static boolean exists(String username) {
        return accounts.stream().anyMatch(a -> a.username.equalsIgnoreCase(username));
    }

    public static OfflineAccount get(String username) {
        return accounts.stream().filter(a -> a.username.equalsIgnoreCase(username)).findFirst().orElse(null);
    }

    public static UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    public static class OfflineAccount {
        public String username;
        public String uuid;

        public OfflineAccount(String username, String uuid) {
            this.username = username;
            this.uuid = uuid;
        }
    }

    private record ConfigData(List<OfflineAccount> accounts) {}
}

