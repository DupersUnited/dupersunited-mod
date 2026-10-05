package wtf.dupers.dupersunited.features.account;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.FriendsService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.util.Util;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.auth.AuthManager;
import wtf.dupers.dupersunited.mixin.accessor.GuiAccessor;
import wtf.dupers.dupersunited.mixin.accessor.MinecraftClientAccessor;
import wtf.dupers.dupersunited.modules.render.NickModule;
import net.minecraft.client.Minecraft;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static wtf.dupers.dupersunited.MainClient.mc;

public class SessionManager {
    public static User originalSession;

    public static Boolean isSessionValid = null;
    public static boolean hasValidationStarted;

    public static void restoreSession() {
        NickModule mod = MainClient.MODULE_MANAGER.getModule(NickModule.class);
        if (mod != null && mod.isEnabled()) mod.username = originalSession.getName();
        setSession(SessionManager.originalSession);
    }

    public static User getSession() {
        return Minecraft.getInstance().getUser();
    }

    public static String getUsername() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static User createSession(String username, String uuidString, String ssid) {
        if (uuidString.length() == 32) {
            uuidString =
                uuidString.substring(0, 8) + "-" +
                    uuidString.substring(8, 12) + "-" +
                    uuidString.substring(12, 16) + "-" +
                    uuidString.substring(16, 20) + "-" +
                    uuidString.substring(20, 32);
        }

        //update username in nick idt this is very smart but like wtv bro
        NickModule mod = MainClient.MODULE_MANAGER.getModule(NickModule.class);
        if (mod != null && mod.isEnabled()) mod.username = username;
        User current = Minecraft.getInstance().getUser();
        Optional<String> xuid = current.getXuid();
        Optional<String> clientId = current.getClientId();

        return new User(
            username,
            UUID.fromString(uuidString),
            ssid,
            xuid,
            clientId
        );
    }

    public static User createSession(String username, UUID uuid, String ssid) {
        User current = Minecraft.getInstance().getUser();

        return new User(
            username,
            uuid,
            ssid,
            current.getXuid(),
            current.getClientId()
        );
    }

    public static void setSession(User session) {
        isSessionValid = null;
        hasValidationStarted = false;

        MinecraftClientAccessor client = (MinecraftClientAccessor) Minecraft.getInstance();
        client.dupersunited$setSession(session);

        YggdrasilAuthenticationService authService = new YggdrasilAuthenticationService(Minecraft.getInstance().getProxy());
        MinecraftSessionService sessionService = authService.createMinecraftSessionService();

        client.dupersunited$setGameProfileFuture(CompletableFuture.supplyAsync(() ->
                sessionService.fetchProfile(session.getProfileId(), true),
            Util.nonCriticalIoPool()));
        ((GuiAccessor) mc.gui).dupersunited$setSplashTextLoader(new SplashManager(session));
        UserApiService userApiService = authService.createUserApiService(session.getAccessToken());
        client.dupersunited$setUserApiService(userApiService);

        FriendsService friendsService = authService.createFriendsService(session.getAccessToken());
        RemoteFriendListUpdateHandler friendUpdateHandler =
            new RemoteFriendListUpdateHandler(friendsService, Minecraft.getInstance());

        client.dupersunited$setSocialInteractionsManager(
            new PlayerSocialManager(
                Minecraft.getInstance(),
                userApiService,
                friendsService,
                friendUpdateHandler
            )
        );
        client.dupersunited$setProfileKeys(ProfileKeyPairManager.create(userApiService, session, FabricLoader.getInstance().getGameDir()));
        client.dupersunited$setAbuseReportContext(ReportingContext.create(ReportEnvironment.local(), userApiService));

        AuthManager.onMinecraftAccountChanged();
    }
}