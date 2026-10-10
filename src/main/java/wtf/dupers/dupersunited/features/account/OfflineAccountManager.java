package wtf.dupers.dupersunited.features.account;

import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.AsyncConfigs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class OfflineAccountManager {
    private static final Path FILE = SharedVariables.DIRECTORY.resolve("offlineaccounts.json");

    public static List<OfflineAccount> accounts = new ArrayList<>();
    public static NameMode nameMode = NameMode.FAKE;

    public static void save() {
        ConfigData data = new ConfigData(accounts, nameMode);
        AsyncConfigs.save(data, FILE, "offline accounts");
    }

    public static CompletableFuture<Void> load() {
        return AsyncConfigs.load(ConfigData.class, FILE, "offline accounts").thenAccept(data -> {
            accounts = data.accounts != null ? data.accounts : new ArrayList<>();
            if (data.nameMode != null) nameMode = data.nameMode;
        });
    }

    public static OfflineAccount create(String username) {
        UUID uuid = offlineUuid(username);
        OfflineAccount account = new OfflineAccount(username, uuid.toString());
        accounts.add(account);
        save();
        return account;
    }

    private static final String[] NAME_STARTS = {"Shadow", "Dark", "Light", "Night", "Fire", "Ice", "Storm", "Thunder", "Ghost", "Pixel", "Craft", "Mine", "Diamond", "Gold", "Iron", "Red", "Blue", "Green", "Hyper", "Ultra", "Super", "Mega", "Pro", "Epic", "Magic", "Dragon", "Wolf", "Fox", "Tiger", "Lion", "Ender", "Nether", "Sky", "Cloud", "Star", "Moon", "Solar", "Lunar", "Void", "Toxic", "Venom", "Frost", "Blaze", "Ember", "Aqua", "Terra", "Lucky", "Silent", "Swift", "Wild"};
    private static final String[] NAME_ENDS = {"gamer", "plays", "craft", "miner", "hunter", "slayer", "king", "queen", "lord", "master", "pro", "yt", "tv", "god", "dragon", "wolf", "fox", "bear", "eagle", "shark", "ninja", "warrior", "knight", "wizard", "archer", "sniper", "legend", "hero", "beast", "claw", "fang", "wing", "fire", "blade", "storm", "dash", "rush", "zone", "hub", "ville", "top", "x", "z"};
    private static final String NAME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";

    public enum NameMode {
        FAKE,
        CHARS
    }

    public static void cycleNameMode() {
        nameMode = nameMode == NameMode.FAKE ? NameMode.CHARS : NameMode.FAKE;
        save();
    }

    public static String randomName() {
        String name;
        do {
            name = nameMode == NameMode.CHARS ? charsName() : fakeName();
        } while (exists(name));
        return name;
    }

    private static String fakeName() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String base = NAME_STARTS[random.nextInt(NAME_STARTS.length)] + NAME_ENDS[random.nextInt(NAME_ENDS.length)];
        if (base.length() > 12) base = base.substring(0, 12);
        String lower = base.toLowerCase();
        String name = switch (random.nextInt(14)) {
            case 0 -> base + random.nextInt(1000);
            case 1 -> "xX" + base + "Xx";
            case 2 -> base + "_" + random.nextInt(100);
            case 3 -> "_" + base + "_";
            case 4 -> "Itz" + base;
            case 5 -> lower;
            case 6 -> lower + random.nextInt(100);
            case 7 -> "The" + base;
            case 8 -> "Real" + base;
            case 9 -> base + "HD";
            case 10 -> base + "MC";
            case 11 -> base + "YT";
            case 12 -> "Xx" + base + "_" + random.nextInt(10);
            default -> base;
        };
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    private static String charsName() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        int len = 6 + random.nextInt(5);
        for (int i = 0; i < len; i++) sb.append(NAME_CHARS.charAt(random.nextInt(NAME_CHARS.length())));
        return sb.toString();
    }

    public static OfflineAccount createRandom() {
        return create(randomName());
    }

    public static List<OfflineAccount> createRandomAccounts(int quantity) {
        List<OfflineAccount> out = new ArrayList<>();
        for (int i = 0; i < quantity; i++) out.add(createRandom());
        return out;
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

    private record ConfigData(List<OfflineAccount> accounts, NameMode nameMode) {}
}

