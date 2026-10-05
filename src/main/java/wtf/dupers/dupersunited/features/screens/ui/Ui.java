package wtf.dupers.dupersunited.features.screens.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import wtf.dupers.dupersunited.features.proxies.ProxyScreen;
import wtf.dupers.dupersunited.features.screens.ClickGui;
import wtf.dupers.dupersunited.features.screens.mainmenu.CapeScreen;
import wtf.dupers.dupersunited.features.screens.mainmenu.KeybindScreen;
import wtf.dupers.dupersunited.features.screens.mainmenu.SettingsScreen;
import wtf.dupers.dupersunited.features.account.AccountsScreen;

import static wtf.dupers.dupersunited.MainClient.mc;

public final class Ui {
    public static void dim(GuiGraphicsExtractor g, int w, int h) {
        g.fill(0, 0, w, h, Theme.backdrop);
    }

    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        box(g, x, y, w, h, Theme.surface, Theme.border);
    }

    public static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int bg, int border) {
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }

    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        for (int i = 4; i >= 1; i--) {
            g.fill(x - i, y - i, x + w + i, y + h + i, (int) (255 * 0.055f * i) << 24);
        }
    }

    public static void header(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, Theme.header);
        g.fill(x, y + h - 1, x + w, y + h, Theme.border);
    }

    public static void slider(GuiGraphicsExtractor g, int x, int y, int w, int h, float pct) {
        int fw = (int) (w * Math.clamp(pct, 0, 1));
        g.fill(x, y, x + w, y + h, Theme.border);
        if (fw > 0) g.fill(x, y, x + fw, y + h, Theme.accent);
    }

    public static final int TAB_H = 18;
    public static final int TAB_Y = 4;
    public static final int TAB_W = 70;
    public static final int TAB_GAP = 4;

    public static String[] tabNames() {
        if (mc.player == null) return new String[]{"Modules", "Capes", "Accounts", "Proxies", "Keybinds", "Settings"};
        return new String[]{"Modules", "Capes", "Proxies", "Keybinds", "Settings"};
    }

    public static int tabHit(int screenWidth, int mx, int my) {
        String[] tabs = tabNames();
        int x = (screenWidth - tabs.length * TAB_W - (tabs.length - 1) * TAB_GAP) / 2;
        for (int i = 0; i < tabs.length; i++) {
            if (mx >= x && mx < x + TAB_W && my >= TAB_Y && my < TAB_Y + TAB_H) return i;
            x += TAB_W + TAB_GAP;
        }
        return -1;
    }

    public static void openTab(Screen parent, int i) {
        switch (tabNames()[i]) {
            case "Modules" -> {
                if (parent instanceof ClickGui cg) mc.gui.setScreen(cg);
                else mc.gui.setScreen(new ClickGui(parent));
            }
            case "Capes" -> mc.gui.setScreen(new CapeScreen(parent));
            case "Accounts" -> mc.gui.setScreen(new AccountsScreen(parent));
            case "Proxies" -> mc.gui.setScreen(new ProxyScreen(parent));
            case "Keybinds" -> mc.gui.setScreen(new KeybindScreen(parent));
            case "Settings" -> mc.gui.setScreen(new SettingsScreen(parent));
            default -> { }
        }
    }

    public static void drawTabs(GuiGraphicsExtractor g, Font font, int screenWidth, int mx, int my, String active) {
        String[] tabs = tabNames();
        int x = (screenWidth - tabs.length * TAB_W - (tabs.length - 1) * TAB_GAP) / 2;
        for (String tab : tabs) {
            boolean isActive = tab.equals(active);
            boolean hov = mx >= x && mx < x + TAB_W && my >= TAB_Y && my < TAB_Y + TAB_H;
            g.fill(x, TAB_Y, x + TAB_W, TAB_Y + TAB_H, isActive ? Theme.header : hov ? Theme.hover : Theme.surface);
            g.fill(x, TAB_Y + TAB_H - 1, x + TAB_W, TAB_Y + TAB_H, isActive ? Theme.accent : Theme.border);
            int tw = font.width(tab);
            g.text(font, tab, x + (TAB_W - tw) / 2, TAB_Y + (TAB_H - 7) / 2, isActive ? Theme.accent : Theme.muted, false);
            x += TAB_W + TAB_GAP;
        }
    }

    public static boolean handleTabClick(int screenWidth, Screen parent, int mx, int my, String active) {
        int i = tabHit(screenWidth, mx, my);
        if (i < 0) return false;
        if (!tabNames()[i].equals(active)) openTab(parent, i);
        return true;
    }
}