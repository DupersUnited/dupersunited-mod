package wtf.dupers.dupersunited.features.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.User;
import wtf.dupers.dupersunited.features.cosmetics.CapeManager;
import wtf.dupers.dupersunited.features.ServerInviteManager;
import wtf.dupers.dupersunited.features.account.SessionAPI;
import wtf.dupers.dupersunited.features.account.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class AuthManager {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final String PUBLIC_WS_URL = "wss://dupersunited-server.dupers.wtf/ws/public";
    private static final String PUBLIC_API_BASE_URL = "https://dupersunited-server.dupers.wtf";
    private static final long RECONNECT_DELAY_MS = 3_000L;

    private static volatile String statusLine = UiText.CONNECTING_SOCKET.status;
    private static volatile String detailLine = UiText.CONNECTING_SOCKET.detail;

    private static volatile boolean initialized;
    private static volatile boolean socketConnected;
    private static volatile boolean accountVerified;
    private static volatile boolean socketConnecting;

    private static volatile int generation;
    private static volatile long reconnectAtMs = -1L;

    private static volatile WebSocket socket;
    private static volatile MinecraftAccount linkedAccount;
    private static volatile String linkToken;
    private static volatile long linkTokenExpiresAtMs;
    private static final CapeCatalog EMPTY_CAPE_CATALOG = new CapeCatalog(Map.of(), null);
    private static volatile CapeCatalog capeCatalog = EMPTY_CAPE_CATALOG;
    private static volatile String verifiedSessionFingerprint = "";
    private static volatile String requestedSessionFingerprint = "";

    public static void init() {
        if (initialized) return;

        initialized = true;
        resetAccountState();
        connectSocket();
    }

    public static void onTick() {
        if (!initialized) return;

        long reconnectAt = reconnectAtMs;
        if (reconnectAt > 0L && System.currentTimeMillis() >= reconnectAt) {
            reconnectAtMs = -1L;
            connectSocket();
        }

        refreshExpiredToken();
        detectSessionSwitch();
    }

    public static void onMinecraftAccountChanged() {
        if (!initialized) {
            return;
        }

        resetAccountState();
        syncCapeState();

        ensureSessionValidatedAndRetry(true);
    }

    public static boolean isSocketConnected() {
        return socketConnected;
    }

    public static boolean canUseCapes() {
        return accountVerified && linkedAccount != null;
    }

    public static String getStatusLine() {
        return statusLine;
    }

    public static String getDetailLine() {
        return detailLine;
    }

    public static MinecraftAccount getLinkedAccount() {
        return linkedAccount;
    }

    public static CapeCatalog getCapeCatalog() {
        return capeCatalog;
    }

    public static String getApiBaseUrl() {
        return PUBLIC_API_BASE_URL;
    }

    public static void retry() {
        init();

        if (!socketConnected && !socketConnecting) connectSocket();

        if (!accountVerified && ensureSessionValidatedAndRetry(false)) {
            retryAccountVerification();
            return;
        }

        if (canUseCapes()) requestCapeList();
    }

    public static void retryAccountVerification() {
        if (!ensureSessionValidatedAndRetry(false)) {
            setPhase(UiText.READY_UNSUPPORTED_SESSION);
            syncCapeState();
            return;
        }

        requestedSessionFingerprint = getSessionFingerprint();
        linkedAccount = null;
        linkToken = null;
        linkTokenExpiresAtMs = 0L;
        accountVerified = false;
        capeCatalog = EMPTY_CAPE_CATALOG;

        setPhase(UiText.WAITING_FOR_ACCOUNT_REQUEST);
        syncCapeState();

        String fingerprint = requestedSessionFingerprint;
        CompletableFuture.runAsync(() -> runLinkFlow(fingerprint));
    }

    public static void requestCapeList() {
        String token = linkToken;
        if (!canUseCapes() || token == null) return;

        CompletableFuture.runAsync(() -> {
            try {
                applyCapeCatalog(getJson("/api/capes", token));
            } catch (ApiException exception) {
                handleCapeFailure(token, exception);
            }
        });
    }

    public static void pickCape(String capeKey) {
        String token = linkToken;
        if (!canUseCapes() || token == null) return;

        JsonObject body = new JsonObject();
        if (capeKey == null || capeKey.isBlank() || "disabled".equalsIgnoreCase(capeKey)) {
            body.add("capeKey", JsonNull.INSTANCE);
        } else {
            body.addProperty("capeKey", capeKey);
        }

        CompletableFuture.runAsync(() -> {
            try {
                applyCapeCatalog(postJson("/api/capes/pick", body, token));
            } catch (ApiException exception) {
                handleCapeFailure(token, exception);
            }
        });
    }

    private static void runLinkFlow(String fingerprint) {
        try {
            JsonObject challenge = postJson("/api/link/challenge", new JsonObject(), null);
            String serverId = getNullableString(challenge, "serverId");
            if (serverId == null) throw new ApiException(500, UiText.MINECRAFT_VERIFY_FAILED, "Backend sent an empty serverId.");

            if (!fingerprint.equals(getSessionFingerprint())) return;

            setPhase(UiText.WAITING_FOR_ACCOUNT_REQUEST, "Joining Mojang session server with challenge " + serverId + "...");
            syncCapeState();

            joinServerSession(serverId);

            if (!fingerprint.equals(getSessionFingerprint())) return;

            setPhase(UiText.WAITING_FOR_ACCOUNT_CONFIRMATION);
            syncCapeState();

            User session = mc.getUser();
            JsonObject body = new JsonObject();
            body.addProperty("username", session.getName());
            body.addProperty("serverId", serverId);
            JsonObject verify = postJson("/api/link/verify", body, null);

            String uuid = getNullableString(verify, "uuid");
            String username = firstNonBlank(getNullableString(verify, "username"), session.getName());
            String token = getNullableString(verify, "linkToken");
            if (uuid == null || token == null) throw new ApiException(500, UiText.MINECRAFT_VERIFY_FAILED, "Backend sent an incomplete verify response.");

            linkedAccount = new MinecraftAccount(uuid, username);
            linkToken = token;
            linkTokenExpiresAtMs = parseExpiresAt(verify);
            accountVerified = true;
            verifiedSessionFingerprint = fingerprint;
            requestedSessionFingerprint = fingerprint;
            setPhase(UiText.ADDON_READY, "Linked Minecraft account: " + linkedAccount.username());
            applyCapeCatalog(getJson("/api/capes", token));
            syncCapeState();
        } catch (ApiException exception) {
            setPhase(exception.text, exception.getMessage());
            syncCapeState();
        }
    }

    private static void joinServerSession(String serverId) throws ApiException {
        User session = Minecraft.getInstance().getUser();
        UUID uuid = session.getProfileId();
        String accessToken = session.getAccessToken();
        if (accessToken.isBlank()) throw new ApiException(400, UiText.READY_UNSUPPORTED_SESSION, UiText.READY_UNSUPPORTED_SESSION.detail);

        try {
            mc.services().sessionService().joinServer(uuid, accessToken, serverId);
        } catch (AuthenticationException exception) {
            throw new ApiException(400, UiText.READY_LINK_FAILED, UiText.READY_LINK_FAILED.detail);
        }
    }

    private static void applyCapeCatalog(JsonObject catalog) {
        MinecraftAccount account = parseAccount(getChild(catalog, "account"));
        if (account != null) {
            linkedAccount = account;
            accountVerified = true;
        }
        capeCatalog = parseCapeCatalog(catalog);
        syncCapeState();
    }

    private static void handleCapeFailure(String token, ApiException exception) {
        if (exception.status == 401 && token != null && token.equals(linkToken)) {
            linkToken = null;
            linkTokenExpiresAtMs = 0L;
            linkedAccount = null;
            accountVerified = false;
            capeCatalog = EMPTY_CAPE_CATALOG;
            retryAccountVerification();
            return;
        }
        setPhase(UiText.BACKEND_REJECTED, exception.getMessage());
        syncCapeState();
    }

    private static JsonObject postJson(String path, JsonObject body, String token) throws ApiException {
        return requestJson("POST", path, body, token);
    }

    private static JsonObject getJson(String path, String token) throws ApiException {
        return requestJson("GET", path, null, token);
    }

    private static JsonObject requestJson(String method, String path, JsonObject body, String token) throws ApiException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(PUBLIC_API_BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json");
            if (token != null) builder.header("Authorization", "Bearer " + token);
            if ("POST".equals(method)) builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : GSON.toJson(body)));
            else builder.GET();

            HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                String error = null;
                try {
                    error = getNullableString(JsonParser.parseString(response.body()).getAsJsonObject(), "error");
                } catch (Exception ignored) { }
                throw new ApiException(response.statusCode(), UiText.BACKEND_REJECTED,
                    error != null ? error : "Request failed (" + response.statusCode() + ").");
            }
            try {
                return JsonParser.parseString(response.body()).getAsJsonObject();
            } catch (Exception ignored) {
                throw new ApiException(response.statusCode(), UiText.BACKEND_REJECTED, "Backend sent an unreadable response.");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, UiText.BACKEND_REJECTED, "Request interrupted.");
        } catch (Exception exception) {
            throw new ApiException(0, UiText.BACKEND_REJECTED, "Could not reach the backend: " + simplifyError(exception));
        }
    }

    private static final class ApiException extends Exception {
        private final int status;
        private final UiText text;

        private ApiException(int status, UiText text, String message) {
            super(message);
            this.status = status;
            this.text = text;
        }
    }

    private static void connectSocket() {
        if (socketConnected || socketConnecting) {
            return;
        }

        int gen = ++generation;

        closeSocket();
        socketConnected = false;
        socketConnecting = true;
        reconnectAtMs = -1L;

        setPhase(UiText.CONNECTING_SOCKET);
        syncCapeState();

        HTTP_CLIENT.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .buildAsync(URI.create(PUBLIC_WS_URL), new AddonSocketListener(gen))
            .exceptionally(throwable -> {
                handleSocketDisconnect(gen, null, "WebSocket connection failed: " + simplifyError(throwable));
                return null;
            });
    }

    private static void handleSocketOpen(int gen, WebSocket webSocket) {
        if (gen != generation) {
            webSocket.abort();
            return;
        }

        socket = webSocket;
        socketConnected = true;
        socketConnecting = false;
        reconnectAtMs = -1L;

        if (!accountVerified) {
            setPhase(UiText.READY_NO_ACCOUNT);
            syncCapeState();
        }
        ensureSessionValidatedAndRetry(true);
    }

    private static void handleSocketMessage(int gen, WebSocket webSocket, String rawMessage) {
        if (gen != generation || socket != webSocket) return;

        JsonObject root;
        try {
            root = JsonParser.parseString(rawMessage).getAsJsonObject();
        } catch (Exception ignored) {
            return;
        }

        String type = getNullableString(root, "type");
        if (type == null) return;

        switch (type) {
            case "broadcast" -> handleBroadcast(root);
            case "server_invite" -> handleServerInvite(root);
            case "error" -> {
                String message = Objects.requireNonNullElse(getNullableString(root, "message"), "Unknown backend error.");
                setPhase(UiText.BACKEND_REJECTED, message);
                syncCapeState();
            }
            default -> {
            }
        }
    }

    private static void handleBroadcast(JsonObject root) {
        String message = getNullableString(root, "msg");
        if (message == null) return;

        mc.execute(() -> {
            mc.gui.hud.getChat().addClientSystemMessage(
                Component.empty()
                    .append(Component.literal("\n\n"))
                    .append(Component.literal("DU Broadcast:").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                    .append(Component.literal("\n"))
                    .append(Component.literal(message).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("\n\n"))
            );
        });
    }

    private static void handleServerInvite(JsonObject root) {
        String ip = getNullableString(root, "ip");
        String inviter = getNullableString(root, "from");
        String sentAt = getNullableString(root, "sentAt");
        if (ip == null || inviter == null) return;

        mc.execute(() -> ServerInviteManager.receiveInvite(ip, inviter, sentAt));
    }

    private static void handleSocketDisconnect(int gen, WebSocket webSocket, String reason) {
        if (gen != generation) return;
        if (webSocket != null && socket != webSocket) return;

        socket = null;
        socketConnected = false;
        socketConnecting = false;

        reconnectAtMs = System.currentTimeMillis() + RECONNECT_DELAY_MS;
        setPhase(UiText.DISCONNECTED, reason + " Retrying shortly...");
        syncCapeState();
    }

    private static void refreshExpiredToken() {
        if (!accountVerified || linkToken == null || linkTokenExpiresAtMs <= 0L) return;
        if (System.currentTimeMillis() < linkTokenExpiresAtMs - 60_000L) return;
        if (SessionManager.isSessionValid == false) return;
        retryAccountVerification();
    }

    private static void detectSessionSwitch() {
        if (!accountVerified) return;
        if (!ensureSessionValidatedAndRetry(false)) return;

        String currentFingerprint = getSessionFingerprint();
        if (currentFingerprint.isBlank()) return;
        if (currentFingerprint.equals(verifiedSessionFingerprint) || currentFingerprint.equals(requestedSessionFingerprint)) return;

        retryAccountVerification();
    }

    private static boolean ensureSessionValidatedAndRetry(boolean retryAfterValidation) {
        if (SessionManager.isSessionValid != null) {
            if (SessionManager.isSessionValid && retryAfterValidation) {
                retryAccountVerificationIfReady();
            }
            return SessionManager.isSessionValid;
        }

        if (!SessionManager.hasValidationStarted) {
            SessionManager.hasValidationStarted = true;

            String sessionFingerprint = getSessionFingerprint();
            String accessToken = Minecraft.getInstance().getUser().getAccessToken();

            Thread.ofVirtual().start(() -> {
                boolean valid = !accessToken.isBlank() && SessionAPI.validateSession(accessToken);

                Minecraft.getInstance().execute(() -> {
                    if (!Objects.equals(sessionFingerprint, getSessionFingerprint())) {
                        SessionManager.hasValidationStarted = false;
                        ensureSessionValidatedAndRetry(retryAfterValidation);
                        return;
                    }

                    SessionManager.isSessionValid = valid;
                    SessionManager.hasValidationStarted = false;

                    if (valid && retryAfterValidation) {
                        retryAccountVerificationIfReady();
                    }
                });
            });
        }

        return false;
    }

    private static void retryAccountVerificationIfReady() {
        if (!accountVerified) retryAccountVerification();
    }

    private static void resetAccountState() {
        accountVerified = false;
        linkedAccount = null;
        linkToken = null;
        linkTokenExpiresAtMs = 0L;
        capeCatalog = EMPTY_CAPE_CATALOG;
        verifiedSessionFingerprint = "";
        requestedSessionFingerprint = "";
    }

    private static void closeSocket() {
        WebSocket previous = socket;
        socket = null;
        if (previous != null) {
            try {
                previous.sendClose(WebSocket.NORMAL_CLOSURE, "reset");
            } catch (Exception ignored) {
                previous.abort();
            }
        }
    }

    private static void syncCapeState() {
        if (!canUseCapes()) {
            CapeManager.disableCape();
            return;
        }
        String selected = capeCatalog.selected();
        if (selected == null || selected.isBlank()) CapeManager.disableCape();
        else CapeManager.setCape(selected);
    }

    private static void setPhase(UiText text) {
        setPhase(text, text.detail);
    }

    private static void setPhase(UiText text, String detail) {
        statusLine = text.status;
        detailLine = detail;
    }

    private static MinecraftAccount parseAccount(JsonElement element) {
        if (!(element instanceof JsonObject object)) return null;

        String uuid = firstNonBlank(getNullableString(object, "uuid"), getNullableString(object, "id"));
        String username = firstNonBlank(getNullableString(object, "username"), getNullableString(object, "name"));
        if (uuid == null && username == null) return null;
        return new MinecraftAccount(uuid, username);
    }

    private static CapeCatalog parseCapeCatalog(JsonObject catalog) {
        if (catalog == null) return EMPTY_CAPE_CATALOG;

        String topSelected = firstNonBlank(getNullableString(catalog, "selected"), getNullableString(catalog, "selectedCape"));

        JsonElement capes = getChild(catalog, "capes");
        if (!(capes instanceof JsonArray array)) return new CapeCatalog(Map.of(), topSelected);

        Map<String, String> textureUrls = new LinkedHashMap<>();
        String selectedKey = topSelected;
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull() || !element.isJsonObject()) continue;

            JsonObject object = element.getAsJsonObject();
            String key = getNullableString(object, "capeKey");
            String textureUrl = firstNonBlank(
                getNullableString(object, "texture"),
                getNullableString(object, "textureUrl"),
                getNullableString(object, "url"));
            String state = getNullableString(object, "state");
            if (key == null) continue;

            if ("active".equalsIgnoreCase(state)) selectedKey = key;

            if (textureUrl != null) textureUrls.putIfAbsent(key, textureUrl);
        }
        return new CapeCatalog(Map.copyOf(textureUrls), selectedKey);
    }

    private static long parseExpiresAt(JsonObject object) {
        if (object == null || !object.has("expiresAt") || object.get("expiresAt").isJsonNull()) return 0L;
        try {
            JsonElement el = object.get("expiresAt");
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                long raw = el.getAsLong();
                return raw < 1_000_000_000_000L ? raw * 1_000L : raw;
            }
            String raw = el.getAsString();
            try {
                return java.time.Instant.parse(raw).toEpochMilli();
            } catch (Exception ignored) {
                long num = Long.parseLong(raw);
                return num < 1_000_000_000_000L ? num * 1_000L : num;
            }
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String getSessionFingerprint() {        User session = Minecraft.getInstance().getUser();
        UUID uuid = session.getProfileId();
        return session.getAccessToken() + "|" + uuid;
    }

    private static String simplifyError(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null || cause.getMessage().isBlank()
            ? cause.getClass().getSimpleName()
            : cause.getMessage();
    }

    private static JsonElement getChild(JsonObject object, String key) {
        if (object == null || !object.has(key)) return null;
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? null : element;
    }

    private static String getNullableString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return null;
        String value;
        try {
            value = object.get(key).getAsString();
        } catch (Exception ignored) {
            return null;
        }
        return value.isBlank() ? null : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    public record MinecraftAccount(String uuid, String username) {}

    public record CapeCatalog(Map<String, String> textureUrls, String selected) {}

    private enum UiText {
        CONNECTING_SOCKET("Connecting to backend", "Opening the WebSocket session..."),
        WAITING_FOR_ACCOUNT_REQUEST("Waiting for Minecraft account verification", "Requesting a new verification challenge..."),
        WAITING_FOR_ACCOUNT_CONFIRMATION("Waiting for Minecraft account verification", "Mojang session join succeeded. Waiting for backend confirmation..."),
        ADDON_READY("Addon ready", ""),
        READY_NO_ACCOUNT("Addon ready", "Verify a Minecraft account to access cape management."),
        READY_UNSUPPORTED_SESSION("Addon ready", "This Minecraft session cannot be linked. Please use a valid Minecraft account."),
        READY_LINK_FAILED("Addon ready", "Mojang session join failed for this account. Please try again later."),
        DISCONNECTED("Backend disconnected", "Retrying shortly..."),
        MINECRAFT_VERIFY_FAILED("Minecraft verification failed", "Minecraft account verification failed."),
        BACKEND_REJECTED("Backend rejected the current step", "Unknown backend error.");

        private final String status;
        private final String detail;

        UiText(String status, String detail) {
            this.status = status;
            this.detail = detail;
        }
    }

    private static final class AddonSocketListener implements WebSocket.Listener {
        private final int gen;
        private final StringBuilder textBuffer = new StringBuilder();

        private AddonSocketListener(int gen) {
            this.gen = gen;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
            handleSocketOpen(gen, webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String message = textBuffer.toString();
                textBuffer.setLength(0);
                handleSocketMessage(gen, webSocket, message);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            handleSocketDisconnect(gen, webSocket, reason == null || reason.isBlank() ? "WebSocket closed." : reason);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            handleSocketDisconnect(gen, webSocket, "WebSocket error: " + simplifyError(error));
        }
    }
}