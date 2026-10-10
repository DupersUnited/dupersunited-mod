package wtf.dupers.dupersunited.features.account;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import wtf.dupers.dupersunited.features.screens.ui.DuScreen;
import wtf.dupers.dupersunited.features.screens.ui.Ui;

import static wtf.dupers.dupersunited.features.screens.ui.Theme.*;

public class AddAccountScreen extends DuScreen {
    private final AccountsScreen accounts;
    private final boolean offline;
    private EditBox field;
    private EditBox quantityField;
    private Component status = Component.empty();

    public AddAccountScreen(AccountsScreen parent, boolean offline) {
        super(Component.literal(offline ? "Add offline account" : "Add access token"), parent, null);
        this.accounts = parent;
        this.offline = offline;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 120;
        int y = this.height / 2 - 60;

        String value = field != null ? field.getValue() : "";
        field = new EditBox(this.font, x + 20, y + 36, 200, 20, Component.literal("Add"));
        field.setMaxLength(offline ? 16 : 32767);
        field.setHint(Component.literal(offline ? "Offline Username" : "Paste session ID...").withStyle(ChatFormatting.DARK_GRAY));
        field.setValue(value);
        this.addRenderableWidget(field);

        this.addRenderableWidget(Button.builder(
            Component.literal(offline ? "Create" : "Add"),
            this::submit
        ).bounds(x + 20, y + 62, 95, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("Back"),
            _ -> goBack()
        ).bounds(x + 125, y + 62, 95, 20).build());

        if (offline) {
            String qty = quantityField != null ? quantityField.getValue() : "1";
            quantityField = new EditBox(this.font, x + 20, y + 100, 95, 20, Component.literal("Amount"));
            quantityField.setMaxLength(3);
            quantityField.setHint(Component.literal("Amount (1-100)").withStyle(ChatFormatting.DARK_GRAY));
            quantityField.setValue(qty);
            this.addRenderableWidget(quantityField);

            this.addRenderableWidget(Button.builder(
                Component.literal("Generate"),
                _ -> submitRandom()
            ).bounds(x + 125, y + 100, 95, 20).build());

            this.addRenderableWidget(Button.builder(
                Component.literal("Name style: " + (OfflineAccountManager.nameMode == OfflineAccountManager.NameMode.CHARS ? "Random chars" : "Fake names")),
                b -> {
                    OfflineAccountManager.cycleNameMode();
                    b.setMessage(Component.literal("Name style: " + (OfflineAccountManager.nameMode == OfflineAccountManager.NameMode.CHARS ? "Random chars" : "Fake names")));
                }
            ).bounds(x + 20, y + 124, 200, 20).build());
        }

        this.setFocused(field);
    }

    private void goBack() {
        Minecraft.getInstance().gui.setScreen(accounts);
    }

    private void submit(Button btn) {
        if (offline) submitOffline();
        else submitSsid(btn);
    }

    private void submitSsid(Button btn) {
        String token = field.getValue().trim();
        if (token.isEmpty()) {
            status = Component.literal("Paste a session ID first").withStyle(ChatFormatting.RED);
            return;
        }
        status = Component.literal("Checking...").withStyle(ChatFormatting.YELLOW);
        btn.active = false;

        Thread.ofVirtual().start(() -> {
            String[] info = SessionAPI.getProfileInfo(token);
            Minecraft.getInstance().execute(() -> {
                if (info == null) {
                    status = Component.literal("Invalid session ID!").withStyle(ChatFormatting.RED);
                    btn.active = true;
                    return;
                }
                accounts.addSsidAccount(info[0], token, info[1]);
                goBack();
            });
        });
    }

    private void submitOffline() {
        String name = field.getValue().trim();
        if (name.isEmpty()) {
            status = Component.literal("Enter a username first").withStyle(ChatFormatting.RED);
            return;
        }
        if (OfflineAccountManager.exists(name)) {
            status = Component.literal(name + " already exists").withStyle(ChatFormatting.RED);
            return;
        }
        status = Component.literal("Adding...").withStyle(ChatFormatting.YELLOW);
        accounts.addOfflineAccount(name, this::goBack);
    }

    private void submitRandom() {
        int qty = 1;
        try {
            qty = Math.clamp(Integer.parseInt(quantityField.getValue().trim()), 1, 100);
        } catch (NumberFormatException ignored) {}
        for (int i = 0; i < qty; i++) OfflineAccountManager.createRandom();
        status = Component.literal("Added " + qty + " random account" + (qty == 1 ? "" : "s")).withStyle(ChatFormatting.YELLOW);
        AccountsScreen.loadAccounts(this::goBack);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawStructure(graphics, mouseX, mouseY);

        int x0 = this.width / 2 - 120;
        int y0 = this.height / 2 - 60;
        Ui.box(graphics, x0, y0, 240, offline ? 190 : 120, header, edge);
        graphics.centeredText(
            this.font,
            Component.literal(offline ? "Add offline account" : "Add access token"),
            this.width / 2,
            y0 + 14,
            secondary
        );
        if (offline) {
            graphics.centeredText(
                this.font,
                Component.literal("or generate random accounts"),
                this.width / 2,
                y0 + 88,
                dim
            );
        }
        if (!status.getString().isEmpty()) {
            graphics.centeredText(this.font, status, this.width / 2, offline ? y0 + 152 : y0 + 90, text);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }
}