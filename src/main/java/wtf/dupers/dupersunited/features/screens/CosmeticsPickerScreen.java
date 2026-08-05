package wtf.dupers.dupersunited.features.screens;

import wtf.dupers.dupersunited.api.module.settings.IntSetting;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.features.cosmetics.CosmeticCatalog;
import wtf.dupers.dupersunited.features.cosmetics.CosmeticsFeatureRenderer;
import wtf.dupers.dupersunited.modules.render.CosmeticsModule;
import wtf.dupers.dupersunited.utils.ColorUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CosmeticsPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_MARGIN = 30;
    private static final int PANEL_Y = 16;
    private static final int SIDE_WIDTH = 130;
    private static final int SIDE_PADDING = 10;
    private static final int SIDE_GAP = 5;
    private static final int LIST_Y = 65;
    private static final int ROW_HEIGHT = 18;
    private static final int CONTROL_CONTENT_HEIGHT = 189;
    private static final String[] SETTING_NAMES = {
            "X",
            "Y",
            "Z",
            "Size",
            "Width",
            "Height",
            "Pitch",
            "Yaw",
            "Roll"
    };

    private final CosmeticsModule module;

    private CosmeticCatalog.Slot slot = CosmeticCatalog.Slot.HEAD;
    private String query = "";
    private int scroll;
    private int controlScroll;
    private float previewYaw;
    private float previewPitch;
    private float previewZoom = 1.0f;
    private float previewPanX;
    private float previewPanY;

    public CosmeticsPickerScreen(CosmeticsModule module) {
        super(Text.literal("Cosmetics"));
        this.module = module;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Layout layout = getLayout();
        List<CosmeticCatalog.Item> items = CosmeticCatalog.search(slot, query);

        scroll = MathHelper.clamp(scroll, 0, maxListScroll(layout, items));
        controlScroll = MathHelper.clamp(controlScroll, 0, layout.maxControlScroll());

        renderPanel(context, layout);
        renderTabs(context, layout, mouseX, mouseY);
        renderSearch(context, layout);
        renderList(context, layout, items, mouseX, mouseY);
        renderPreview(context, layout);
        renderControls(context, layout, mouseX, mouseY);
        renderCredit(context, layout, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderPanel(DrawContext context, Layout layout) {
        context.fill(layout.x(), layout.y(), layout.x() + layout.width(),
                layout.y() + layout.height(), ColorUtil.DEEP_SAPPHIRE);
        context.drawCenteredTextWithShadow(textRenderer, "Cosmetics", width / 2,
                layout.y() + 8, ColorUtil.MAUVE);
    }

    private void renderTabs(DrawContext context, Layout layout, int mouseX, int mouseY) {
        int tabWidth = (layout.width() - 24) / 2;
        int tabY = layout.y() + 24;

        drawButton(context, layout.x() + 10, tabY, tabWidth, 15, "Head",
                slot == CosmeticCatalog.Slot.HEAD, mouseX, mouseY);
        drawButton(context, layout.x() + 14 + tabWidth, tabY, tabWidth, 15, "Tail",
                slot == CosmeticCatalog.Slot.TAIL, mouseX, mouseY);
    }

    private void renderSearch(DrawContext context, Layout layout) {
        int searchX = layout.x() + 10;
        int searchY = layout.y() + 44;
        int searchWidth = layout.width() - 20;
        String text = query.isEmpty() ? "Search..." : query;
        int color = query.isEmpty() ? ColorUtil.SUBTEXT : ColorUtil.PALE_NAVY;

        context.fill(searchX, searchY, searchX + searchWidth, searchY + 15, ColorUtil.DEEP_INDIGO);
        context.drawText(textRenderer, text, searchX + 4, searchY + 4, color, false);
    }

    private void renderList(DrawContext context, Layout layout, List<CosmeticCatalog.Item> items,
                            int mouseX, int mouseY) {
        int end = Math.min(items.size(), scroll + visibleRows(layout));
        for (int index = scroll; index < end; index++) {
            CosmeticCatalog.Item item = items.get(index);
            int rowY = layout.y() + LIST_Y + (index - scroll) * ROW_HEIGHT;
            boolean selected = selected().equals(item.name());
            boolean hovered = contains(mouseX, mouseY, layout.x() + 10, rowY,
                    layout.listRight() - layout.x() - 10, 16);
            int color = hovered ? 0x4439C5BB : selected ? 0x5549B8A8 : ColorUtil.DEEP_INDIGO;

            context.fill(layout.x() + 10, rowY, layout.listRight(), rowY + 16, color);
            context.drawText(textRenderer, item.name(), layout.x() + 15, rowY + 4,
                    selected ? ColorUtil.TEAL : ColorUtil.PALE_NAVY, false);

            int available = layout.listRight() - layout.x() - 30 - textRenderer.getWidth(item.name());
            if (available > textRenderer.getWidth(item.creator())) {
                context.drawText(textRenderer, item.creator(),
                        layout.listRight() - 5 - textRenderer.getWidth(item.creator()),
                        rowY + 4, ColorUtil.SUBTEXT, false);
            }
        }
    }

    private void renderPreview(DrawContext context, Layout layout) {
        int previewBottom = layout.previewY() + layout.previewHeight();
        context.fill(layout.sideX(), layout.previewY(), layout.sideX() + SIDE_WIDTH,
                previewBottom, 0xFF111522);
        context.enableScissor(layout.sideX(), layout.previewY(),
                layout.sideX() + SIDE_WIDTH, previewBottom);
        if (client != null && client.player != null) {
            drawPlayerPreview(context, layout);
        }
        context.disableScissor();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void drawPlayerPreview(DrawContext context, Layout layout) {
        EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(client.player);
        EntityRenderState state = renderer.getAndUpdateRenderState(client.player, 1.0f);

        if (state instanceof LivingEntityRenderState living) {
            living.bodyYaw = 180.0f + previewYaw;
            living.relativeHeadYaw = 0;
            living.pitch = previewPitch;
            living.width /= living.baseScale;
            living.height /= living.baseScale;
            living.baseScale = 1.0f;
        }

        state.light = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;

        float size = 42.0f * previewZoom;
        Vector3f offset = new Vector3f(
                previewPanX / size, state.height / 2 + previewPanY / size, 0);
        CosmeticsFeatureRenderer.preview(() -> context.addEntity(
                state,
                size,
                offset,
                new Quaternionf().rotateZ((float) Math.PI),
                new Quaternionf(),
                layout.sideX(),
                layout.previewY(),
                layout.sideX() + SIDE_WIDTH,
                layout.previewY() + layout.previewHeight()
        ));
    }

    private void renderControls(DrawContext context, Layout layout, int mouseX, int mouseY) {
        if (layout.controlBottom() <= layout.controlTop()) return;

        context.enableScissor(layout.sideX(), layout.controlTop(),
                layout.sideX() + SIDE_WIDTH, layout.controlBottom());
        drawControls(context, layout.sideX(), layout.controlTop() - controlScroll,
                SIDE_WIDTH, mouseX, mouseY);
        context.disableScissor();
    }

    private void drawControls(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        String title = slot == CosmeticCatalog.Slot.HEAD ? "Head position" : "Tail position";
        context.drawCenteredTextWithShadow(textRenderer, title, x + width / 2, y, ColorUtil.MAUVE);

        List<IntSetting> settings = module.settings(slot);
        for (int index = 0; index < SETTING_NAMES.length; index++) {
            int rowY = y + 13 + index * 16;
            String value = Integer.toString(settings.get(index).getValue());

            context.fill(x, rowY, x + width, rowY + 14, ColorUtil.DEEP_INDIGO);
            context.drawText(textRenderer, SETTING_NAMES[index], x + 4, rowY + 3, ColorUtil.PALE_NAVY, false);
            context.drawText(textRenderer, "-", x + width - 43, rowY + 3, ColorUtil.TEAL, false);
            context.drawText(textRenderer, value,
                    x + width - 24 - textRenderer.getWidth(value) / 2,
                    rowY + 3, ColorUtil.PALE_NAVY, false);
            context.drawText(textRenderer, "+", x + width - 8, rowY + 3, ColorUtil.TEAL, false);
        }

        int resetY = y + 158;
        drawButton(context, x, resetY, width, 14, "Reset", false, mouseX, mouseY);
        drawButton(context, x, resetY + 17, width, 14,
                "Animate: " + module.animate.getValue(), module.animate.getValue(), mouseX, mouseY);
    }

    private void renderCredit(DrawContext context, Layout layout, int mouseX, int mouseY) {
        CosmeticCatalog.Item item = CosmeticCatalog.named(slot, selected());
        String creditText = "Cosmetic by " + item.creator();
        int textX = layout.x() + 10;
        int textY = layout.y() + layout.height() - 20;
        context.drawText(textRenderer, creditText, textX, textY, ColorUtil.SUBTEXT, false);

        if (!item.source().isBlank()) {
            int buttonWidth = 60;
            int buttonX = textX + textRenderer.getWidth(creditText) + 4;
            int buttonY = textY - 2;
            drawButton(context, buttonX, buttonY, buttonWidth, 14, "Source", false, mouseX, mouseY);
        }
    }

    private void drawButton(DrawContext context, int x, int y, int width, int height,
                            String text, boolean active, int mouseX, int mouseY) {
        int color = active
                ? 0x6649B8A8
                : contains(mouseX, mouseY, x, y, width, height) ? 0x4439C5BB : ColorUtil.DEEP_INDIGO;
        context.fill(x, y, x + width, y + height, color);
        context.drawCenteredTextWithShadow(textRenderer, text, x + width / 2, y + 4,
                active ? ColorUtil.TEAL : ColorUtil.PALE_NAVY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Layout layout = getLayout();
        int mouseX = (int) click.x();
        int mouseY = (int) click.y();

        if (selectTab(layout, mouseX, mouseY)) return true;
        if (selectCosmetic(layout, mouseX, mouseY)) return true;
        if (changeControl(layout, mouseX, mouseY)) return true;

        CosmeticCatalog.Item selectedItem = CosmeticCatalog.named(slot, selected());
        if (!selectedItem.source().isBlank()) {
            String creditText = "Cosmetic by " + selectedItem.creator();
            int textX = layout.x() + 10;
            int buttonWidth = 60;
            int buttonX = textX + textRenderer.getWidth(creditText) + 4;
            int buttonY = layout.y() + layout.height() - 22;
            if (contains(mouseX, mouseY, buttonX, buttonY, buttonWidth, 14)) {
                openSource(selectedItem);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private boolean selectTab(Layout layout, int mouseX, int mouseY) {
        int tabWidth = (layout.width() - 24) / 2;
        int tabY = layout.y() + 24;

        if (contains(mouseX, mouseY, layout.x() + 10, tabY, tabWidth, 15)) {
            slot = CosmeticCatalog.Slot.HEAD;
        } else if (contains(mouseX, mouseY, layout.x() + 14 + tabWidth, tabY, tabWidth, 15)) {
            slot = CosmeticCatalog.Slot.TAIL;
        } else {
            return false;
        }

        scroll = 0;
        controlScroll = 0;
        return true;
    }

    private boolean selectCosmetic(Layout layout, int mouseX, int mouseY) {
        List<CosmeticCatalog.Item> items = CosmeticCatalog.search(slot, query);
        int end = Math.min(items.size(), scroll + visibleRows(layout));

        for (int index = scroll; index < end; index++) {
            int rowY = layout.y() + LIST_Y + (index - scroll) * ROW_HEIGHT;
            if (contains(mouseX, mouseY, layout.x() + 10, rowY,
                    layout.listRight() - layout.x() - 10, 16)) {
                select(items.get(index).name());
                return true;
            }
        }

        return false;
    }

    private boolean changeControl(Layout layout, int mouseX, int mouseY) {
        if (mouseY < layout.controlTop() || mouseY >= layout.controlBottom()) return false;

        int controlY = layout.controlTop() - controlScroll;
        List<IntSetting> settings = module.settings(slot);

        for (int index = 0; index < settings.size(); index++) {
            int rowY = controlY + 13 + index * 16;
            if (!contains(mouseX, mouseY, layout.sideX(), rowY, SIDE_WIDTH, 14)) continue;

            int step = index < 3 ? 1 : 5;
            if (mouseX >= layout.sideX() + SIDE_WIDTH - 48
                    && mouseX < layout.sideX() + SIDE_WIDTH - 30) {
                adjust(settings.get(index), -step);
            } else if (mouseX >= layout.sideX() + SIDE_WIDTH - 18) {
                adjust(settings.get(index), step);
            }
            ConfigManager.save();
            return true;
        }

        int resetY = controlY + 158;
        if (contains(mouseX, mouseY, layout.sideX(), resetY, SIDE_WIDTH, 14)) {
            for (int index = 0; index < settings.size(); index++) {
                settings.get(index).setValue(index >= 3 && index <= 5 ? 100 : 0);
            }
            ConfigManager.save();
            return true;
        }

        if (contains(mouseX, mouseY, layout.sideX(), resetY + 17, SIDE_WIDTH, 14)) {
            module.animate.toggle();
            ConfigManager.save();
            return true;
        }

        return false;
    }

    private static void adjust(IntSetting setting, int amount) {
        setting.setValue(MathHelper.clamp(setting.getValue() + amount, setting.getMin(), setting.getMax()));
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        Layout layout = getLayout();
        if (click.x() <= layout.listRight()) {
            return super.mouseDragged(click, deltaX, deltaY);
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            previewYaw = (previewYaw - (float) deltaX * 1.5f) % 360.0f;
            previewPitch = MathHelper.clamp(previewPitch + (float) deltaY, -80.0f, 80.0f);
        } else if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            previewPanX += (float) deltaX;
            previewPanY += (float) deltaY;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        Layout layout = getLayout();
        int direction = (int) Math.signum(verticalAmount);

        if (mouseX > layout.listRight() && mouseY >= layout.controlTop() && layout.maxControlScroll() > 0) {
            controlScroll = MathHelper.clamp(
                    controlScroll - direction * 12, 0, layout.maxControlScroll());
        } else if (mouseX > layout.listRight()) {
            previewZoom = MathHelper.clamp(previewZoom + (float) verticalAmount * 0.1f, 0.55f, 1.8f);
        } else {
            scroll = Math.max(0, scroll - direction);
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (input.isValidChar()) {
            query += input.asString();
            scroll = 0;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            scroll = 0;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        ConfigManager.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private Layout getLayout() {
        int panelWidth = Math.min(PANEL_WIDTH, width - PANEL_MARGIN);
        int panelX = (width - panelWidth) / 2;
        int panelHeight = height - PANEL_Y * 2;
        int sideX = panelX + panelWidth - SIDE_PADDING - SIDE_WIDTH;
        int listRight = sideX - SIDE_GAP;
        int previewY = PANEL_Y + LIST_Y;
        int previewHeight = Math.max(90, panelHeight - 325);
        int controlTop = previewY + previewHeight + 5;
        int controlBottom = PANEL_Y + panelHeight - 38;
        int controlHeight = Math.max(0, controlBottom - controlTop);
        int maxControlScroll = Math.max(0, CONTROL_CONTENT_HEIGHT - controlHeight);

        return new Layout(panelX, PANEL_Y, panelWidth, panelHeight, listRight, sideX,
                previewY, previewHeight, controlTop, controlBottom, maxControlScroll);
    }

    private static int visibleRows(Layout layout) {
        return Math.max(1, (layout.height() - 105) / ROW_HEIGHT);
    }

    private static int maxListScroll(Layout layout, List<CosmeticCatalog.Item> items) {
        return Math.max(0, items.size() - visibleRows(layout));
    }

    private static boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private String selected() {
        return slot == CosmeticCatalog.Slot.HEAD ? module.head.getValue() : module.tail.getValue();
    }

    private void select(String value) {
        if (slot == CosmeticCatalog.Slot.HEAD) {
            module.head.setValue(value);
        } else {
            module.tail.setValue(value);
        }
        module.setEnabled(!module.head.getValue().equals("None") || !module.tail.getValue().equals("None"));
        ConfigManager.save();
    }

    private static void openSource(CosmeticCatalog.Item item) {
        if (!item.source().isBlank()) {
            Util.getOperatingSystem().open(item.source());
        }
    }

    private record Layout(
            int x,
            int y,
            int width,
            int height,
            int listRight,
            int sideX,
            int previewY,
            int previewHeight,
            int controlTop,
            int controlBottom,
            int maxControlScroll
    ) {}
}
