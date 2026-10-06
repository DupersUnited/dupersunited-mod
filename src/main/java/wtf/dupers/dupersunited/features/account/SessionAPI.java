package wtf.dupers.dupersunited.features.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.util.UndashedUuid;
import net.minecraft.client.User;
import wtf.dupers.dupersunited.MainClient;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static java.util.UUID.randomUUID;

public class SessionAPI {
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    public static String getJson(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "Mozilla/5.0")
            .GET().build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404 || response.statusCode() == 204) throw new Exception("Player not found");
        if (response.statusCode() == 429) throw new Exception("Rate limited by Mojang, try again in a minute");
        if (response.statusCode() != 200) throw new Exception("Lookup failed (HTTP " + response.statusCode() + ")");
        return response.body();
    }

    public static byte[] downloadBytes(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url.replace("http://", "https://")))
            .timeout(Duration.ofSeconds(15))
            .header("User-Agent", "Mozilla/5.0")
            .GET().build();
        HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new Exception("Download failed (HTTP " + response.statusCode() + ")");
        return response.body();
    }

    public static String[] getProfileInfo(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + token)
                .GET().build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
            if (jsonObject == null || !jsonObject.has("name") || !jsonObject.has("id")) return null;

            return new String[] { jsonObject.get("name").getAsString(), jsonObject.get("id").getAsString() };
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean validateSession(String token) {
        try {
            String[] profileInfo = getProfileInfo(token);
            if (profileInfo == null || profileInfo.length < 2) return false;

            String ign = profileInfo[0];
            String uuidString = profileInfo[1];

            UUID uuid = uuidString.contains("-")
                ? UUID.fromString(uuidString)
                : UndashedUuid.fromString(uuidString);

            User session = Minecraft.getInstance().getUser();
            return ign.equalsIgnoreCase(session.getName()) && uuid.equals(session.getProfileId());
        } catch (Exception e) {
            MainClient.LOGGER.error("something went wrong with session api", e);
            return false;
        }
    }

    public static int changeSkin(String url, String variant, String token) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("variant", variant);
            body.addProperty("url", url);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    public static int uploadSkin(byte[] png, String variant, String token) {
        try {
            String boundary = "----DupersBoundary" + randomUUID().toString().replace("-", "");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"variant\"\r\n\r\n" + variant + "\r\n")
                .getBytes(StandardCharsets.UTF_8));
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\nContent-Type: image/png\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
            out.write(png);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                .build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    public static SkinInfo extractSkinInfoFromProperties(JsonArray properties) {
        for (JsonElement el : properties) {
            JsonObject prop = el.getAsJsonObject();
            if (!prop.has("name") || !"textures".equals(prop.get("name").getAsString())) continue;
            try {
                String decoded = new String(Base64.getDecoder().decode(prop.get("value").getAsString()), StandardCharsets.UTF_8);
                JsonObject texturesRoot = JsonParser.parseString(decoded).getAsJsonObject();
                JsonObject all = texturesRoot.has("textures") ? texturesRoot.getAsJsonObject("textures") : null;
                if (all == null || !all.has("SKIN")) continue;
                JsonObject skin = all.getAsJsonObject("SKIN");
                if (skin == null || !skin.has("url")) continue;

                String url = skin.get("url").getAsString().replace("http://", "https://");
                String variant = "classic";
                if (skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")) {
                    variant = "slim".equalsIgnoreCase(skin.getAsJsonObject("metadata").get("model").getAsString()) ? "slim" : "classic";
                }
                return new SkinInfo(url, variant);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static String[] getCurrentSkin(String token) {
        try {
            String json = getJson("https://api.minecraftservices.com/minecraft/profile");
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray skins = root.has("skins") ? root.getAsJsonArray("skins") : null;
            if (skins != null) {
                for (JsonElement el : skins) {
                    JsonObject skin = el.getAsJsonObject();
                    if (!skin.has("url")) continue;
                    boolean active = !skin.has("state") || "ACTIVE".equalsIgnoreCase(skin.get("state").getAsString());
                    if (!active) continue;
                    String variant = skin.has("variant") ? skin.get("variant").getAsString().toLowerCase() : "classic";
                    return new String[] { skin.get("url").getAsString().replace("http://", "https://"), variant };
                }
            }
        } catch (Exception e) {
            //MainClient.LOGGER.error("something went wrong with grabbing skin", e);
            //silently fail now its fine
        }
        return null;
    }

    public static String[] getSkinByUuid(String uuid) {
        try {
            String cleanUuid = uuid.replace("-", "");
            String json = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + cleanUuid);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("properties")) {
                SkinInfo info = extractSkinInfoFromProperties(root.getAsJsonArray("properties"));
                if (info != null) {
                    return new String[] { info.url(), info.variant() };
                }
            }
        } catch (Exception e) {
            MainClient.LOGGER.error("issue with skin by uuid", e);
        }
        return null;
    }

    public static int resetSkin(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/skins/active"))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    //TODO: add this in a later release,
    //perhaps somewhere in the ChangeSkinScreem? Change it to be able to swap capes, ign, etc?
    public static int changeName(String newName, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/name/" + newName))
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(""))
                .build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (Exception e) {
            return -1;
        }
    }

    public record SkinInfo(String url, String variant) {}
}