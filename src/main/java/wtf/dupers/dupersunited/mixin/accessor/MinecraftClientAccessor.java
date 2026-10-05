package wtf.dupers.dupersunited.mixin.accessor;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {
    @Mutable @Accessor("user")
    void dupersunited$setSession(User session);

    @Mutable @Accessor("profileFuture")
    void dupersunited$setGameProfileFuture(CompletableFuture<ProfileResult> future);

    @Mutable @Accessor("userApiService")
    void dupersunited$setUserApiService(UserApiService userApiService);

    @Mutable @Accessor("playerSocialManager")
    void dupersunited$setSocialInteractionsManager(PlayerSocialManager socialInteractionsManager);

    @Mutable @Accessor("profileKeyPairManager")
    void dupersunited$setProfileKeys(ProfileKeyPairManager profileKeys);

    @Accessor("reportingContext")
    void dupersunited$setAbuseReportContext(ReportingContext abuseReportContext);
}