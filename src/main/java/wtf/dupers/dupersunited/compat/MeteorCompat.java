package wtf.dupers.dupersunited.compat;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.account.AccountsScreen;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * dumb shit magic stuff
 * @author Crosby
 */
public final class MeteorCompat {
    private static boolean errored = false;

    public static boolean isPresent() {
        return FabricLoader.getInstance().isModLoaded("meteor-client");
    }

    public static List<AccountsScreen.AccountEntry> getAccounts() {
        if (!isPresent()) return List.of();

        try {
            Class<?> accountsClass = Class.forName("meteordevelopment.meteorclient.systems.accounts.Accounts");
            Method accounts_get_Method = accountsClass.getDeclaredMethod("get");
            Iterable<?> accounts = (Iterable<?>) accounts_get_Method.invoke(null);

            if (accounts == null) return List.of();

            Class<?> accountClass = Class.forName("meteordevelopment.meteorclient.systems.accounts.Account");
            Method account_fetchInfo_Method = accountClass.getDeclaredMethod("fetchInfo");
            Method account_getUsername_Method = accountClass.getDeclaredMethod("getUsername");

            @Nullable Class<?> sessionAccountClass;
            Field sessionAccount_accessToken_Field = null;
            try {
                sessionAccountClass = Class.forName("meteordevelopment.meteorclient.systems.accounts.types.SessionAccount");
                sessionAccount_accessToken_Field = sessionAccountClass.getDeclaredField("accessToken");
                sessionAccount_accessToken_Field.setAccessible(true);
            } catch (ClassNotFoundException e) {
                sessionAccountClass = null;
            }


            Class<?> microsoftAccountClass = Class.forName("meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount");
            Field microsoftAccount_token_Field = microsoftAccountClass.getDeclaredField("token");
            microsoftAccount_token_Field.setAccessible(true);

            List<AccountsScreen.AccountEntry> meteorAccountEntries = new ArrayList<>();

            for (Object account : accounts) {
                String token;
                if (sessionAccountClass != null && sessionAccountClass.isInstance(account)) {
                    token = (String) sessionAccount_accessToken_Field.get(account);
                } else if (microsoftAccountClass.isInstance(account)) {
                    token = (String) microsoftAccount_token_Field.get(account);
                    if (token == null) {
                        boolean isValid = (boolean) account_fetchInfo_Method.invoke(account);
                        if (isValid) {
                            token = (String) microsoftAccount_token_Field.get(account);
                        } else {
                            continue;
                        }
                    }
                } else {
                    continue;
                }

                String username = (String) account_getUsername_Method.invoke(account);

                meteorAccountEntries.add(new AccountsScreen.AccountEntry(username, token, "Meteor"));
            }

            return meteorAccountEntries;
        } catch (ReflectiveOperationException e) {
            error(e);
            return List.of();
        }
    }

    public static boolean shouldWarnUnsafeModules() {
        if (!isPresent()) return false;

        try {
            Class<?> modulesClass = Class.forName("meteordevelopment.meteorclient.systems.modules.Modules");
            Method modules_get_Method = modulesClass.getDeclaredMethod("get");
            @Nullable Object modules = modules_get_Method.invoke(null);
            if (modules == null) {
                return false; // meteor modules not initialized yet
            }

            Method modules_getActive_Method = modulesClass.getDeclaredMethod("getActive");
            List<?> activeModules = (List<?>) modules_getActive_Method.invoke(modules);

            Class<?> moduleClass = Class.forName("meteordevelopment.meteorclient.systems.modules.Module");
            Field module_category_Field = moduleClass.getDeclaredField("category");
            Field module_name_Field = moduleClass.getDeclaredField("name");

            Class<?> categoriesClass = Class.forName("meteordevelopment.meteorclient.systems.modules.Categories");
            Object combatCategory = categoriesClass.getDeclaredField("Combat").get(null);
            Object playerCategory = categoriesClass.getDeclaredField("Player").get(null);
            Object movementCategory = categoriesClass.getDeclaredField("Movement").get(null);
            Object worldCategory = categoriesClass.getDeclaredField("World").get(null);

            Set<String> unsafePlayerModules = Set.of(
                "air-place",
                "anti-hunger",
                "auto-fish",
                "fast-use",
                "ghost-hand",
                "instant-rebreak",
                "liquid-interact",
                "multitask",
                "no-interact",
                "no-mining-trace",
                "reach",
                "speed-mine"
            );

            Set<String> unsafeWorldModules = Set.of(
                "auto-breed",
                "auto-brewer",
                "auto-mount",
                "auto-nametag",
                "auto-shearer",
                "auto-smelter",
                "collisions",
                "excavator",
                "flamethrower",
                "highway-builder",
                "infinity-miner",
                "liquid-filler",
                "nuker",
                "packet-mine",
                "spawn-proofer",
                "timer",
                "vein-miner"
            );

            for (Object module : activeModules) {
                Object category = module_category_Field.get(module);

                if (category == combatCategory || category == movementCategory) {
                    return true;
                }

                String name = (String) module_name_Field.get(module);
                if (category == playerCategory && unsafePlayerModules.contains(name)) {
                    return true;
                } else if (category == worldCategory && unsafeWorldModules.contains(name)) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException e) {
            error(e);
        }

        return false;
    }

    public static int getTitleScreenYOffset() {
        if (!isPresent()) return 0;

        try {
            Class<?> entrypointType = Class.forName("meteordevelopment.meteorclient.addons.MeteorAddon");
            int entries = 1 + FabricLoader.getInstance().getEntrypoints("meteor", entrypointType).size();
            return (Minecraft.getInstance().font.lineHeight + 2) * entries - 2;
        } catch (ClassNotFoundException e) {
            error(e);
            return 0;
        }
    }

    private static void error(Throwable t) {
        if (!errored) {
            MainClient.LOGGER.error("Error loading meteor client compat", t);
            errored = true;
        }
    }
}
