package wtf.dupers.dupersunited.features.autologin;

import com.google.gson.reflect.TypeToken;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.AsyncConfigs;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AutoLoginManager {
    private static final Path PATH = SharedVariables.DIRECTORY.resolve("autologin.json");
    private static final List<AutoLoginAccount> accounts = new ArrayList<>();

    public static void load() {
        List<AutoLoginAccount> loaded = AsyncConfigs.loadSync(new TypeToken<List<AutoLoginAccount>>() {}, PATH, "autologin.json");
        accounts.clear();
        if (loaded != null) {
            accounts.addAll(loaded);
        }
    }

    public static void save() {
        AsyncConfigs.save(accounts, new TypeToken<List<AutoLoginAccount>>() {}.getType(), PATH, "autologin.json");
    }

    public static String getPassword(String username) {
        for (AutoLoginAccount account : accounts) {
            if (account.getUsername().equalsIgnoreCase(username)) {
                return account.getPassword();
            }
        }
        return null;
    }

    public static void saveAccount(String username, String password) {
        accounts.removeIf(acc -> acc.getUsername().equalsIgnoreCase(username));
        accounts.add(new AutoLoginAccount(username, password));
        save();
    }
}