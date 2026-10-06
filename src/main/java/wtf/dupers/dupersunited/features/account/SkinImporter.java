package wtf.dupers.dupersunited.features.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkinImporter {
    private SkinImporter() {}

    public record ImportedSkin(byte[] png, String variant) {
    }

    private static final int MAXBYTE = 1024 * 1024;
    private static final Pattern NAMEMC = Pattern.compile("^(?:[a-z0-9-]+\\.)?namemc\\.com/.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMESKINTHING = Pattern.compile(
        "^https?://(?:[a-z0-9-]+\\.)?namemc\\.com/skin/([0-9a-fA-F]{16})(?:[/?#].*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMEPROFILETHING = Pattern.compile(
        "^https?://(?:[a-z0-9-]+\\.)?namemc\\.com/profile/([^/?#]+)(?:[/?#].*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final Pattern LOOKSLIKEUUID = Pattern.compile("^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$");

    public static ImportedSkin resolve(String rawInput) throws Exception {
        String input = rawInput.trim();
        if (input.length() > 1 && input.startsWith("\"") && input.endsWith("\"")) {
            input = input.substring(1, input.length() - 1).trim();
        }

        if (input.isEmpty()) throw new Exception("Nothing to import");
        if (NAMEMC.matcher(input).matches()) input = "https://" + input;

        Path file = asFile(input);
        if (file != null) {
            if (Files.size(file) > MAXBYTE) throw new Exception("File is too large to be a skin???");
            return validate(Files.readAllBytes(file), null);
        }

        if (input.regionMatches(true, 0, "http://", 0, 7) || input.regionMatches(true, 0, "https://", 0, 8)) {
            Matcher skin = NAMESKINTHING.matcher(input);
            if (skin.matches()) {
                return validate(SessionAPI.downloadBytes("https://s.namemc.com/i/" + skin.group(1).toLowerCase() + ".png"), null);
            }
            Matcher profile = NAMEPROFILETHING.matcher(input);
            if (profile.matches()) {
                String who = profile.group(1).replaceAll("\\.\\d+$", "");
                return fromPlayer(who);
            }
            return validate(SessionAPI.downloadBytes(input), null);
        }

        if (USERNAME.matcher(input).matches() || LOOKSLIKEUUID.matcher(input).matches()) {
            return fromPlayer(input);
        }

        throw new Exception("Not a file, NameMC link, image link or username");
    }

    @Nullable
    private static Path asFile(String input) {
        try {
            Path p = Path.of(input);
            return Files.isRegularFile(p) ? p : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static ImportedSkin fromPlayer(String nameOrUuid) throws Exception {
        String uuid;
        if (LOOKSLIKEUUID.matcher(nameOrUuid).matches()) {
            uuid = nameOrUuid.replace("-", "");
        } else {
            JsonObject lookup = JsonParser.parseString(
                SessionAPI.getJson("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + nameOrUuid)).getAsJsonObject();
            if (!lookup.has("id")) throw new Exception("Player not found");
            uuid = lookup.get("id").getAsString();
        }

        JsonObject profile = JsonParser.parseString(
            SessionAPI.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid)).getAsJsonObject();
        JsonArray props = profile.has("properties") ? profile.getAsJsonArray("properties") : null;
        if (props == null) throw new Exception("Player has no skin data");

        SessionAPI.SkinInfo skinInfo = SessionAPI.extractSkinInfoFromProperties(props);
        if (skinInfo == null) throw new Exception("Player has no custom skin");

        byte[] skinBytes = SessionAPI.downloadBytes(skinInfo.url());
        return validate(skinBytes, skinInfo.variant());
    }

    private static ImportedSkin validate(byte[] bytes, @Nullable String variantHint) throws Exception {
        BufferedImage img;
        try {
            img = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            img = null;
        }
        if (img == null) throw new Exception("That isn't a PNG image");
        if (img.getWidth() != 64 || (img.getHeight() != 64 && img.getHeight() != 32)) {
            throw new Exception("Skin must be 64x64 (found " + img.getWidth() + "x" + img.getHeight() + ")");
        }
        return new ImportedSkin(bytes, variantHint != null ? variantHint : (detectSlim(img) ? "slim" : "classic"));
    }

    private static boolean detectSlim(BufferedImage img) {
        if (img.getHeight() < 64) return false;
        for (int y = 20; y < 32; y++) {
            for (int x = 54; x < 56; x++) {
                if (((img.getRGB(x, y) >>> 24) & 0xFF) != 0) return false;
            }
        }
        return true;
    }
}