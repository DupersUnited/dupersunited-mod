package wtf.dupers.dupersunited.features.screens.mainmenu.alerts;

import net.minecraft.ChatFormatting;
import wtf.dupers.dupersunited.features.proxies.AccountProxyLinks;
import wtf.dupers.dupersunited.features.proxies.ProxyConfigManager;
import wtf.dupers.dupersunited.features.proxies.ProxyScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import static wtf.dupers.dupersunited.MainClient.mc;

public class NoProxyWarningScreen extends Screen {

    private final Screen parent;
    private final ServerData serverInfo;

    public NoProxyWarningScreen(Screen parent, ServerData serverInfo) {
        super(Component.literal("No Proxy Warning"));
        this.parent = parent;
        this.serverInfo = serverInfo;
    }

    @Override
    protected void init() {
        String currentAccount = mc.getUser().getName();
        boolean canBypass = AccountProxyLinks.hasBypass(currentAccount);

        if (canBypass) {
            this.addRenderableWidget(Button.builder(Component.literal("Connect Anyway").withStyle(ChatFormatting.GREEN), btn -> {
                ServerAddress address = ServerAddress.parseString(serverInfo.ip);
                ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc, address, serverInfo, false, null);
            }).bounds(this.width / 2 - 155, this.height / 2 + 20, 150, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("Go Back").withStyle(ChatFormatting.RED), btn ->
                mc.gui.setScreen(parent)
            ).bounds(this.width / 2 + 5, this.height / 2 + 20, 150, 20).build());
        } else {
            this.addRenderableWidget(Button.builder(Component.literal("Go Back").withStyle(ChatFormatting.RED), btn ->
                mc.gui.setScreen(parent)
            ).bounds(this.width / 2 - 100, this.height / 2 + 20, 200, 20).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("Open Proxy Manager"), btn ->
                mc.gui.setScreen(new ProxyScreen(this))
        ).bounds(this.width / 2 - 100, this.height / 2 + 45, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font,
            Component.literal("No Proxy Active!").withStyle(ChatFormatting.RED),
            this.width / 2, this.height / 2 - 50, 0xFFFFFFFF);

        graphics.centeredText(font,
            Component.literal("You are about to connect to ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(serverInfo.ip).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)),
            this.width / 2, this.height / 2 - 30, 0xFFFFFFFF);

        graphics.centeredText(font,
            Component.literal("without a proxy enabled, are you sure about this?").withStyle(ChatFormatting.GRAY),
            this.width / 2, this.height / 2 - 18, 0xFFFFFFFF);

        graphics.centeredText(font,
            Component.literal(ProxyConfigManager.getWarningReason()),
            this.width / 2, this.height / 2 - 5, 0xFFFFFFFF);

        // lt the user know why there's no connect anyway button
        String currentAccount = mc.getUser().getName();
        if (!AccountProxyLinks.hasBypass(currentAccount)) {
            graphics.centeredText(font,
                Component.literal("Enable bypass in Account Manager to connect without a proxy.").withStyle(ChatFormatting.DARK_GRAY),
                this.width / 2, this.height / 2 + 8, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}