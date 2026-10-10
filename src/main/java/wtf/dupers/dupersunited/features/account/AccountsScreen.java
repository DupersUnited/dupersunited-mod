package wtf.dupers.dupersunited.features.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.util.UndashedUuid;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import wtf.dupers.dupersunited.compat.MeteorCompat;
import wtf.dupers.dupersunited.features.cosmetics.CapeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.User;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import wtf.dupers.dupersunited.features.proxies.*;
import wtf.dupers.dupersunited.features.screens.ui.DuScreen;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.features.screens.ui.Theme.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AccountsScreen extends DuScreen {
    public static final List<AccountEntry> ACCOUNTS = new ArrayList<>();
    private static boolean preloaded = false;

    private final List<AccountEntry> filteredAccounts = new ArrayList<>();
    private int scrollOffset = 0;
    private EditBox searchField;
    private EditBox pathField;
    private Component statusMessage = Component.empty();

    private static final Map<String, String> HEAD_SKINS = new HashMap<>();
    private static final Map<String, UUID> HEAD_UUIDS = new HashMap<>();
    private static final Set<String> HEAD_FETCHING = new HashSet<>();
    private static final Map<String, Long> HEAD_RETRY_AT = new HashMap<>();

    private static final int ROW_HEIGHT = 24;
    private static final int START_Y = 74;
    private static final int MAX_VISIBLE = 16;

    public AccountsScreen(Screen parent) {
        super(Component.literal("Accounts"), parent, "Accounts");
        applyFilter();
    }

    public static void preloadAccounts() {
        if (!preloaded) {
            preloaded = true;
            loadAccounts(null);
        }
    }

    public static void loadAccounts(@Nullable Runnable callback) {
        CompletableFuture.runAsync(() -> {
            Map<String, AccountEntry> accountsToAdd = new LinkedHashMap<>();

            // current user
            if (SessionManager.isSessionValid) {
                User session = SessionManager.getSession();
                accountsToAdd.putIfAbsent(session.getName(), new AccountEntry(session.getName(), session.getAccessToken(), "Current"));
            }

            String os = System.getProperty("os.name", "").toLowerCase();
            List<LauncherSource> sources = new ArrayList<>();

            // prism
            File prism = resolvePath(os, "PrismLauncher");
            if (prism != null && prism.exists()) sources.add(new LauncherSource("Prism", prism));

            // multimc
            File multimc = resolvePath(os, "MultiMC");
            if (multimc != null && multimc.exists()) sources.add(new LauncherSource("MultiMC", multimc));

            // custom paths from config (fuck you vinzy!)
            for (String path : ProxyConfigManager.customAccountPaths) {
                File customFile = new File(path);
                if (customFile.exists()) {
                    sources.add(new LauncherSource("Custom", customFile));
                }
            }

            for (LauncherSource src : sources) {
                try (FileReader reader = new FileReader(src.file)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray arr = root.getAsJsonArray("accounts");
                    if (arr == null) continue;

                    for (var el : arr) {
                        JsonObject acc = el.getAsJsonObject();
                        JsonObject profile = acc.has("profile") ? acc.getAsJsonObject("profile") : null;
                        JsonObject ygg = acc.has("ygg") ? acc.getAsJsonObject("ygg") : null;

                        String name = (profile != null && profile.has("name"))
                            ? profile.get("name").getAsString() : "Unknown";
                        String token = (ygg != null && ygg.has("token"))
                            ? ygg.get("token").getAsString() : null;

                        if (token != null && !token.isEmpty()) {
                            accountsToAdd.putIfAbsent(name, new AccountEntry(name, token, src.name));
                        }
                    }
                } catch (Exception ignored) {}
            }

            // meteor
            for (AccountEntry entry : MeteorCompat.getAccounts()) {
                accountsToAdd.putIfAbsent(entry.name, entry);
            }

            // offline accounts
            for (OfflineAccountManager.OfflineAccount offline : OfflineAccountManager.accounts) {
                accountsToAdd.putIfAbsent(offline.username, new AccountEntry(offline.username, offline.uuid, "Offline", true));
            }

            Minecraft.getInstance().execute(() -> {
                ACCOUNTS.clear();
                ACCOUNTS.addAll(accountsToAdd.values());

                List<String> order = AccountProxyLinks.accountOrder;
                if (order != null && !order.isEmpty()) {
                    ACCOUNTS.sort(Comparator.comparingInt(a -> orderIndex(order, a.name)));
                }

                if (callback != null) {
                    callback.run();
                } else if (Minecraft.getInstance().gui.screen() instanceof AccountsScreen accountsScreen) {
                    accountsScreen.applyFilter();
                    accountsScreen.rebuildList();
                }
            });
        });
    }

    private static int orderIndex(List<String> order, String name) {
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).equalsIgnoreCase(name)) return i;
        }
        return Integer.MAX_VALUE;
    }

    private static File resolvePath(String os, String launcherName) {
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return appData != null ? new File(appData, launcherName + "/accounts.json") : null;
        } else if (os.contains("mac")) {
            return new File(System.getProperty("user.home"), "Library/Application Support/" + launcherName + "/accounts.json");
        } else {
            return new File(System.getProperty("user.home"), ".local/share/" + launcherName + "/accounts.json");
        }
    }

    private int visibleRows() {
        return Math.clamp((this.height - 100) / ROW_HEIGHT, 1, MAX_VISIBLE);
    }

    private void applyFilter() {
        filteredAccounts.clear();
        String query = (searchField != null) ? searchField.getValue().toLowerCase() : "";
        for (AccountEntry e : ACCOUNTS) {
            if (query.isEmpty() || e.name.toLowerCase().contains(query) || e.source.toLowerCase().contains(query)) {
                filteredAccounts.add(e);
            }
        }
        scrollOffset = Math.clamp(filteredAccounts.size() - visibleRows(), 0, scrollOffset);
    }

    @Override
    protected void init() {
        int searchWidth = Math.min(200, this.width - 20);
        searchField = new EditBox(
            this.font,
            this.width / 2 - searchWidth / 2, 50,
            searchWidth, 16,
            Component.literal("Search")
        );
        searchField.setMaxLength(100);
        searchField.setHint(Component.literal("search accounts...").withStyle(ChatFormatting.DARK_GRAY));
        searchField.setResponder(_ -> {
            applyFilter();
            rebuildList();
        });
        this.addRenderableWidget(searchField);

        pathField = new EditBox(this.font, 10, this.height - 40, 150, 16, Component.literal("Path"));
        pathField.setMaxLength(255);
        pathField.setHint(Component.literal("Input the path").withStyle(ChatFormatting.WHITE));
        this.addRenderableWidget(pathField);

        rebuildList();
    }

    private void rebuildList() {
        this.clearWidgets();
        this.addRenderableWidget(searchField);
        this.addRenderableWidget(pathField);

        this.addRenderableWidget(Button.builder(
            Component.literal("SSID"),
            _ -> Minecraft.getInstance().gui.setScreen(new AddAccountScreen(this, false))
        ).bounds(5, 30, 55, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Offline"),
            _ -> Minecraft.getInstance().gui.setScreen(new AddAccountScreen(this, true))
        ).bounds(65, 30, 60, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Refresh"),
            _ -> loadAccounts(() -> {
                applyFilter();
                this.rebuildList();
            })
        ).bounds(this.width - 70, 30, 60, 18).build());

        if (SessionManager.originalSession != null) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("Restore Original"),
                    _ -> {
                        SessionManager.restoreSession();
                        statusMessage = Component.literal("Restored original session: " + SessionManager.getUsername()).withStyle(ChatFormatting.GREEN);
                        rebuildList();
                    }
                ).bounds(130, 30, 100, 20)
                .tooltip(Tooltip.create(
                    Component.literal("Restores the account you originally launched the game with")
                )).build());
        }

        int visibleRows = visibleRows();

        for (int i = 0; i < visibleRows && (i + scrollOffset) < filteredAccounts.size(); i++) {
            int index = i + scrollOffset;
            AccountEntry entry = filteredAccounts.get(index);
            int y = START_Y + i * ROW_HEIGHT;

            if (!entry.isOffline()) {
                this.addRenderableWidget(Button.builder(
                        Component.literal("Skin"),
                        _ -> mc.gui.setScreen(new ChangeSkinScreen(this, entry))
                    ).bounds(this.width - 305, y, 50, 20)
                    .tooltip(Tooltip.create(
                        Component.literal("Change this account's skin")
                    )).build());
            }

            this.addRenderableWidget(Button.builder(
                Component.literal("Login"),
                btn -> {
                    if (entry.isOffline()) {
                        attemptOfflineLogin(entry);
                    } else {
                        attemptLogin(entry, btn);
                    }
                }
            ).bounds(this.width - 250, y, 50, 20).build());

            boolean hasLink = AccountProxyLinks.hasLink(entry.name);
            this.addRenderableWidget(Button.builder(
                    Component.literal("Proxy").withStyle(hasLink ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    _ -> mc.gui.setScreen(new LinkProxyScreen(this, entry.name))
                ).bounds(this.width - 195, y, 50, 20)
                .tooltip(Tooltip.create(
                    Component.literal(hasLink
                        ? "Linked: " + AccountProxyLinks.getLinkedProxy(entry.name) + "\nClick to change"
                        : "No proxy linked\nClick to link one")
                )).build());

            boolean hasBypass = AccountProxyLinks.hasBypass(entry.name);
            this.addRenderableWidget(Button.builder(
                    Component.literal("Bypass").withStyle(hasBypass ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    _ -> {
                        AccountProxyLinks.toggleBypass(entry.name);
                        rebuildList();
                    }
                ).bounds(this.width - 140, y, 50, 20)
                .tooltip(Tooltip.create(
                    Component.literal(hasBypass
                        ? "Proxy bypass is currently enabled, you can connect without proxy\nClick to disable"
                        : "Proxy bypass is currently disabled\nClick to allow connecting without a proxy")
                )).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("▲"),
                    _ -> moveEntry(entry, -1)
                ).bounds(this.width - 85, y, 20, 20)
                .tooltip(Tooltip.create(
                    Component.literal("Move up")
                )).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("▼"),
                    _ -> moveEntry(entry, 1)
                ).bounds(this.width - 60, y, 20, 20)
                .tooltip(Tooltip.create(
                    Component.literal("Move down")
                )).build());

            if (entry.isOffline() && OfflineAccountManager.exists(entry.name)) {
                this.addRenderableWidget(Button.builder(
                        Component.literal("X").withStyle(ChatFormatting.RED),
                        _ -> {
                            OfflineAccountManager.delete(entry.name);
                            AccountProxyLinks.unlink(entry.name);
                            loadAccounts(() -> {
                                applyFilter();
                                this.rebuildList();
                            });
                        }
                    ).bounds(this.width - 35, y, 20, 20)
                    .tooltip(Tooltip.create(
                        Component.literal("Deletes this offline account")
                    )).build());
            }
        }

        this.addRenderableWidget(Button.builder(Component.literal("Confirm"), _ -> {
            String path = pathField.getValue();
            if (!path.isEmpty()) {
                ProxyConfigManager.customAccountPaths.add(path);
                ProxyConfigManager.save();
                loadAccounts(() -> {
                    pathField.setValue("");
                    applyFilter();
                    this.rebuildList();
                });
            }
        }).bounds(165, this.height - 41, 60, 18).build());
    }

    private void moveEntry(AccountEntry entry, int dir) {
        int i = ACCOUNTS.indexOf(entry);
        int j = i + dir;
        if (i < 0 || j < 0 || j >= ACCOUNTS.size()) return;
        Collections.swap(ACCOUNTS, i, j);
        List<String> order = new ArrayList<>();
        for (AccountEntry e : ACCOUNTS) order.add(e.name);
        AccountProxyLinks.saveOrder(order);
        applyFilter();
        rebuildList();
    }

    static void setHeadSkin(String name, String url) {
        HEAD_SKINS.put(name, url);
        HEAD_RETRY_AT.remove(name);
    }

    static void refreshHead(String name) {
        HEAD_SKINS.remove(name);
        HEAD_RETRY_AT.remove(name);
    }

    void addSsidAccount(String name, String token, String uuidString) {
        ACCOUNTS.removeIf(e -> e.name.equals(name) && e.source.equals("SSID"));
        ACCOUNTS.add(new AccountEntry(name, token, "SSID"));
        try {
            HEAD_UUIDS.put(name, UndashedUuid.fromString(uuidString));
        } catch (IllegalArgumentException ignored) {}
        applyFilter();
        rebuildList();
    }

    void addOfflineAccount(String name, Runnable done) {
        OfflineAccountManager.create(name);
        refreshAccounts(done);
    }

    void refreshAccounts(Runnable done) {
        loadAccounts(() -> {
            applyFilter();
            rebuildList();
            done.run();
        });
    }

    private void attemptLogin(AccountEntry entry, Button btn) {
        statusMessage = Component.literal("Checking token...").withStyle(ChatFormatting.YELLOW);
        btn.active = false;

        Thread.ofVirtual().start(() -> {
            try {
                String linkedProxy = AccountProxyLinks.getLinkedProxy(entry.name);
                if (linkedProxy != null) {
                    ProxyConfigManager.activeProfileName = linkedProxy;
                    ProxyConfigManager.globalEnabled = true;
                } else {
                    ProxyConfigManager.globalEnabled = false;
                    ProxyConfigManager.activeProfileName = "";
                }
                ProxyConfigManager.save();

                String[] info = SessionAPI.getProfileInfo(entry.token);
                if (info == null) throw new Exception("Invalid token");
                SessionManager.setSession(
                    SessionManager.createSession(info[0], info[1], entry.token)
                );
                mc.execute(() -> {
                    statusMessage = Component.literal("Logged in as: " + info[0]).withStyle(ChatFormatting.GREEN);
                    btn.active = true;
                });
            } catch (Exception e) {
                mc.execute(() -> {
                    statusMessage = Component.literal("Invalid token for " + entry.name).withStyle(ChatFormatting.RED);
                    btn.active = true;
                });
            }
        });
    }

    private void attemptOfflineLogin(AccountEntry entry) {
        String linkedProxy = AccountProxyLinks.getLinkedProxy(entry.name);

        if (linkedProxy != null && !linkedProxy.isEmpty()) {
            ProxyConfigManager.activeProfileName = linkedProxy;
            ProxyConfigManager.globalEnabled = true;
        } else {
            ProxyConfigManager.globalEnabled = false;
            ProxyConfigManager.activeProfileName = "";
        }
        ProxyConfigManager.save();

        SessionManager.setSession(
            SessionManager.createSession(entry.name, entry.token, "")
        );
        statusMessage = Component.literal("Logged in as: " + entry.name)
            .withStyle(ChatFormatting.GREEN)
            .append(Component.literal(" (offline)").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredAccounts.size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        rebuildList();
        return true;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawStructure(graphics, mouseX, mouseY);

        graphics.centeredText(
            this.font,
            Component.literal("Account Manager").withStyle(ChatFormatting.BOLD),
            this.width / 2,
            30,
            secondary
        );

        if (!ACCOUNTS.isEmpty()) {
            String sub = filteredAccounts.size() + "/" + ACCOUNTS.size() + " accounts";
            graphics.centeredText(
                this.font,
                Component.literal(sub),
                this.width / 2,
                40,
                dim
            );
        }

        if (!statusMessage.getString().isEmpty()) {
            graphics.centeredText(
                this.font,
                statusMessage,
                this.width / 2,
                this.height - 42,
                text
            );
        }

        if (filteredAccounts.isEmpty()) {
            String msg = ACCOUNTS.isEmpty()
                ? "no accounts found"
                : "no matches for \"" + searchField.getValue() + "\"";
            graphics.centeredText(
                this.font,
                Component.literal(msg),
                this.width / 2,
                this.height / 2,
                bad
            );
    } else {
        int visibleRows = visibleRows();
        for (int i = 0; i < visibleRows && (i + scrollOffset) < filteredAccounts.size(); i++) {
            int index = i + scrollOffset;
            AccountEntry entry = filteredAccounts.get(index);
            int y = START_Y + i * ROW_HEIGHT;

            int rowBg = (index % 2 == 0) ? border : header;
            graphics.fill(5, y - 2, this.width - 310, y + 22, rowBg);

            boolean hasLink = AccountProxyLinks.hasLink(entry.name);
            graphics.fill(5, y - 2, 7, y + 22, hasLink ? good : edge);

            drawHead(graphics, entry, 9, y + 1);

            boolean isLoggedIn = SessionManager.getUsername().equals(entry.name);
            MutableComponent usernameText = Component.literal(entry.name);
            if (isLoggedIn) usernameText.withStyle(ChatFormatting.BOLD);

            graphics.text(
                this.font,
                usernameText,
                29,
                y + 3,
                isLoggedIn ? value : text,
                true
            );
            graphics.text(
                this.font,
                Component.literal("[" + entry.source + "]"),
                29,
                y + 13,
                dim,
                true
            );
        }

            if (filteredAccounts.size() > visibleRows) {
                int totalHeight = visibleRows * ROW_HEIGHT;
                int barHeight = Math.max(10, totalHeight * visibleRows / filteredAccounts.size());
                int maxScroll = filteredAccounts.size() - visibleRows;
                int barY = START_Y + (totalHeight - barHeight) * scrollOffset / Math.max(1, maxScroll);
                graphics.fill(this.width - 4, START_Y, this.width - 1, START_Y + totalHeight, border);
                graphics.fill(this.width - 4, barY, this.width - 1, barY + barHeight, secondary);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawHead(GuiGraphicsExtractor context, AccountEntry entry, int x, int y) {
        Identifier texture = headTexture(entry);
        if (texture == null) {
            context.fill(x, y, x + 16, y + 16, header);
            context.fill(x, y, x + 16, y + 1, edge);
            context.fill(x, y + 15, x + 16, y + 16, edge);
            context.fill(x, y, x + 1, y + 16, edge);
            context.fill(x + 15, y, x + 16, y + 16, edge);
            return;
        }
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8, 8, 16, 16, 8, 8, 64, 64);
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 40, 8, 16, 16, 8, 8, 64, 64);
    }

    @Nullable
    private Identifier headTexture(AccountEntry entry) {
        String url = HEAD_SKINS.get(entry.name);
        if (url == null) {
            resolveHead(entry);
            return null;
        }
        return CapeManager.getTextureOrLoad(url);
    }

    @Nullable
    private UUID headUuid(AccountEntry entry) {
        UUID uuid = HEAD_UUIDS.get(entry.name);
        if (uuid != null) return uuid;
        User session = SessionManager.getSession();
        if (entry.name.equals(session.getName()))
            return session.getProfileId();
        return null;
    }

    private void resolveHead(AccountEntry entry) {
        if (entry.isOffline() || !HEAD_FETCHING.add(entry.name)) return;
        if (System.currentTimeMillis() < HEAD_RETRY_AT.getOrDefault(entry.name, 0L)) {
            HEAD_FETCHING.remove(entry.name);
            return;
        }
        Thread.ofVirtual().start(() -> {
            try {
                UUID uuid = headUuid(entry);
                if (uuid == null) {
                    String[] info = SessionAPI.getProfileInfo(entry.token);
                    if (info != null) {
                        try {
                            uuid = UndashedUuid.fromString(info[1]);
                            HEAD_UUIDS.put(entry.name, uuid);
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                if (uuid == null) throw new IllegalStateException("no uuid");

                String skinUrl = fetchSkinUrl(uuid);
                if (skinUrl == null) throw new IllegalStateException("no skin");
                HEAD_SKINS.put(entry.name, skinUrl);
                HEAD_RETRY_AT.remove(entry.name);
            } catch (Exception ignored) {
                HEAD_RETRY_AT.put(entry.name, System.currentTimeMillis() + 30_000L);
            }
            HEAD_FETCHING.remove(entry.name);
        });
    }

    @Nullable
    private static String fetchSkinUrl(UUID uuid) {
        try {
            String json = SessionAPI.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false");
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray properties = root.has("properties") ? root.getAsJsonArray("properties") : null;
            if (properties == null) return null;

            SessionAPI.SkinInfo skinInfo = SessionAPI.extractSkinInfoFromProperties(properties);
            return skinInfo != null ? skinInfo.url() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public record AccountEntry(String name, String token, String source, boolean isOffline) {
        public AccountEntry(String name, String token, String source) {
            this(name, token, source, false);
        }
    }
    private record LauncherSource(String name, File file) {}
}