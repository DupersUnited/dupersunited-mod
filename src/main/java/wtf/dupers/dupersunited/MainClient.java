package wtf.dupers.dupersunited;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.jetbrains.annotations.Nullable;
import wtf.dupers.dupersunited.api.DupersUnitedAddon;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.commands.subcommands.*;
import wtf.dupers.dupersunited.events.GuiEvent;
import wtf.dupers.dupersunited.events.TickEvent;
import wtf.dupers.dupersunited.events.WorldEvent;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.features.HudOverlay;
import wtf.dupers.dupersunited.features.ServerAlertConfig;
import wtf.dupers.dupersunited.features.auth.AuthManager;
import wtf.dupers.dupersunited.features.chatmacros.ChatMacroManager;
import wtf.dupers.dupersunited.features.proxies.AccountProxyLinks;
import wtf.dupers.dupersunited.features.proxies.ProxyConfigManager;
import wtf.dupers.dupersunited.features.screens.mainmenu.WelcomeScreen;
import wtf.dupers.dupersunited.features.screens.mainmenu.alerts.HallOfFame;
import wtf.dupers.dupersunited.features.screens.mainmenu.alerts.HallOfShame;
import wtf.dupers.dupersunited.keybinds.*;
import wtf.dupers.dupersunited.modules.glitcha.*;
import wtf.dupers.dupersunited.modules.misc.*;
import wtf.dupers.dupersunited.modules.render.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.dupers.dupersunited.features.ssidLogin.SessionManager;
import wtf.dupers.dupersunited.modules.ModuleManager;
import wtf.dupers.dupersunited.modules.exploit.AnySignModule;
import wtf.dupers.dupersunited.modules.exploit.BookBotModule;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static wtf.dupers.dupersunited.features.ssidLogin.SessionManager.*;

public class MainClient implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("DupersUnited");
    public static final String MOD_ID = "dupersunited";
    public static ModuleManager MODULE_MANAGER;
    public static boolean addonsPresent = false;
    private static Map<String, Command> COMMANDS;

    @Override
    public void onInitialize() {
        System.setProperty("java.awt.headless", "false");

        //SSID Login
        originalSession = SessionManager.getSession();

        //registry
        DupersUnitedRegistryImpl registry = new DupersUnitedRegistryImpl();
        registry.namespace = "dupersunited";

        registry.registerModules(
            new EspModule(),
            new FullBrightModule(),
            new AutoSprintModule(),
            new PacketLoggerModule(),
            new WatermarkModule(),
            new HidePlayersModule(),
            new AnySignModule(),
            new NickModule(),
            new GuiUtilsModule(),
            new TpsCounterModule(),
            new FreeLookModule(),
            new FreecamModule(),
            new WarnUnsafeModule(),
            new HudModule(),
            new BookBotModule(),
            new PropagandaModule(),
            new PacketDelayModule(),
            new PayAllSettingsModule(),
            new InvDropModule(),
            new BlockEspModule(),
            new CosmeticsModule(),
            new NoRenderModule(),
            new NoTextureRotationsModule(),
            new BetterTabModule(),
            new ChatStackerModule(),
            new ClickSlotModule(),
            new VanillaFlyModule(),
            new NoFallModule(),
            new SpamModule()
        );

        registry.registerCommands(
            new ClickSlotCommand(),
            new DropCommand(),
            new DupeCommand(),
            new ForceOpCommand(),
            new HelpCommand(),
            new KeybindCommand(),
            new KickCommand(),
            new ModuleCommand(),
            new NbtCommand(),
            new NewCommandsCommand(),
            new PayAllCommand(),
            new PluginsCommand(),
            new QuoteCommand(),
            new ReloadConfigCommand(),
            new ReplaceBlockCommand(),
            new RestoreGhostsCommand(),
            new SetHandCommand(),
            new ToggleCommand(),
            new WaitCommand()
        );

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            registry.registerCommand(new MixinAuditCommand());
        }

        registry.registerKeybinds(
            new GhostBlockKeybind(),
            JoinServerInviteKeybind.INSTANCE,
            new PacketPauseKeybind(),
            new RestoreGuiKeybind(),
            new RevertGhostBlockKeybind(),
            new SaveGuiKeybind()
        );

        // addon initialization
        var addonContainers = FabricLoader.getInstance().getEntrypointContainers("dupersunited:addon", DupersUnitedAddon.class);
        for (EntrypointContainer<DupersUnitedAddon> addonContainer : addonContainers) {
            addonsPresent = true;
            registry.namespace = addonContainer.getProvider().getMetadata().getId();
            addonContainer.getEntrypoint().initialize(registry);
        }

        // apply registry
        MODULE_MANAGER = new ModuleManager(registry.modules);
        MainCommand.register(COMMANDS = Collections.unmodifiableMap(registry.commands));
        registry.keybinds.forEach(KeybindManager::registerKeybind);

        //config
        CompletableFuture<Void> proxyConfigTask = CompletableFuture.allOf(
            ProxyConfigManager.load(),
            AccountProxyLinks.load()
        ).thenAccept(nil -> {
            // auto apply proxy linked to the launch account if it exists
            String launchUsername = SessionManager.getSession() != null ? SessionManager.getSession().getUsername() : null;
            if (launchUsername != null && AccountProxyLinks.hasLink(launchUsername) && !AccountProxyLinks.hasBypass(launchUsername)) {
                String linkedProxy = AccountProxyLinks.getLinkedProxy(launchUsername);
                ProxyConfigManager.activeProfileName = linkedProxy;
                ProxyConfigManager.globalEnabled = true;
                ProxyConfigManager.save();
                //LOGGER.info("Auto Applied Proxy Profile '{}' for launch account '{}'", linkedProxy, launchUsername); worked
            } else if (launchUsername != null && !AccountProxyLinks.hasLink(launchUsername)) {
                // no proxy linked to this account, make sure proxy is disabled haha oopsie
                ProxyConfigManager.globalEnabled = false;
                ProxyConfigManager.activeProfileName = "";
                ProxyConfigManager.save();
                //LOGGER.info("No proxy linked for launch account '{}', disabling proxy!", launchUsername); bamgangnbang
            }
        });

        CompletableFuture<Void> configLoadTask = CompletableFuture.allOf(
            proxyConfigTask,
            ConfigManager.load(),
            ServerAlertConfig.load(),
            ChatMacroManager.load()
        );

        HallOfShame.prefetch();
        HallOfFame.prefetch();

        //config
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            FreecamModule freecam = MODULE_MANAGER.getModule(FreecamModule.class);
            if (freecam != null && freecam.isEnabled()) freecam.setEnabled(false);

            ConfigManager.saveBlocking();
        }));

        //events
        WorldEvent.register();
        GuiEvent.register();
        TickEvent.register();

        //keybinds
        ClickGuiKeybind.register();

        //other
        HudOverlay.init();
        AuthManager.init();

        // first launch shizz
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof TitleScreen && ConfigManager.firstLaunch) {
                ConfigManager.firstLaunch = false;
                ConfigManager.save();
                client.execute(() -> {
                    client.setScreen(new WelcomeScreen(screen));
                });
            }
        });

        // ensure configs loaded before finishing init
        try {
            configLoadTask.join();
        } catch (Exception ignored) {}
    }

    public static Collection<Module> getModules() {
        return MODULE_MANAGER.modules();
    }

    @Nullable
    public static <T extends Module> T getModule(Class<T> moduleClass) {
        return MODULE_MANAGER.getModule(moduleClass);
    }

    @Nullable
    public static Module getModule(String moduleName) {
        return MODULE_MANAGER.getModuleByName(moduleName);
    }

    public static Collection<Command> getCommands() {
        return COMMANDS.values();
    }

    @Nullable
    public static <T extends Command> T getCommand(Class<T> commandClass) {
        return COMMANDS.values().stream()
            .filter(commandClass::isInstance)
            .map(commandClass::cast)
            .findFirst().orElse(null);
    }
}
