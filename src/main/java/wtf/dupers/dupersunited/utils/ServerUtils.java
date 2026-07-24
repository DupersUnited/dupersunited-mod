package wtf.dupers.dupersunited.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.List;


public class ServerUtils {
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
        MinecraftClient mc = MinecraftClient.getInstance();

        var serverInfo = mc.getCurrentServerEntry();
        if (serverInfo == null || serverInfo.address == null) {
            return false;
        }

        return serverInfo.address.toLowerCase().contains("donutsmp.net");
    }
}
