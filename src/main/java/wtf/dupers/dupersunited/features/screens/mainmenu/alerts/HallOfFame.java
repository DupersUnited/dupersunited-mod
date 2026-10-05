package wtf.dupers.dupersunited.features.screens.mainmenu.alerts;

import net.minecraft.ChatFormatting;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.SharedVariables;
import wtf.dupers.dupersunited.features.ServerAlertConfig;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static wtf.dupers.dupersunited.MainClient.mc;

public class HallOfFame {

    // code for this is god awful but it does the job so who gaf
    // also yeah i did just copy paste HallOfShame and change a couple things

    private static final String LIST = "https://raw.githubusercontent.com/DupersUnited/halloffame/refs/heads/main/cooldudes.json";
    private static Set<String> fameSet = new ObjectOpenHashSet<>();
    private static long lastFetched = 0L;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;

    public static CompletableFuture<Boolean> checkAsync(String serverAddress) {
        return CompletableFuture.supplyAsync(() -> {
            refreshIfStale();
            return lookup(serverAddress);
        });
    }

    public static boolean lookupCached(String serverAddress) {
        return lookup(serverAddress);
    }

    private static boolean lookup(String serverAddress) {
        if (fameSet.isEmpty()) return false;

        String host = normalise(serverAddress);

        if (fameSet.contains(host)) return true;

        for (String listed : fameSet) {
            if (host.endsWith("." + listed) || listed.endsWith("." + host)) {
                return true;
            }
        }

        return false;
    }

    private static String normalise(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).trim();
        int colon = s.lastIndexOf(':');
        if (colon != -1 && s.indexOf(':') == colon) {
            s = s.substring(0, colon);
        }
        return s;
    }

    private static synchronized void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (now - lastFetched < CACHE_TTL_MS) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(LIST).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "glitcha");

            if (conn.getResponseCode() != 200) return;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            Set<String> parsed = parse(reader);
            reader.close();

            if (!parsed.isEmpty()) {
                fameSet = parsed;
                lastFetched = now;
            }
        } catch (Exception e) {
            MainClient.LOGGER.error("HoF fetch failed", e);
        }
    }

    private static Set<String> parse(BufferedReader reader) throws Exception {
        Set<String> result = new ObjectOpenHashSet<>();
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line.trim());
        }

        String json = sb.toString();
        String[] entries = json.split("\\},\\s*\\{");

        for (String entry : entries) {
            entry = entry.replaceAll("[\\[\\]{}]", "").trim();
            String ip = extractJsonValue(entry, "ip");
            if (ip != null && !ip.isEmpty()) {
                result.add(ip.toLowerCase(Locale.ROOT).trim());
            }
        }

        return result;
    }

    private static String extractJsonValue(String fragment, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = fragment.indexOf(search);
        if (keyIdx == -1) return null;

        int colon = fragment.indexOf(':', keyIdx + search.length());
        if (colon == -1) return null;

        int open = fragment.indexOf('"', colon + 1);
        if (open == -1) return null;

        int close = fragment.indexOf('"', open + 1);
        if (close == -1) return null;

        return fragment.substring(open + 1, close);
    }

    public static void prefetch() {
        if (System.currentTimeMillis() - lastFetched >= CACHE_TTL_MS) {
            CompletableFuture.runAsync(HallOfFame::refreshIfStale, SharedVariables.IO_EXECUTOR);
        }
    }

    public static class NoticeScreen extends Screen {

        private final Screen parent;
        private final ServerData serverInfo;

        public NoticeScreen(Screen parent, ServerData serverInfo) {
            super(Component.literal("Hall of Fame Notice"));
            this.parent = parent;
            this.serverInfo = serverInfo;
        }

        @Override
        protected void init() {
            this.addRenderableWidget(Button.builder(Component.literal("Connect").withStyle(ChatFormatting.GREEN), btn -> {
                ServerAddress address = ServerAddress.parseString(serverInfo.ip);
                ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc, address, serverInfo, false, null);
            }).bounds(this.width / 2 - 155, this.height / 2 + 15, 150, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Back Out").withStyle(ChatFormatting.RED), btn ->
                mc.gui.setScreen(parent)
            ).bounds(this.width / 2 + 5, this.height / 2 + 15, 150, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Disable for this server"), btn -> {
                ServerAlertConfig.dismiss(serverInfo.ip);
                ServerAddress address = ServerAddress.parseString(serverInfo.ip);
                ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc, address, serverInfo, false, null);
            }).bounds(this.width / 2 - 75, this.height / 2 + 40, 150, 20).build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);

            graphics.centeredText(font,
                Component.literal("★ HALL OF FAME ★").withStyle(ChatFormatting.GREEN),
                this.width / 2, this.height / 2 - 50, 0xFFFFFFFF);

            graphics.centeredText(font,
                Component.literal(serverInfo.ip).withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.literal(" is a recognised as an ethical server by DupersUnited!").withStyle(ChatFormatting.RESET)),
                this.width / 2, this.height / 2 - 32, 0xFFFFFFFF);

            graphics.centeredText(font,
                Component.literal("Please remember to ").withStyle(ChatFormatting.RESET)
                    .append(Component.literal("NOT").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                    .append(Component.literal(" dupe on servers with ethical ways of making money, if you find an exploit here you should report it to them!").withStyle(ChatFormatting.RESET)),
                this.width / 2, this.height / 2 - 16, 0xFFFFFFFF);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}