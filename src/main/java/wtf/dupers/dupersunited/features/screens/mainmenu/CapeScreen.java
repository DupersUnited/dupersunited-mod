package wtf.dupers.dupersunited.features.screens.mainmenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import wtf.dupers.dupersunited.features.cosmetics.CapeManager;
import wtf.dupers.dupersunited.features.account.AccountsScreen;
import wtf.dupers.dupersunited.features.account.SessionManager;
import wtf.dupers.dupersunited.features.auth.AuthManager;
import wtf.dupers.dupersunited.features.auth.AuthManager.MinecraftAccount;
import wtf.dupers.dupersunited.features.screens.ui.DuScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static wtf.dupers.dupersunited.features.screens.ui.Theme.*;

public class CapeScreen extends DuScreen {
    private static final int CARD_W = 76;
    private static final int CARD_H = 72;
    private static final int GAP = 8;

    private static final int HERO_TOP = 44;
    private static final int HERO_PREVIEW_W = 40;
    private static final int HERO_PREVIEW_H = 64;

    private final List<Button> capeButtons = new ArrayList<>();

    private Button actionButton;
    private int scrollOffset;
    private Set<String> lastCapes = Set.of();
    private String lastSelectedCape;
    private String lastLinkedUuid;
    private String lastSessionName;
    private Boolean lastSessionValid;

    public CapeScreen(Screen parent) {
        super(Component.literal("Capes"), parent, "Capes");
    }

    @Override
    protected void init() {
        AuthManager.init();
        AuthManager.retry();

        actionButton = Button.builder(Component.literal("Refresh"), _ -> {
                if (SessionManager.isSessionValid == null) return;
                if (!SessionManager.isSessionValid) {
                    this.minecraft.gui.setScreen(new AccountsScreen(this));
                    return;
                }
                if (AuthManager.canUseCapes()) {
                    AuthManager.requestCapeList();
                } else {
                    AuthManager.retryAccountVerification();
                }
            })
            .bounds(this.width - 72, 28, 62, 18)
            .build();
        this.addRenderableWidget(actionButton);

        rebuildList();
        refreshAction();
    }

    @Override
    public void tick() {
        super.tick();

        MinecraftAccount linked = AuthManager.getLinkedAccount();
        String linkedUuid = linked == null ? null : linked.uuid();
        AuthManager.CapeCatalog catalog = AuthManager.getCapeCatalog();

        if (!Objects.equals(lastLinkedUuid, linkedUuid)
                || !Objects.equals(lastSelectedCape, catalog.selected())
                || !lastCapes.equals(catalog.textureUrls().keySet())
                || !Objects.equals(lastSessionValid, SessionManager.isSessionValid)
                || !Objects.equals(lastSessionName, SessionManager.getUsername())) {
            rebuildList();
        }

        refreshAction();
    }

    private void refreshAction() {
        if (SessionManager.isSessionValid == null) {
            actionButton.active = false;
            actionButton.setMessage(Component.literal("..."));
        } else if (!SessionManager.isSessionValid) {
            actionButton.active = true;
            actionButton.setMessage(Component.literal("Accounts"));
        } else {
            actionButton.active = true;
            actionButton.setMessage(Component.literal(AuthManager.canUseCapes() ? "Refresh" : "Verify"));
        }
    }

    private static List<String> capeOptions() {
        List<String> options = new ArrayList<>();
        if (AuthManager.canUseCapes()) {
            options.add("disabled");
            options.addAll(AuthManager.getCapeCatalog().textureUrls().keySet());
        }
        return options;
    }

    private static String equippedKey() {
        String selected = AuthManager.getCapeCatalog().selected();
        return selected == null || selected.isBlank() ? "disabled" : selected;
    }

    private boolean hasHero() {
        return AuthManager.canUseCapes();
    }

    private int gridTop() {
        return hasHero() ? HERO_TOP + HERO_PREVIEW_H + 16 : 54;
    }

    private int gridBottom() {
        return this.height - 52;
    }

    private int columns() {
        return Math.max(1, (this.width - 20 + GAP) / (CARD_W + GAP));
    }

    private int visibleCardRows() {
        return Math.max(1, (gridBottom() - gridTop() + GAP) / (CARD_H + GAP));
    }

    private int totalCardRows(int count) {
        return Math.max(1, (count + columns() - 1) / columns());
    }

    private void rebuildList() {
        capeButtons.forEach(this::removeWidget);
        capeButtons.clear();

        MinecraftAccount linked = AuthManager.getLinkedAccount();
        lastLinkedUuid = linked == null ? null : linked.uuid();
        AuthManager.CapeCatalog catalog = AuthManager.getCapeCatalog();
        lastCapes = AuthManager.canUseCapes() ? catalog.textureUrls().keySet() : Set.of();
        lastSelectedCape = catalog.selected();
        lastSessionValid = SessionManager.isSessionValid;
        lastSessionName = SessionManager.getUsername();

        this.clearWidgets();
        this.addRenderableWidget(actionButton);

        List<String> options = capeOptions();
        int cols = columns();
        int maxOffset = Math.max(0, totalCardRows(options.size()) - visibleCardRows());
        scrollOffset = Math.clamp(scrollOffset, 0, maxOffset);

        String selected = equippedKey();
        int startX = (this.width - (cols * CARD_W + (cols - 1) * GAP)) / 2;
        for (int i = 0; i < options.size(); i++) {
            int row = i / cols - scrollOffset;
            if (row < 0 || row >= visibleCardRows()) {
                continue;
            }
            String capeKey = options.get(i);
            int x = startX + (i % cols) * (CARD_W + GAP);
            int y = gridTop() + row * (CARD_H + GAP);

            if (selected.equals(capeKey)) continue;
            Button pick = Button.builder(Component.empty(), _ ->
                    AuthManager.pickCape("disabled".equals(capeKey) ? null : capeKey))
                .bounds(x, y, CARD_W, CARD_H)
                .build();
            pick.setAlpha(0f);
            this.addRenderableWidget(pick);
            capeButtons.add(pick);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxOffset = Math.max(0, totalCardRows(capeOptions().size()) - visibleCardRows());
        int next = Math.max(0, Math.min(maxOffset, scrollOffset - (int) verticalAmount));
        if (next != scrollOffset) {
            scrollOffset = next;
            rebuildList();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawStructure(graphics, mouseX, mouseY);

        graphics.centeredText(
            this.font,
            Component.literal("Cape Manager").withStyle(ChatFormatting.BOLD),
            this.width / 2,
            30,
            secondary
        );

        Component status = statusLine();
        if (!status.getString().isEmpty()) {
            graphics.centeredText(this.font, status, this.width / 2, this.height - 42, text);
        }

        if (hasHero()) {
            renderHero(graphics);
        }
        renderGrid(graphics, mouseX, mouseY);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private static Component statusLine() {
        if (SessionManager.isSessionValid == null) {
            return Component.literal("validating session...").withStyle(ChatFormatting.YELLOW);
        }
        if (!SessionManager.isSessionValid) {
            return Component.literal("switch to a valid account first").withStyle(ChatFormatting.RED);
        }
        if (!AuthManager.canUseCapes()) {
            return Component.literal("verifying account").withStyle(ChatFormatting.YELLOW);
        }
        return Component.empty();
    }

    private void renderHero(GuiGraphicsExtractor graphics) {
        String key = equippedKey();
        boolean empty = "disabled".equals(key);
        Identifier texture = empty ? null : CapeManager.getPreviewTexture(key);

        int x = this.width / 2 - (HERO_PREVIEW_W + 8) / 2;
        int border = empty ? edge : accent;
        graphics.fill(x, HERO_TOP, x + HERO_PREVIEW_W + 8, HERO_TOP + HERO_PREVIEW_H + 8, surface);
        graphics.fill(x, HERO_TOP, x + HERO_PREVIEW_W + 8, HERO_TOP + 1, border);
        graphics.fill(x, HERO_TOP + HERO_PREVIEW_H + 7, x + HERO_PREVIEW_W + 8, HERO_TOP + HERO_PREVIEW_H + 8, border);
        graphics.fill(x, HERO_TOP, x + 1, HERO_TOP + HERO_PREVIEW_H + 8, border);
        graphics.fill(x + HERO_PREVIEW_W + 7, HERO_TOP, x + HERO_PREVIEW_W + 8, HERO_TOP + HERO_PREVIEW_H + 8, border);

        if (texture == null) {
            graphics.centeredText(this.font, Component.literal(empty ? "-" : "?"), this.width / 2, HERO_TOP + 30, empty ? dim : warn);
            return;
        }
        blitScaled(graphics, texture, x + 4, HERO_TOP + 4, 4);
    }

    private void renderGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<String> options = capeOptions();
        if (options.isEmpty()) {
            if (AuthManager.canUseCapes()) {
                graphics.centeredText(
                    this.font,
                    Component.literal("no capes on this account yet"),
                    this.width / 2,
                    (gridTop() + gridBottom()) / 2,
                    bad
                );
            }
            return;
        }

        String selected = equippedKey();
        int cols = columns();
        int startX = (this.width - (cols * CARD_W + (cols - 1) * GAP)) / 2;

        for (int i = 0; i < options.size(); i++) {
            int row = i / cols - scrollOffset;
            if (row < 0 || row >= visibleCardRows()) {
                continue;
            }
            String capeKey = options.get(i);
            int x = startX + (i % cols) * (CARD_W + GAP);
            int y = gridTop() + row * (CARD_H + GAP);
            boolean equipped = selected.equals(capeKey);
            boolean hovered = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;

            graphics.fill(x, y, x + CARD_W, y + CARD_H, equipped ? border : header);
            if (hovered && !equipped) {
                graphics.fill(x, y, x + CARD_W, y + CARD_H, 0x22FFFFFF);
            }
            int edgeColor = equipped ? accent : edge;
            graphics.fill(x, y, x + CARD_W, y + 1, edgeColor);
            graphics.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, edgeColor);
            graphics.fill(x, y, x + 1, y + CARD_H, edgeColor);
            graphics.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H, edgeColor);

            renderCardPreview(graphics, capeKey, x + (CARD_W - 20) / 2, y + 8);
            graphics.centeredText(
                this.font,
                Component.literal(shortName(capeKey)),
                x + CARD_W / 2,
                y + 48,
                equipped ? value : text
            );
        }

        int totalRows = totalCardRows(options.size());
        if (totalRows > visibleCardRows()) {
            int totalH = visibleCardRows() * (CARD_H + GAP) - GAP;
            int barH = Math.max(24, totalH * visibleCardRows() / totalRows);
            int maxScroll = totalRows - visibleCardRows();
            int barY = gridTop() + (totalH - barH) * scrollOffset / Math.max(1, maxScroll);
            graphics.fill(this.width - 4, gridTop(), this.width - 1, gridTop() + totalH, border);
            graphics.fill(this.width - 4, barY, this.width - 1, barY + barH, secondary);
        }
    }

    private String shortName(String capeKey) {
        String name = "disabled".equals(capeKey) ? "No cape" : capeKey;
        if (this.font.width(name) <= CARD_W - 10) {
            return name;
        }
        while (name.length() > 1 && this.font.width(name + "...") > CARD_W - 10) {
            name = name.substring(0, name.length() - 1);
        }
        return name + "...";
    }

    private void renderCardPreview(GuiGraphicsExtractor graphics, String capeKey, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 22, y + 34, border);
        graphics.fill(x - 2, y - 2, x + 22, y - 1, edge);
        graphics.fill(x - 2, y + 33, x + 22, y + 34, edge);
        graphics.fill(x - 2, y - 2, x - 1, y + 34, edge);
        graphics.fill(x + 21, y - 2, x + 22, y + 34, edge);

        if ("disabled".equals(capeKey)) {
            graphics.centeredText(this.font, Component.literal("-"), x + 10, y + 11, dim);
            return;
        }

        Identifier texture = CapeManager.getPreviewTexture(capeKey);
        if (texture == null) {
            graphics.centeredText(this.font, Component.literal("?"), x + 10, y + 11, warn);
            return;
        }
        blitScaled(graphics, texture, x, y, 2);
    }

    private static void blitScaled(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int scale) {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale((float) scale, (float) scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 1, 1, 10, 16, 64, 32);
        pose.popMatrix();
    }
}
