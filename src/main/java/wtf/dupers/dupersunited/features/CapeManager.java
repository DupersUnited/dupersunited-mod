package wtf.dupers.dupersunited.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.auth.AuthManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CapeManager {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final Map<String, Identifier> textures = new ConcurrentHashMap<>();
    private static final Set<String> textureLoads = ConcurrentHashMap.newKeySet();

    private static final Map<UUID, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();
    private static final Set<UUID> profileLoads = ConcurrentHashMap.newKeySet();

    private static final long PLAYER_CACHE_MS = 60_000L;
    private static final long FAILED_PROFILE_CACHE_MS = 10_000L;
    private static final int MAX_CAPE_BYTES = 2 * 1024 * 1024;

    @Nullable
    public static Identifier getProfile(UUID uuid) {
        long now = System.currentTimeMillis();
        PlayerProfile cached = playerProfiles.get(uuid);

        if (cached == null || cached.expiresAt < now) {
            fetchProfileAsync(uuid);
            return null;
        }

        if (cached.url == null || cached.url.isBlank()) {
            return null;
        }

        return getTextureOrLoad(cached.url);
    }

    public static void setCape(String capeKey) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String url = AuthManager.getCapeCatalog().textureUrls().get(capeKey);
        playerProfiles.put(client.player.getUuid(), new PlayerProfile(url, Long.MAX_VALUE));

        if (url != null && !url.isBlank()) {
            loadTextureAsync(url);
        }
    }

    public static void disableCape() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        playerProfiles.put(client.player.getUuid(), new PlayerProfile(null, Long.MAX_VALUE));
    }

    public static void clearPlayer(UUID uuid) {
        if (uuid != null) {
            playerProfiles.remove(uuid);
            profileLoads.remove(uuid);
        }
    }

    @Nullable
    public static Identifier getPreviewTexture(String capeKey) {
        if (capeKey == null || capeKey.isBlank()) {
            return null;
        }

        String url = AuthManager.getCapeCatalog().textureUrls().get(capeKey);
        if (url == null || url.isBlank()) {
            return null;
        }

        return getTextureOrLoad(url);
    }

    private static void fetchProfileAsync(UUID uuid) {
        if (!profileLoads.add(uuid)) {
            return; // already loading
        }

        Thread.ofVirtual().start(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(AuthManager.getApiBaseUrl() + "/profile/" + uuid))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    playerProfiles.put(uuid, new PlayerProfile(null, System.currentTimeMillis() + PLAYER_CACHE_MS));
                    return;
                }

                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                String url = (!root.has("cape") || root.get("cape").isJsonNull())
                        ? null
                        : root.get("cape").getAsString();

                playerProfiles.put(uuid, new PlayerProfile(url, System.currentTimeMillis() + PLAYER_CACHE_MS));
                if (url != null && !url.isBlank()) {
                    loadTextureAsync(url);
                }
            } catch (Exception exception) {
                playerProfiles.put(uuid, new PlayerProfile(null, System.currentTimeMillis() + FAILED_PROFILE_CACHE_MS));
            } finally {
                profileLoads.remove(uuid);
            }
        });
    }

    @Nullable
    private static Identifier getTextureOrLoad(String url) {
        Identifier existing = textures.get(url);
        if (existing != null) {
            return existing;
        }

        loadTextureAsync(url);
        return null;
    }

    private static void loadTextureAsync(String url) {
        if (!textureLoads.add(url)) {
            return; // already loading
        }

        Thread.ofVirtual().start(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
                NativeImage image;

                try (InputStream body = response.body()) {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        return;
                    }

                    long declaredLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                    if (declaredLength > MAX_CAPE_BYTES) {
                        MainClient.LOGGER.warn("Cape texture exceeded the size limit: {}", url);
                        return;
                    }

                    byte[] imageBytes = body.readNBytes(MAX_CAPE_BYTES + 1);
                    if (imageBytes.length > MAX_CAPE_BYTES) {
                        MainClient.LOGGER.warn("Cape texture exceeded the size limit: {}", url);
                        return;
                    }

                    image = NativeImage.read(new ByteArrayInputStream(imageBytes));
                }

                Identifier identifier = Identifier.of(
                        "dupersunited",
                        "cape/" + UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "")
                );

                MinecraftClient.getInstance().execute(() -> {
                    try {
                        NativeImageBackedTexture nativeTexture = new NativeImageBackedTexture(() -> "cape_" + url.hashCode(), image);
                        MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, nativeTexture);
                        textures.put(url, identifier);
                    } catch (Exception e) {
                        image.close();
                    }
                });
            } catch (Exception exception) {
                MainClient.LOGGER.error("Failed to load cape texture {}", url, exception);
            } finally {
                textureLoads.remove(url);
            }
        });
    }

    private record PlayerProfile(String url, long expiresAt) {}
}