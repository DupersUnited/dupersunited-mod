package wtf.dupers.dupersunited.features.proxies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.NonNull;
import wtf.dupers.dupersunited.features.screens.ui.DuScreen;

import static wtf.dupers.dupersunited.features.screens.ui.Theme.*;
import static wtf.dupers.dupersunited.MainClient.mc;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ProxyScreen extends DuScreen {

    private static final int ENTRY_HEIGHT = 30;
    private static final int LIST_TOP = 62;
    private static final int BOTTOM_PANEL_HEIGHT = 160;

    private final List<Button> entryButtons = new ArrayList<>();

    private EditBox nameField, addressField, userField, passField;
    private Button addButton;
    private Button editButton;
    private Button deleteButton;
    private Button importButton;
    private Button toggleButton;
    private Button warningButton;
    private Component errorMessage = Component.empty();
    private Component statusMessage = Component.empty();
    private int editingIndex = -1;

    private int listBottom;
    private int selectedIndex = -1;
    private int scrollOffset = 0;

    public ProxyScreen(Screen parent) {
        super(Component.literal("Proxy Manager"), parent, "Proxies");
    }

    private int getMaxVisible() {
        return Math.max(1, (listBottom - LIST_TOP) / ENTRY_HEIGHT);
    }

    @Override
    protected void init() {
        listBottom = this.height - BOTTOM_PANEL_HEIGHT - 10;
        int cx = this.width / 2 - 100;

        String name = nameField != null ? nameField.getValue() : "";
        String addr = addressField != null ? addressField.getValue() : "";
        String user = userField != null ? userField.getValue() : "";
        String pass = passField != null ? passField.getValue() : "";

        nameField = field(cx, height - 145, "Profile Name", name);
        addressField = field(cx + 105, height - 145, "Host:Port", addr);
        userField = field(cx, height - 120, "Username", user);
        passField = field(cx + 105, height - 120, "Password", pass);

        this.addRenderableWidget(nameField);
        this.addRenderableWidget(addressField);
        this.addRenderableWidget(userField);
        this.addRenderableWidget(passField);

        addButton = Button.builder(Component.literal("Add").withStyle(ChatFormatting.GREEN), _ -> saveProfile())
            .bounds(cx, height - 95, 47, 20)
            .tooltip(Tooltip.create(Component.literal("Add a new proxy profile")))
            .build();
        this.addRenderableWidget(addButton);

        editButton = Button.builder(Component.literal("Edit").withStyle(ChatFormatting.YELLOW), _ -> startEdit())
            .bounds(cx + 51, height - 95, 47, 20)
            .tooltip(Tooltip.create(Component.literal("Load the selected profile into the fields to edit it")))
            .build();
        this.addRenderableWidget(editButton);

        deleteButton = Button.builder(Component.literal("Delete").withStyle(ChatFormatting.RED), _ -> deleteSelected())
            .bounds(cx + 102, height - 95, 47, 20)
            .tooltip(Tooltip.create(Component.literal("Delete the selected proxy profile")))
            .build();
        this.addRenderableWidget(deleteButton);

        //import proxies button
        importButton = Button.builder(Component.literal("Import").withStyle(ChatFormatting.AQUA), _ -> massImportFromClipboard())
            .bounds(cx + 153, height - 95, 47, 20)
            .tooltip(Tooltip.create(Component.literal("Bulk adds proxies from your clipboard.\nOne per line, as host:port or host:port:user:pass")))
            .build();
        this.addRenderableWidget(importButton);

        // toggle
        toggleButton = Button.builder(toggleText(), btn -> {
                ProxyConfigManager.globalEnabled = !ProxyConfigManager.globalEnabled;
                ProxyConfigManager.save();
                btn.setMessage(toggleText());
            })
            .bounds(cx, height - 70, 200, 20)
            .tooltip(Tooltip.create(Component.literal("Toggles proxies on or off")))
            .build();
        this.addRenderableWidget(toggleButton);

        // proxy warning toggle
        warningButton = Button.builder(warningText(), btn -> {
                ProxyConfigManager.proxyWarningEnabled = !ProxyConfigManager.proxyWarningEnabled;
                ProxyConfigManager.save();
                btn.setMessage(warningText());
            })
            .bounds(cx, height - 45, 200, 20)
            .tooltip(Tooltip.create(Component.literal("Warns you when joining a server without a proxy enabled")))
            .build();
        this.addRenderableWidget(warningButton);

        this.setInitialFocus(nameField);
        rebuildEntryButtons();
    }

    @Override
    public void tick() {
        super.tick();
        addButton.active = !nameField.getValue().trim().isEmpty() && !addressField.getValue().trim().isEmpty();
        boolean hasSel = selectedIndex >= 0 && selectedIndex < ProxyConfigManager.profiles.size();
        editButton.active = hasSel;
        deleteButton.active = hasSel;
        addButton.setMessage(Component.literal(editingIndex >= 0 ? "Save" : "Add").withStyle(ChatFormatting.GREEN));
    }

    private EditBox field(int x, int y, String hint, String value) {
        int maxLen = 16;//set to 16 for now instead of 64
        EditBox f = new EditBox(this.font, x, y, 95, 20, Component.literal(hint));
        f.setMaxLength(maxLen);
        f.setHint(Component.literal(hint));
        f.setValue(value);
        f.setResponder(text -> {
            if (f == nameField) {
                String warningText = "You hit character limit for Proxy Names (" + maxLen + " characters)! Maybe name it something else?";
                if (text.length() >= maxLen) {
                    errorMessage = Component.literal(warningText).withStyle(ChatFormatting.RED);
                } else if (errorMessage.getString().equals(warningText)) {
                    errorMessage = Component.empty();
                }
            }
        });
        return f;
    }

    private void saveProfile() {
        String name = nameField.getValue().trim();
        String addr = addressField.getValue().trim();
        statusMessage = Component.empty();
        if (name.isEmpty() || addr.isEmpty()) {
            errorMessage = Component.literal("Name and Host:Port are required!").withStyle(ChatFormatting.RED);
            return;
        }
        if (!validAddress(addr)) {
            errorMessage = Component.literal("Invalid address, use host:port (port 1-65535)!").withStyle(ChatFormatting.RED);
            return;
        }

        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;
        for (int i = 0; i < profiles.size(); i++) {
            if (i != editingIndex && profiles.get(i).name.equalsIgnoreCase(name)) {
                errorMessage = Component.literal("A proxy with that name already exists!").withStyle(ChatFormatting.RED);
                return;
            }
        }
        errorMessage = Component.empty();

        ProxyProfiles p = new ProxyProfiles(name, addr, userField.getValue().trim(), passField.getValue().trim());
        if (editingIndex >= 0 && editingIndex < profiles.size()) {
            if (ProxyConfigManager.activeProfileName.equals(profiles.get(editingIndex).name))
                ProxyConfigManager.activeProfileName = name;
            profiles.set(editingIndex, p);
            selectedIndex = editingIndex;
        } else {
            profiles.add(p);
            selectedIndex = profiles.size() - 1;
        }
        ProxyConfigManager.save();

        editingIndex = -1;
        clearFields();
        rebuildEntryButtons();
    }

    private void massImportFromClipboard() {
        String clipboard = mc.keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            errorMessage = Component.literal("Failed to import, clipboard is empty!").withStyle(ChatFormatting.RED);
            statusMessage = Component.empty();
            return;
        }

        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;
        int imported = 0, skippedInvalid = 0, skippedDuplicate = 0;

        for (String rawLine : clipboard.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(":");
            String host, portStr, user = "", pass = "";
            if (parts.length == 2) {
                host = parts[0];
                portStr = parts[1];
            } else if (parts.length >= 4) {
                host = parts[0];
                portStr = parts[1];
                user = parts[2];
                pass = parts[3];
            } else {
                skippedInvalid++;
                continue;
            }

            String address = host + ":" + portStr;
            if (!validAddress(address)) {
                skippedInvalid++;
                continue;
            }

            boolean alreadyExists = profiles.stream().anyMatch(p -> p.address.equalsIgnoreCase(address));
            if (alreadyExists) {
                skippedDuplicate++;
                continue;
            }

            profiles.add(new ProxyProfiles(uniqueImportName(host), address, user, pass));
            imported++;
        }

        if (imported > 0) ProxyConfigManager.save();

        MutableComponent result = Component.literal("Importing " + imported).withStyle(ChatFormatting.GREEN);
        if (skippedDuplicate > 0) result.append(Component.literal(", " + skippedDuplicate + " duplicate").withStyle(ChatFormatting.GRAY));
        if (skippedInvalid > 0) result.append(Component.literal(", " + skippedInvalid + " invalid").withStyle(ChatFormatting.GRAY));

        errorMessage = Component.empty();
        statusMessage = imported > 0 || skippedDuplicate > 0 || skippedInvalid > 0
            ? result
            : Component.literal("No proxies found in clipboard!").withStyle(ChatFormatting.RED);

        rebuildEntryButtons();
    }

    private String uniqueImportName(String base) {
        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;
        String name = base;
        int suffix = 2;
        while (true) {
            String candidateName = name;
            boolean taken = profiles.stream().anyMatch(p -> p.name.equalsIgnoreCase(candidateName));
            if (!taken) return name;
            name = base + " (" + suffix + ")";
            suffix++;
        }
    }

    private void startEdit() {
        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;
        if (selectedIndex < 0 || selectedIndex >= profiles.size()) return;
        editingIndex = selectedIndex;
        ProxyProfiles p = profiles.get(selectedIndex);
        nameField.setValue(p.name);
        addressField.setValue(p.address);
        userField.setValue(p.user);
        passField.setValue(p.pass);
        errorMessage = Component.empty();
    }

    private void clearFields() {
        nameField.setValue("");
        addressField.setValue("");
        userField.setValue("");
        passField.setValue("");
    }

    // i love chatgpt
    private boolean validAddress(String addr) {
        int colon = addr.lastIndexOf(':');
        if (colon <= 0 || colon == addr.length() - 1) return false;
        if (!addr.substring(0, colon).matches("[A-Za-z0-9._-]+")) return false;
        try {
            int port = Integer.parseInt(addr.substring(colon + 1));
            return port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void deleteSelected() {
        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;
        if (selectedIndex < 0 || selectedIndex >= profiles.size()) return;
        statusMessage = Component.empty();

        if (profiles.get(selectedIndex).name.equals(ProxyConfigManager.activeProfileName))
            ProxyConfigManager.activeProfileName = "";

        profiles.remove(selectedIndex);
        ProxyConfigManager.save();
        if (editingIndex == selectedIndex) {
            editingIndex = -1;
            clearFields();
        } else if (editingIndex > selectedIndex) {
            editingIndex--;
        }
        selectedIndex = -1;
        rebuildEntryButtons();
    }

    private void rebuildEntryButtons() {
        entryButtons.forEach(this::removeWidget);
        entryButtons.clear();

        int maxVisible = getMaxVisible();
        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;

        for (int i = 0; i < maxVisible && (i + scrollOffset) < profiles.size(); i++) {
            int index = i + scrollOffset;
            int y = LIST_TOP + i * ENTRY_HEIGHT;

            Button selectBtn = Button.builder(Component.empty(), _ -> selectedIndex = index)
                .bounds(0, y, this.width - 115, ENTRY_HEIGHT)
                .build();
            selectBtn.setAlpha(0f);
            this.addRenderableWidget(selectBtn);
            entryButtons.add(selectBtn);

            Button checkBtn = Button.builder(Component.literal("Check").withStyle(ChatFormatting.GREEN), btn -> {
                    checkProxy(profiles.get(index), btn);
                    selectedIndex = index;
                })
                .bounds(this.width - 110, y + (ENTRY_HEIGHT / 2 - 10), 50, 20)
                .tooltip(Tooltip.create(Component.literal("Verify if the proxy is active")))
                .build();
            this.addRenderableWidget(checkBtn);
            entryButtons.add(checkBtn);

            Button useBtn = Button.builder(Component.literal("Use").withStyle(ChatFormatting.GREEN), _ -> {
                    ProxyConfigManager.activeProfileName = profiles.get(index).name;
                    ProxyConfigManager.save();
                    selectedIndex = index;
                })
                .bounds(this.width - 55, y + (ENTRY_HEIGHT / 2 - 10), 50, 20)
                .tooltip(Tooltip.create(Component.literal("Set as active proxy")))
                .build();
            this.addRenderableWidget(useBtn);
            entryButtons.add(useBtn);
        }
    }

    private Component toggleText() {
        return Component.literal("Proxies are now ").withStyle(ChatFormatting.WHITE)
            .append(Component.literal(ProxyConfigManager.globalEnabled ? "Enabled" : "Disabled")
                .withStyle(ProxyConfigManager.globalEnabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private Component warningText() {
        return Component.literal("Proxy Warning ").withStyle(ChatFormatting.WHITE)
            .append(Component.literal(ProxyConfigManager.proxyWarningEnabled ? "Enabled" : "Disabled")
                .withStyle(ProxyConfigManager.proxyWarningEnabled ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, ProxyConfigManager.profiles.size() - getMaxVisible());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) verticalAmount));
        rebuildEntryButtons();
        return true;
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawStructure(graphics, mouseX, mouseY);

        renderProxyList(graphics, mouseX, mouseY);

        graphics.centeredText(font, Component.literal("Proxy Manager").withStyle(ChatFormatting.BOLD), this.width / 2, 30, secondary);

        if (!errorMessage.getString().isEmpty()) {
            graphics.centeredText(font, errorMessage, this.width / 2, 42, bad);
        } else if (!statusMessage.getString().isEmpty()) {
            graphics.centeredText(font, statusMessage, this.width / 2, 42, text);
        }

        toggleButton.setMessage(toggleText());
        warningButton.setMessage(warningText());

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderProxyList(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;
        int maxVisible = getMaxVisible();

        for (int i = 0; i < maxVisible && (i + scrollOffset) < profiles.size(); i++) {
            int index = i + scrollOffset;
            ProxyProfiles profile = profiles.get(index);
            int y = LIST_TOP + i * ENTRY_HEIGHT;

            boolean isSelected = index == selectedIndex;
            boolean isActive = profile.name.equals(ProxyConfigManager.activeProfileName);
            boolean hovered = mouseX >= 0 && mouseX < this.width - 115 && mouseY >= y && mouseY < y + ENTRY_HEIGHT;

            int rowBg = (index % 2 == 0) ? border : header;
            context.fill(5, y, this.width - 115, y + ENTRY_HEIGHT - 1, rowBg);

            if (isSelected) context.fill(5, y, this.width - 115, y + ENTRY_HEIGHT - 1, 0x44FFFFFF);
            else if (hovered) context.fill(5, y, this.width - 115, y + ENTRY_HEIGHT - 1, 0x22FFFFFF);

            context.fill(5, y, 7, y + ENTRY_HEIGHT - 1, isActive ? accent : edge);

            ChatFormatting color = isActive ? ChatFormatting.GREEN : isSelected ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
            context.text(font, Component.literal(profile.name).withStyle(color), 13, y + 5, 0xFFFFFFFF, true);
            context.text(font, Component.literal(profile.address).withStyle(ChatFormatting.GRAY), 13, y + 17, 0xFFFFFFFF, true);
        }

        if (profiles.size() > maxVisible) {
            int totalListArea = maxVisible * ENTRY_HEIGHT;
            int barHeight = Math.max(10, totalListArea * maxVisible / profiles.size());
            int maxScroll = profiles.size() - maxVisible;
            int barY = LIST_TOP + (totalListArea - barHeight) * scrollOffset / Math.max(1, maxScroll);
            context.fill(this.width - 4, LIST_TOP, this.width - 1, LIST_TOP + totalListArea, border);
            context.fill(this.width - 4, barY, this.width - 1, barY + barHeight, secondary);
        }
    }

    public void checkProxy(ProxyProfiles profile, Button btn) {
        statusMessage = Component.literal("Checking proxy...").withStyle(ChatFormatting.YELLOW);
        btn.active = false;

        Thread.ofVirtual().start(() -> {
            boolean valid = testProxy(profile, 5000);
            statusMessage = Component.literal("Proxy ").append(Component.literal(profile.name).withStyle(ChatFormatting.BOLD)).append(valid ? " is valid" : " is invalid").withStyle(valid ? ChatFormatting.GREEN : ChatFormatting.RED);
            btn.active = true;
        });
    }

    private boolean testProxy(ProxyProfiles profile, int timeoutMs) {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(profile.getHost(), profile.getPort()));
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create("http://www.google.com/generate_204").toURL().openConnection(proxy);
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("HEAD");

            if (!profile.user.isEmpty() || !profile.pass.isEmpty()) {
                String encodedAuth = Base64.getEncoder().encodeToString((profile.user + ":" + profile.pass).getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Proxy-Authorization", "Basic " + encodedAuth);
            }

            int responseCode = conn.getResponseCode();
            return (responseCode >= 200 && responseCode < 400) || responseCode == 407;
        } catch (IOException e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}