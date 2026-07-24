package wtf.dupers.dupersunited.features.proxies;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class LinkProxyScreen extends Screen {

    private final Screen parent;
    private final String accountName;

    private static final int ENTRY_HEIGHT = 25;
    private static final int LIST_TOP = 50;
    private static final int LIST_BOTTOM_MARGIN = 10;

    private final List<ButtonWidget> proxyButtons = new ArrayList<>();
    private double scrollOffset = 0;
    private int maxScroll = 0;

    public LinkProxyScreen(Screen parent, String accountName) {
        super(Text.literal("Link Proxy to " + accountName));
        this.parent = parent;
        this.accountName = accountName;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn ->
                client.setScreen(parent)
        ).dimensions(5, 8, 50, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("§cUnlink Proxy"), btn -> {
            AccountProxyLinks.unlink(accountName);
            client.setScreen(parent);
        }).dimensions(this.width - 110, 8, 100, 20).build());

        proxyButtons.clear();
        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;
        int y = LIST_TOP;
        for (ProxyProfiles profile : profiles) {
            final ProxyProfiles p = profile;
            boolean isLinked = p.name.equals(AccountProxyLinks.getLinkedProxy(accountName));
            String label = (isLinked ? "§a " : "") + p.name + " §7(" + p.address + ")";

            ButtonWidget button = ButtonWidget.builder(Text.literal(label), btn -> {
                AccountProxyLinks.link(accountName, p.name);
                client.setScreen(parent);
            }).dimensions(this.width / 2 - 150, y, 300, 20).build();

            this.addDrawableChild(button);
            proxyButtons.add(button);

            y += ENTRY_HEIGHT;
        }

        int listBottom = this.height - LIST_BOTTOM_MARGIN;
        int contentHeight = profiles.size() * ENTRY_HEIGHT;
        int visibleHeight = listBottom - LIST_TOP;
        maxScroll = Math.max(0, contentHeight - visibleHeight);

        updateButtonPositions();
    }

    private void updateButtonPositions() {
        int listBottom = this.height - LIST_BOTTOM_MARGIN;
        int y = LIST_TOP - (int) scrollOffset;

        for (ButtonWidget button : proxyButtons) {
            button.setY(y);

            boolean visible = y + button.getHeight() > LIST_TOP && y < listBottom;
            button.visible = visible;
            button.active = visible;

            y += ENTRY_HEIGHT;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScroll > 0) {
            scrollOffset -= verticalAmount * ENTRY_HEIGHT;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            updateButtonPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF101010);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Link a proxy to §b" + accountName), this.width / 2, 20, 0xFFFFFFFF);

        String current = AccountProxyLinks.getLinkedProxy(accountName);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(current != null ? "§7Current Profile Linked: §a" + current : "§7No proxy linked"),
                this.width / 2, 35, 0xFFFFFFFF);

        if (ProxyConfigManager.profiles.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§cNo proxy profiles found! Please remember to add some in Proxy Manager first. :)"),
                    this.width / 2, this.height / 2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}