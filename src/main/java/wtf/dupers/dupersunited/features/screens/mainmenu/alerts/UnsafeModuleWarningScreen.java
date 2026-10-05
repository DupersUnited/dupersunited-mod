package wtf.dupers.dupersunited.features.screens.mainmenu.alerts;

import net.minecraft.ChatFormatting;
import wtf.dupers.dupersunited.features.screens.ClickGui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import static wtf.dupers.dupersunited.MainClient.mc;

public class UnsafeModuleWarningScreen extends Screen {

    private final Screen parent;
    private final ServerData serverInfo;

    public UnsafeModuleWarningScreen(Screen parent, ServerData serverInfo) {
        super(Component.literal("Unsafe Module Warning"));
        this.parent = parent;
        this.serverInfo = serverInfo;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Connect Anyway").withStyle(ChatFormatting.GREEN), btn -> {
            ServerAddress address = ServerAddress.parseString(serverInfo.ip);
            ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), mc, address, serverInfo, false, null);
        }).bounds(this.width / 2 - 155, this.height / 2 + 20, 150, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Go Back").withStyle(ChatFormatting.RED), btn ->
            mc.gui.setScreen(parent)
        ).bounds(this.width / 2 + 5, this.height / 2 + 20, 150, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Open Click GUI"), btn ->
            mc.gui.setScreen(new ClickGui(this))
        ).bounds(this.width / 2 - 100, this.height / 2 + 45, 200, 20).build());

    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(font,
            Component.literal("Unsafe Modules Active!").withStyle(ChatFormatting.RED),
            this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);

        graphics.centeredText(font,
            Component.literal("Hold on! You are about to connect to").withStyle(ChatFormatting.GRAY),
            this.width / 2, this.height / 2 - 20, 0xFFFFFFFF);

        graphics.centeredText(font,
            Component.literal(serverInfo.ip).withStyle(ChatFormatting.WHITE),
            this.width / 2, this.height / 2 - 8, 0xFFFFFFFF);

        graphics.centeredText(font,
            Component.literal("with a unsafe module enabled.").withStyle(ChatFormatting.GRAY),
            this.width / 2, this.height / 2 + 4, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
