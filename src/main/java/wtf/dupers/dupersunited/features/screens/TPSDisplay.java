package wtf.dupers.dupersunited.features.screens;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;

public class TPSDisplay {
    public static long lastPacketTime = -1;
    public static double tps = 20.0;

    public static void onWorldTimeUpdate() {
        long now = System.currentTimeMillis();
        if (lastPacketTime != -1) {
            long delta = Math.max(1, now - lastPacketTime);
            double instantTps = 20000.0 / (double) delta;
            tps = Mth.clamp((tps * 0.8) + (Math.min(20.0, instantTps) * 0.2), 0, 20);
        }
        lastPacketTime = now;
    }

    public static ChatFormatting getTpsColorCode(double tps) {
        if (tps < 12.0) return ChatFormatting.RED;
        if (tps < 17.0) return ChatFormatting.YELLOW;
        return ChatFormatting.GREEN;
    }

}
