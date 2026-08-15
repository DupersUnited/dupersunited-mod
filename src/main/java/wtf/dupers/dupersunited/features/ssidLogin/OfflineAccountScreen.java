package wtf.dupers.dupersunited.features.ssidLogin;

import wtf.dupers.dupersunited.features.OfflineAccountManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class OfflineAccountScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private Text statusMessage = Text.literal("");

    public OfflineAccountScreen(Screen parent) {
        super(Text.literal("Offline Account Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> {
            AccountsScreen.loadAccounts(null);
            client.setScreen(parent);
        }).dimensions(5, 8, 50, 20).build());

        nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 40, 200, 20, Text.literal("Username"));
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("offline username"));
        this.addSelectableChild(nameField);
        this.addDrawableChild(nameField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Create Account"), btn -> createAccount())
            .dimensions(this.width / 2 - 100, 65, 200, 20).build());
    }

    private void createAccount() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusMessage = Text.literal("Enter a username first").formatted(net.minecraft.util.Formatting.RED);
            return;
        }
        if (OfflineAccountManager.exists(name)) {
            statusMessage = Text.literal("An offline account named " + name + " already exists").formatted(net.minecraft.util.Formatting.RED);
            return;
        }
        OfflineAccountManager.create(name);
        AccountsScreen.loadAccounts(null);
        statusMessage = Text.literal("Created offline account: " + name).formatted(net.minecraft.util.Formatting.GREEN);
        nameField.setText("");
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xFF101010);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Create an offline account"), this.width / 2, 20, 0xFFFFFFFF);
        if (!statusMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, statusMessage, this.width / 2, 95, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (nameField != null && nameField.keyPressed(input)) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (nameField != null && nameField.charTyped(input)) return true;
        return super.charTyped(input);
    }

    @Override
    public void close() {
        AccountsScreen.loadAccounts(null);
        MinecraftClient.getInstance().setScreen(parent);
    }
}

