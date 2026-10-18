package wtf.dupers.dupersunited.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.scoreboard.Scoreboard;
//import net.minecraft.scoreboard.ScoreboardObjective;
//import net.minecraft.scoreboard.ScoreboardDisplaySlot;
//import net.minecraft.scoreboard.Team;
//import net.minecraft.text.Text;

//import java.util.List;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static wtf.dupers.dupersunited.SharedVariables.*;


public class ServerUtils {
    /*
   There's for sure a better place to put all of this (I was considering putting this in SharedVariables instead of ServerUtils originally though)
   just lmk
     */
    public static volatile ServerData pendingServer;
    public static final String UNKNOWNPACKETSYO = "unknown (server doesn't report it)";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path VERSIONS_FILE = DIRECTORY.resolve("server_versions.json");
    private static final Map<String, String> SERVERVERSIONS = new ConcurrentHashMap<>();

    /*public static List<String> getScoreboardLines() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world == null) return List.of();

        Scoreboard scoreboard = mc.world.getScoreboard();
        if (scoreboard == null) return List.of();

        ScoreboardObjective obj =
                scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        if (obj == null) return List.of();

        return scoreboard.getScoreboardEntries(obj).stream()
                .map(entry ->
                        Team.decorateName(
                                scoreboard.getScoreHolderTeam(entry.owner()),
                                Text.literal(entry.owner())
                        ).getString()
                )
                .toList();
    }
    public static String stripFormatting(String s) {
        return s.replaceAll("§.", "");
    }*/

    public static boolean isDonut() {
        Minecraft mc = Minecraft.getInstance();

        var serverInfo = mc.getCurrentServer();
        if (serverInfo == null || serverInfo.ip == null) {
            return false;
        }

        return serverInfo.ip.toLowerCase(Locale.ROOT).contains("donutsmp.net");
    }

    static {
        loadRealServerVersions();
    }

    public static void setRealServerVersion(String address, String version) {
        if (address == null || version == null) return;
        if (UNKNOWNPACKETSYO.equals(version) && SERVERVERSIONS.containsKey(address)) return;

        String previous = SERVERVERSIONS.put(address, version);
        if (!version.equals(previous)) {
            IO_EXECUTOR.execute(ServerUtils::saveRealServerVersions);
        }
    }

    public static String getRealServerVersion(String address) {
        if (address == null) return null;
        return SERVERVERSIONS.get(address);
    }

    private static void loadRealServerVersions() {
        if (!Files.exists(VERSIONS_FILE)) return;
        try (Reader reader = Files.newBufferedReader(VERSIONS_FILE)) {
            Map<String, String> loaded = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            if (loaded != null) SERVERVERSIONS.putAll(loaded);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized void saveRealServerVersions() {
        try {
            Files.createDirectories(DIRECTORY);
            try (Writer writer = Files.newBufferedWriter(VERSIONS_FILE)) {
                GSON.toJson(new HashMap<>(SERVERVERSIONS), writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
