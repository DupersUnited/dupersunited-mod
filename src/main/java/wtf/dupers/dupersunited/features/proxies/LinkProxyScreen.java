package wtf.dupers.dupersunited.features.proxies;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class LinkProxyScreen extends Screen {

    private final Screen parent;
    private final String accountName;

    private static final int ENTRY_HEIGHT = 25;
    private static final int LIST_TOP = 50;
    private static final int LIST_BOTTOM_MARGIN = 10;

    private final List<Button> proxyButtons = new ArrayList<>();
    private double scrollOffset = 0;
    private int maxScroll = 0;

    public LinkProxyScreen(Screen parent, String accountName) {
        super(Component.literal("Link Proxy to " + accountName));
        this.parent = parent;
        this.accountName = accountName;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Back"), btn -> {
            assert this.minecraft != null;
            this.minecraft.gui.setScreen(parent);
        }).bounds(5, 8, 50, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Unlink Proxy").withStyle(ChatFormatting.RED), btn -> {
            AccountProxyLinks.unlink(accountName);
            assert this.minecraft != null;
            this.minecraft.gui.setScreen(parent);
        }).bounds(this.width - 110, 8, 100, 20).build());

        proxyButtons.clear();
        List<ProxyProfiles> profiles = ProxyConfigManager.profiles;
        int y = LIST_TOP;
        for (ProxyProfiles profile : profiles) {
            final ProxyProfiles p = profile;
            boolean isLinked = p.name.equals(AccountProxyLinks.getLinkedProxy(accountName));
            String label = (isLinked ? "§a " : "") + p.name + " §7(" + p.address + ")";

            Button button = Button.builder(Component.literal(label), btn -> {
                AccountProxyLinks.link(accountName, p.name);
                assert this.minecraft != null;
                this.minecraft.gui.setScreen(parent);
            }).bounds(this.width / 2 - 150, y, 300, 20).build();
            this.addRenderableWidget(button);
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

        for (Button button : proxyButtons) {
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xFF101010);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(font,
            Component.literal("Link a proxy to ").withStyle(ChatFormatting.RESET)
                .append(Component.literal(accountName).withStyle(ChatFormatting.AQUA)),
            this.width / 2, 20, 0xFFFFFFFF);

        String current = AccountProxyLinks.getLinkedProxy(accountName);
        Component currentStatusComponent;
        if (current != null) {
            currentStatusComponent = Component.literal("Current Profile Linked: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(current).withStyle(ChatFormatting.GREEN));
        } else {
            currentStatusComponent = Component.literal("No proxy linked").withStyle(ChatFormatting.GRAY);
        }

        graphics.centeredText(font, currentStatusComponent, this.width / 2, 35, 0xFFFFFFFF);

        if (ProxyConfigManager.profiles.isEmpty()) {
            graphics.centeredText(font,
                Component.literal("No proxy profiles found! Please remember to add some in Proxy Manager first. :)").withStyle(ChatFormatting.RED),
                this.width / 2, this.height / 2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}