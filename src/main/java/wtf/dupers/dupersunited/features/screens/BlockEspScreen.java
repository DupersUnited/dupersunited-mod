package wtf.dupers.dupersunited.features.screens;

import wtf.dupers.dupersunited.modules.render.BlockEspModule;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.utils.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlockEspScreen extends Screen {

    private static final int BG = ColorUtil.DEEP_SAPPHIRE;
    private static final int SURFACE = ColorUtil.DEEP_INDIGO;
    private static final int OVERLAY = ColorUtil.FADED_INDIGO;
    private static final int MAUVE = ColorUtil.MAUVE;
    private static final int TEXT_COL = ColorUtil.PALE_NAVY;
    private static final int SUBTEXT = ColorUtil.SUBTEXT;
    private static final int TEAL = ColorUtil.TEAL;

    private static final int CELL = 36;
    private static final int COLS = 8;
    private static final int PAD = 10;
    private static final int SEARCH_TOP_OFF = 20;
    private static final int SEARCH_H = 13;
    private static final int AFTER_SEARCH_GAP = 5;
    private static final int HEADER_AFTER_CLEAR_GAP = 6;
    private static final String CLEAR_BTN_LABEL = "Clear";
    private static final int CLEAR_BTN_H = 14;
    private static final int HEADER_H = SEARCH_TOP_OFF + SEARCH_H + AFTER_SEARCH_GAP + CLEAR_BTN_H + HEADER_AFTER_CLEAR_GAP;
    private static final int FOOTER_H = 16;
    private final List<Block> allBlocks = new ArrayList<>();
    private final List<Block> shown = new ArrayList<>();
    private String searchQuery = "";
    private int scrollRow = 0;

    private int cursorPos = 0;
    private long lastBlink = 0;
    private boolean cursorVisible = true;

    private boolean gridDragActive;
    private boolean gridDragAdd;
    private int gridDragLastShownIndex = -1;

    public BlockEspScreen() {
        super(Text.literal("Block ESP"));
        Registries.BLOCK.forEach(b -> {
            if (b != Blocks.AIR && b != Blocks.VOID_AIR && b != Blocks.CAVE_AIR
                && (!b.asItem().getDefaultStack().isEmpty() || BlockEspModule.hasMarker(b))) {
                allBlocks.add(b);
            }
        });
        rebuildShown();
    }

    private void rebuildShown() {
        shown.clear();
        String q = searchQuery.toLowerCase(Locale.ROOT);
        for (Block b : allBlocks) {
            if (q.isEmpty()
                || b.getName().getString().toLowerCase(Locale.ROOT).contains(q)
                || Registries.BLOCK.getId(b).toString().contains(q)) {
                shown.add(b);
            }
        }
        scrollRow = 0;
    }

    private int panelX() { return (width - COLS * CELL - PAD * 2) / 2; }
    private int panelW() { return COLS * CELL + PAD * 2; }

    private int visibleGridRows(int ph) {
        return (ph - HEADER_H - FOOTER_H - PAD) / CELL;
    }

    private int gridHitShownIndex(int mx, int my, int px, int pw, int py, int ph) {
        int gridY = py + HEADER_H;
        int visRows = visibleGridRows(ph);
        int startIdx = scrollRow * COLS;
        int endIdx = Math.min(startIdx + visRows * COLS, shown.size());
        for (int i = startIdx; i < endIdx; i++) {
            int col = (i - startIdx) % COLS;
            int row = (i - startIdx) / COLS;
            int cx = px + PAD + col * CELL;
            int cy = gridY + PAD + row * CELL;
            if (mx >= cx && mx < cx + CELL - 2 && my >= cy && my < cy + CELL - 2) {
                return i;
            }
        }
        return -1;
    }

    private void applyGridDragRange(int fromShown, int toShown) {
        if (shown.isEmpty()) {
            return;
        }
        int a = Math.min(fromShown, toShown);
        int b = Math.max(fromShown, toShown);
        a = Math.max(0, Math.min(a, shown.size() - 1));
        b = Math.max(0, Math.min(b, shown.size() - 1));
        boolean changed = false;
        for (int j = a; j <= b; j++) {
            Block block = shown.get(j);
            if (gridDragAdd) {
                if (BlockEspModule.selectedBlocks.add(block)) {
                    changed = true;
                }
            } else {
                if (BlockEspModule.selectedBlocks.remove(block)) {
                    changed = true;
                }
            }
        }
        if (changed) {
            ConfigManager.save();
        }
    }

    private void gridDragMoveToIndex(int idx) {
        if (idx < 0 || !gridDragActive) {
            return;
        }
        applyGridDragRange(gridDragLastShownIndex, idx);
        gridDragLastShownIndex = idx;
    }

    private int clearButtonWidth() {
        return textRenderer.getWidth(CLEAR_BTN_LABEL) + 10;
    }

    private int clearButtonX(int px, int pw) {
        return px + (pw - clearButtonWidth()) / 2;
    }

    private int clearButtonY(int py) {
        return py + SEARCH_TOP_OFF + SEARCH_H + AFTER_SEARCH_GAP;
    }

    private void drawClearButton(DrawContext ctx, int px, int pw, int py, int mouseX, int mouseY) {
        int x = clearButtonX(px, pw);
        int y = clearButtonY(py);
        int w = clearButtonWidth();
        boolean hov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CLEAR_BTN_H;
        ctx.fill(x, y, x + w, y + CLEAR_BTN_H, hov ? 0x44CDD6F4 : OVERLAY);
        ctx.fill(x, y, x + w, y + 1, OVERLAY);
        ctx.fill(x, y + CLEAR_BTN_H - 1, x + w, y + CLEAR_BTN_H, OVERLAY);
        ctx.fill(x, y, x + 1, y + CLEAR_BTN_H, OVERLAY);
        ctx.fill(x + w - 1, y, x + w, y + CLEAR_BTN_H, OVERLAY);
        int lw = textRenderer.getWidth(CLEAR_BTN_LABEL);
        ctx.drawText(textRenderer, CLEAR_BTN_LABEL, x + (w - lw) / 2, y + 3, TEAL, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        if (now - lastBlink > 530) { cursorVisible = !cursorVisible; lastBlink = now; }

        int px = panelX(), pw = panelW(), py = 5, ph = height - 10;
        int gridY   = py + HEADER_H;
        int visRows = visibleGridRows(ph);

        ctx.fill(px, py, px + pw, py + ph, BG);
        ctx.fill(px, py, px + pw, py + 1, SURFACE);
        ctx.fill(px, py + ph - 1, px + pw, py + ph, SURFACE);
        ctx.fill(px, py, px + 1, py + ph, SURFACE);
        ctx.fill(px + pw - 1, py, px + pw, py + ph, SURFACE);

        ctx.fill(px, py, px + pw, py + HEADER_H, 0xFF181825);
        ctx.fill(px, py + HEADER_H - 1, px + pw, py + HEADER_H, SURFACE);
        ctx.drawTextWithShadow(textRenderer, "Block ESP - Select Blocks", px + PAD, py + 7, MAUVE);

        int sfY = py + SEARCH_TOP_OFF, sfX = px + PAD, sfW = pw - PAD * 2;
        ctx.fill(sfX, sfY, sfX + sfW, sfY + SEARCH_H, SURFACE);
        ctx.fill(sfX, sfY, sfX + sfW, sfY + 1, OVERLAY);
        ctx.fill(sfX, sfY + SEARCH_H - 1, sfX + sfW, sfY + SEARCH_H, OVERLAY);
        ctx.fill(sfX, sfY, sfX + 1, sfY + SEARCH_H, OVERLAY);
        ctx.fill(sfX + sfW - 1, sfY, sfX + sfW, sfY + SEARCH_H, OVERLAY);

        int textX = sfX + 3, textY = sfY + 3;
        if (searchQuery.isEmpty()) {
            ctx.drawText(textRenderer, "Search...", textX, textY, SUBTEXT, false);
        } else {
            ctx.drawText(textRenderer, searchQuery, textX, textY, TEXT_COL, false);
        }
        if (cursorVisible) {
            int cx = textX + textRenderer.getWidth(searchQuery.substring(0, Math.min(cursorPos, searchQuery.length())));
            ctx.fill(cx, textY - 1, cx + 1, textY + 9, TEAL);
        }

        drawClearButton(ctx, px, pw, py, mouseX, mouseY);

        int startIdx = scrollRow * COLS;
        int endIdx   = Math.min(startIdx + visRows * COLS, shown.size());
        int visibleCount = endIdx - startIdx;
        int gridRows = visibleCount == 0 ? 0 : (visibleCount + COLS - 1) / COLS;
        int gridBottomEdge;
        if (gridRows == 0) {
            gridBottomEdge = gridY + PAD;
        } else {
            gridBottomEdge = gridY + PAD + gridRows * CELL - 2;
        }

        for (int i = startIdx; i < endIdx; i++) {
            Block block = shown.get(i);
            int col = (i - startIdx) % COLS;
            int row = (i - startIdx) / COLS;
            int cx = px + PAD + col * CELL;
            int cy = gridY + PAD + row * CELL;
            String blockId = Registries.BLOCK.getId(block).toString();
            boolean sel = BlockEspModule.selectedBlocks.contains(block);
            boolean hov = mouseX >= cx && mouseX < cx + CELL - 2
                && mouseY >= cy && mouseY < cy + CELL - 2;

            ctx.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, sel ? 0x5594E2D5 : OVERLAY);

            if (sel) {
                ctx.fill(cx, cy,cx + CELL - 2, cy + 1, TEAL);
                ctx.fill(cx, cy + CELL - 3, cx + CELL - 2, cy + CELL - 2, TEAL);
                ctx.fill(cx, cy, cx + 1,cy + CELL - 2, TEAL);
                ctx.fill(cx + CELL - 3, cy,cx + CELL - 2, cy + CELL - 2, TEAL);
            }

            if (hov) ctx.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, 0x33CDD6F4);

            ItemStack stack = block.asItem().getDefaultStack();
            if (stack.isEmpty()) {
                ctx.drawCenteredTextWithShadow(textRenderer, "?", cx + 17, cy + 13, TEXT_COL);
            } else {
                ctx.drawItem(stack, cx + 9, cy + 9);
            }

            if (hov) {
                ctx.drawTooltip(textRenderer, List.of(
                    Text.literal(block.getName().getString()).withColor(TEXT_COL),
                    Text.literal(blockId).withColor(SUBTEXT)
                ), mouseX, mouseY);
            }
        }

        String footer = BlockEspModule.selectedBlocks.size() + " Selected | " + shown.size()
            + " Shown | Scroll to Navigate";
        int footerW = textRenderer.getWidth(footer);
        int footerX = px + (pw - footerW) / 2;
        int panelBottom = py + ph;
        int gap = panelBottom - gridBottomEdge;
        int fh = textRenderer.fontHeight;
        int footerY;
        if (gap <= fh) {
            footerY = Math.max(gridBottomEdge, panelBottom - fh - PAD);
        } else {
            footerY = gridBottomEdge + (gap - fh) / 2;
        }
        ctx.drawText(textRenderer, footer, footerX, footerY, SUBTEXT, false);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        int px = panelX(), pw = panelW(), py = 5, ph = height - 10;
        int gridY   = py + HEADER_H;
        int visRows = visibleGridRows(ph);

        int sfY = py + SEARCH_TOP_OFF, sfX = px + PAD, sfW = pw - PAD * 2;
        if (mx >= sfX && mx < sfX + sfW && my >= sfY && my < sfY + SEARCH_H) {
            int relX = mx - (sfX + 3), best = searchQuery.length(), bestDist = Integer.MAX_VALUE;
            for (int i = 0; i <= searchQuery.length(); i++) {
                int d = Math.abs(textRenderer.getWidth(searchQuery.substring(0, i)) - relX);
                if (d < bestDist) { bestDist = d; best = i; }
            }
            cursorPos = best;
            return true;
        }

        int cxBtn = clearButtonX(px, pw);
        int cyBtn = clearButtonY(py);
        int cwBtn = clearButtonWidth();
        if (mx >= cxBtn && mx < cxBtn + cwBtn && my >= cyBtn && my < cyBtn + CLEAR_BTN_H) {
            BlockEspModule.selectedBlocks.clear();
            ConfigManager.save();
            return true;
        }

        int startIdx = scrollRow * COLS;
        int endIdx   = Math.min(startIdx + visRows * COLS, shown.size());
        for (int i = startIdx; i < endIdx; i++) {
            int col = (i - startIdx) % COLS;
            int row = (i - startIdx) / COLS;
            int cx  = px + PAD + col * CELL;
            int cy  = gridY + PAD + row * CELL;
            if (mx >= cx && mx < cx + CELL - 2 && my >= cy && my < cy + CELL - 2) {
                if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    return true;
                }
                Block block = shown.get(i);
                gridDragAdd = !BlockEspModule.selectedBlocks.contains(block);
                gridDragActive = true;
                gridDragLastShownIndex = i;
                applyGridDragRange(i, i);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
        if (gridDragActive && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int px = panelX(), pw = panelW(), py = 5, ph = height - 10;
            int idx = gridHitShownIndex((int) click.x(), (int) click.y(), px, pw, py, ph);
            if (idx >= 0) {
                gridDragMoveToIndex(idx);
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        gridDragActive = false;
        gridDragLastShownIndex = -1;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int ph      = height - 10;
        int visRows = visibleGridRows(ph);
        int maxRow  = Math.max(0, (int) Math.ceil((double) shown.size() / COLS) - visRows);
        scrollRow   = Math.max(0, Math.min(scrollRow - (int) Math.signum(verticalAmount), maxRow));
        if (gridDragActive) {
            int px = panelX(), pw = panelW(), py = 5;
            int idx = gridHitShownIndex((int) mouseX, (int) mouseY, px, pw, py, ph);
            if (idx >= 0) {
                gridDragMoveToIndex(idx);
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        int key = input.getKeycode();
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorPos > 0) {
                searchQuery = searchQuery.substring(0, cursorPos - 1) + searchQuery.substring(cursorPos);
                cursorPos--;
                rebuildShown();
            }
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
            if (cursorPos < searchQuery.length()) {
                searchQuery = searchQuery.substring(0, cursorPos) + searchQuery.substring(cursorPos + 1);
                rebuildShown();
            }
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT  && cursorPos > 0) { cursorPos--; return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT && cursorPos < searchQuery.length()) { cursorPos++; return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME) { cursorPos = 0; return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_END)  { cursorPos = searchQuery.length(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        if (input.isValidChar()) {
            searchQuery = searchQuery.substring(0, cursorPos) + input.asString() + searchQuery.substring(cursorPos);
            cursorPos++;
            rebuildShown();
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        ConfigManager.save();
        assert client != null;
        client.setScreen(null);
    }

    @Override
    public boolean shouldPause() { return false; }
}
