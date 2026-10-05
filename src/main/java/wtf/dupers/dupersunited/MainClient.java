package wtf.dupers.dupersunited;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import wtf.dupers.dupersunited.api.DupersUnitedAddon;
import wtf.dupers.dupersunited.api.command.Command;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.commands.MainCommand;
import wtf.dupers.dupersunited.commands.subcommands.*;
import wtf.dupers.dupersunited.events.*;
import wtf.dupers.dupersunited.features.*;
import wtf.dupers.dupersunited.features.account.OfflineAccountManager;
import wtf.dupers.dupersunited.features.auth.*;
import wtf.dupers.dupersunited.features.chatmacros.*;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import wtf.dupers.dupersunited.features.proxies.*;
import wtf.dupers.dupersunited.features.screens.hud.HudOverlay;
import wtf.dupers.dupersunited.features.screens.mainmenu.*;
import wtf.dupers.dupersunited.features.screens.mainmenu.alerts.*;
import wtf.dupers.dupersunited.keybinds.*;
import wtf.dupers.dupersunited.modules.glitcha.*;
import wtf.dupers.dupersunited.modules.misc.*;
import wtf.dupers.dupersunited.modules.render.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wtf.dupers.dupersunited.features.account.SessionManager;
import wtf.dupers.dupersunited.modules.ModuleManager;
import wtf.dupers.dupersunited.modules.exploit.AnySignModule;
import wtf.dupers.dupersunited.modules.exploit.BookBotModule;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static wtf.dupers.dupersunited.features.account.SessionManager.*;

public class MainClient implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("DupersUnited");
    public static final String MOD_ID = "dupersunited";
    public static ModuleManager MODULE_MANAGER;
    public static boolean addonsPresent = false;
    private static Map<String, Command> COMMANDS;

    public static Minecraft mc = Minecraft.getInstance();

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
            new SpamModule(),
            new ServerAlertsModule(),
            new AutoReconnectModule(),
            new RpBypassModule(),
            new BrandSpoofModule(),
            new AutoLoginModule(),
            new MacroGuiSettingsModule()
        );

        registry.registerCommands(
            new ClickSlotCommand(),
            new DropCommand(),
            new DupeCommand(),
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
            new WaitCommand(),
            new MacroGuiCommand()
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

        //macros
        GuiMacro.getInstance().loadMacros();

        //config
        CompletableFuture<Void> proxyConfigTask = CompletableFuture.allOf(
            ProxyConfigManager.load(),
            AccountProxyLinks.load(),
            OfflineAccountManager.load()
        ).thenAccept(nil -> {
            // auto apply proxy linked to the launch account if it exists
            String launchUsername = SessionManager.getSession() != null ? SessionManager.getSession().getName() : null;
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
            if (screen instanceof TitleScreen && (ConfigManager.firstLaunch || !ConfigManager.agreedToPolicy)) {
                if (ConfigManager.firstLaunch) {
                    ConfigManager.firstLaunch = false;
                    ConfigManager.save();
                }
                client.execute(() -> {
                    client.gui.setScreen(new WelcomeScreen(screen));
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