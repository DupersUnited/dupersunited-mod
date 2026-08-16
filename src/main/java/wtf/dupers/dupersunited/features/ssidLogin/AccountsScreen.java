package wtf.dupers.dupersunited.features.ssidLogin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.tooltip.Tooltip;
import wtf.dupers.dupersunited.compat.MeteorCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.session.Session;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import wtf.dupers.dupersunited.features.OfflineAccountManager;
import wtf.dupers.dupersunited.features.proxies.*;
import static wtf.dupers.dupersunited.utils.ColorUtil.*;
import org.jetbrains.annotations.Nullable;
import wtf.dupers.dupersunited.features.proxies.AccountProxyLinks;
import wtf.dupers.dupersunited.features.proxies.LinkProxyScreen;
import wtf.dupers.dupersunited.features.proxies.ProxyConfigManager;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AccountsScreen extends Screen {
    public static final List<AccountEntry> ACCOUNTS = new ArrayList<>();
    private static boolean preloaded = false;

    private final Screen parent;
    private final List<AccountEntry> filteredAccounts = new ArrayList<>();
    private int scrollOffset = 0;
    private TextFieldWidget searchField;
    private TextFieldWidget pathField;
    private Text statusMessage = Text.empty();


    private static final int ROW_HEIGHT  = 24;
    private static final int START_Y     = 60;
    private static final int MAX_VISIBLE = 16;

    public AccountsScreen(Screen parent) {
        super(Text.literal("Accounts"));
        this.parent = parent;
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
            if (SessionManager.isSessionValid()) {
                Session session = SessionManager.getSession();
                accountsToAdd.putIfAbsent(session.getUsername(), new AccountEntry(session.getUsername(), session.getAccessToken(), "Current"));
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

                    for (JsonElement el : arr) {
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

            MinecraftClient.getInstance().execute(() -> {
                ACCOUNTS.clear();
                ACCOUNTS.addAll(accountsToAdd.values());

                if (callback != null) {
                    callback.run();
                } else if (MinecraftClient.getInstance().currentScreen instanceof AccountsScreen accountsScreen) {
                    accountsScreen.applyFilter();
                    accountsScreen.rebuildList();
                }
            });
        });
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
        return Math.min(MAX_VISIBLE, Math.max(1, (this.height - 100) / ROW_HEIGHT));
    }

    private void applyFilter() {
        filteredAccounts.clear();
        String query = (searchField != null) ? searchField.getText().toLowerCase() : "";
        for (AccountEntry e : ACCOUNTS) {
            if (query.isEmpty() || e.name.toLowerCase().contains(query) || e.source.toLowerCase().contains(query)) {
                filteredAccounts.add(e);
            }
        }
        // favorites float to the top (obv)
        filteredAccounts.sort((a, b) -> {
            boolean af = AccountProxyLinks.isFavorite(a.name);
            boolean bf = AccountProxyLinks.isFavorite(b.name);
            return Boolean.compare(bf, af);
        });
        scrollOffset = Math.min(scrollOffset, Math.max(0, filteredAccounts.size() - visibleRows()));
    }

    @Override
    protected void init() {
        int searchWidth = Math.min(200, this.width - 20);
        searchField = new TextFieldWidget(
                this.textRenderer,
                this.width / 2 - searchWidth / 2, 36,
                searchWidth, 16,
                Text.literal("Search")
        );
        searchField.setMaxLength(100);
        searchField.setPlaceholder(Text.literal("search accounts...").formatted(Formatting.DARK_GRAY));
        this.addSelectableChild(searchField);

        // path field for custom accounts.json (yw blendy and fuck you vinzy v2)
        pathField = new TextFieldWidget(this.textRenderer, 10, this.height - 55, 150, 16, Text.literal("Path"));
        pathField.setMaxLength(255);
        pathField.setPlaceholder(Text.literal("Input the path").formatted(Formatting.WHITE));
        this.addSelectableChild(pathField);
        rebuildList();
    }

    private void rebuildList() {
        this.clearChildren();
        this.addSelectableChild(searchField);
        this.addSelectableChild(pathField);

        // offline qaccs button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Offline Manager"),
            btn -> client.setScreen(new OfflineAccountScreen(this))
        ).dimensions(5, 8, 100, 20).build());

        int visibleRows = visibleRows();

        for (int i = 0; i < visibleRows && (i + scrollOffset) < filteredAccounts.size(); i++) {
            int index = i + scrollOffset;
            AccountEntry entry = filteredAccounts.get(index);
            int y = START_Y + i * ROW_HEIGHT;

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Login"),
                    btn -> {
                        if (entry.isOffline()) {
                            attemptOfflineLogin(entry, btn);
                        } else {
                            attemptLogin(entry, btn);
                        }
                    }
            ).dimensions(this.width - 225, y, 50, 20).build());

            // proxy link button
            boolean hasLink = AccountProxyLinks.hasLink(entry.name);
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal(hasLink ? "§aProxy" : "§7Proxy"),
                            btn -> client.setScreen(new LinkProxyScreen(this, entry.name))
                ).dimensions(this.width - 170, y, 50, 20)
                    .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                            Text.literal(hasLink
                                    ? "Linked: " + AccountProxyLinks.getLinkedProxy(entry.name) + "\nClick to change"
                                    : "No proxy linked\nClick to link one")
                    )).build());

            // bypass toggle button (alwos connecting without a proxy for current accc)
            boolean hasBypass = AccountProxyLinks.hasBypass(entry.name);
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal(hasBypass ? "§aBypass" : "§7Bypass"),
                            btn -> {
                                AccountProxyLinks.toggleBypass(entry.name);
                                rebuildList();
                            }
                    ).dimensions(this.width - 115, y, 50, 20)
                    .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                            Text.literal(hasBypass
                                    ? "Proxy bypass is currently enabled, you can connect without proxy\nClick to disable"
                                    : "Proxy bypass is currently disabled\nClick to allow connecting without a proxy")
                    )).build());

            // favorite toggle button
            boolean isFav = AccountProxyLinks.isFavorite(entry.name);
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(isFav ? "§e★" : "§7☆"),
                    btn -> {
                        AccountProxyLinks.toggleFavorite(entry.name);
                        applyFilter();
                        rebuildList();
                    }
                ).dimensions(this.width - 60, y, 20, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                    Text.literal(isFav ? "Unfavourite account" : "Favourite account (pins to top)")
                )).build());

            // delete button (for offline accounts only)
            if (entry.isOffline() && OfflineAccountManager.exists(entry.name)) {
                this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("§cX"),
                        btn -> {
                            OfflineAccountManager.delete(entry.name);
                            AccountProxyLinks.unlink(entry.name);
                            loadAccounts(() -> {
                                applyFilter();
                                this.rebuildList();
                            });
                        }
                    ).dimensions(this.width - 35, y, 20, 20)
                    .tooltip(Tooltip.of(
                        Text.literal("Deletes this offline account")
                    )).build());
            }
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), btn -> {
            String path = pathField.getText();
            if (!path.isEmpty()) {
                ProxyConfigManager.customAccountPaths.add(path);
                ProxyConfigManager.save();
                loadAccounts(() -> {
                    pathField.setText("");
                    applyFilter();
                    this.rebuildList();
                });
            }
        }).dimensions(165, this.height - 56, 60, 18).build());

        // back button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"),
                btn -> MinecraftClient.getInstance().setScreen(parent)
        ).dimensions(this.width / 2 - 50, this.height - 28, 100, 20).build());

        // refresh button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Refresh"),
            btn -> loadAccounts(() -> {
                applyFilter();
                this.rebuildList();
            })
        ).dimensions(this.width - 70, this.height - 56, 60, 18).build());
    }

    private void attemptLogin(AccountEntry entry, ButtonWidget btn) {
        statusMessage = Text.literal("Checking token...").formatted(Formatting.YELLOW);
        btn.active = false;

        Thread.ofVirtual().start(() -> {
            try {
                // applying linked proxy before logging in !!
                String linkedProxy = AccountProxyLinks.getLinkedProxy(entry.name);
                if (linkedProxy != null) {
                    ProxyConfigManager.activeProfileName = linkedProxy;
                    ProxyConfigManager.globalEnabled = true;
                    ProxyConfigManager.save();
                } else {
                    // no linked proxy then disable proxy
                    ProxyConfigManager.globalEnabled = false;
                    ProxyConfigManager.activeProfileName = "";
                    ProxyConfigManager.save();
                }

                String[] info = SessionAPI.getProfileInfo(entry.token);
                SessionManager.setSession(
                        SessionManager.createSession(info[0], info[1], entry.token)
                );
                MinecraftClient.getInstance().execute(() -> {
                    statusMessage = Text.literal("Logged in as: " + info[0]).formatted(Formatting.GREEN);
                    btn.active = true;
                });
            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() -> {
                    statusMessage = Text.literal("Invalid token for " + entry.name).formatted(Formatting.RED);
                    btn.active = true;
                });
            }
        });
    }

    private void attemptOfflineLogin(AccountEntry entry, ButtonWidget btn) {
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
        statusMessage = Text.literal("Logged in as: " + entry.name + " §7(offline)").formatted(Formatting.GREEN);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredAccounts.size() - visibleRows());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        rebuildList();
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (searchField != null && searchField.keyPressed(input)) {
            applyFilter();
            scrollOffset = 0;
            rebuildList();
            return true;
        }
        if (pathField != null && pathField.keyPressed(input)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (searchField != null && searchField.charTyped(input)) {
            applyFilter();
            scrollOffset = 0;
            rebuildList();
            return true;
        }
        if (pathField != null && pathField.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // base background
        context.fill(0, 0, this.width, this.height, DEEP_SAPPHIRE);

        // top header bar
        context.fill(0, 0, this.width, 32, MANTLE);
        context.fill(0, 32, this.width, 33, FADED_INDIGO);

        // bottom footer bar
        context.fill(0, this.height - 36, this.width, this.height, MANTLE);
        context.fill(0, this.height - 37, this.width, this.height - 36, FADED_INDIGO);

        super.render(context, mouseX, mouseY, delta);
        searchField.render(context, mouseX, mouseY, delta);
        pathField.render(context, mouseX, mouseY, delta);

        // title
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Account Manager").formatted(Formatting.BOLD),
                this.width / 2, 12, LAVENDER);

        // account count subtitle
        if (!ACCOUNTS.isEmpty()) {
            String sub = filteredAccounts.size() + "/" + ACCOUNTS.size() + " accounts";
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(sub), this.width / 2, 22, FADED_NAVY);
        }

        // status message
        if (!statusMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    statusMessage, this.width / 2, this.height - 42, PALE_NAVY);
        }

        // empty state
        if (filteredAccounts.isEmpty()) {
            String msg = ACCOUNTS.isEmpty()
                    ? "no accounts found"
                    : "no matches for \"" + searchField.getText() + "\"";
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(msg), this.width / 2, this.height / 2, RED);
            return;
        }

        int visibleRows = visibleRows();
        for (int i = 0; i < visibleRows && (i + scrollOffset) < filteredAccounts.size(); i++) {
            int index = i + scrollOffset;
            AccountEntry entry = filteredAccounts.get(index);
            int y = START_Y + i * ROW_HEIGHT;

            // alternating row bg
            int rowBg = (index % 2 == 0) ? DEEP_INDIGO : MANTLE;
            context.fill(5, y - 2, this.width - 230, y + 22, rowBg);

            // left accent bar — green if has proxy link, surface1 otherwise
            boolean hasLink = AccountProxyLinks.hasLink(entry.name);
            context.fill(5, y - 2, 7, y + 22, hasLink ? GREEN : FADED_INDIGO);

            // account name
            boolean isLoggedIn = SessionManager.getUsername().equals(entry.name);
            MutableText usernameText = Text.literal(entry.name);
            if (isLoggedIn) usernameText.formatted(Formatting.BOLD);

            context.drawTextWithShadow(this.textRenderer,
                usernameText, 13, y + 3, isLoggedIn ? PEACH : PALE_NAVY);

            // source tag
            context.drawTextWithShadow(this.textRenderer,
                    Text.literal("[" + entry.source + "]"), 13, y + 13, FADED_NAVY);
        }

        // scrollbar
        if (filteredAccounts.size() > visibleRows) {
            int totalHeight = visibleRows * ROW_HEIGHT;
            int barHeight = Math.max(10, totalHeight * visibleRows / filteredAccounts.size());
            int maxScroll = filteredAccounts.size() - visibleRows;
            int barY = START_Y + (totalHeight - barHeight) * scrollOffset / Math.max(1, maxScroll);
            context.fill(this.width - 4, START_Y, this.width - 1, START_Y + totalHeight, DEEP_INDIGO);
            context.fill(this.width - 4, barY, this.width - 1, barY + barHeight, LAVENDER);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    public record AccountEntry(String name, String token, String source, boolean isOffline) {
        public AccountEntry(String name, String token, String source) {
            this(name, token, source, false);
        }
    }
    private record LauncherSource(String name, File file) {}
}