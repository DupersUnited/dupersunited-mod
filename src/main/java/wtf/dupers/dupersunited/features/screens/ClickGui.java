package wtf.dupers.dupersunited.features.screens;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.features.screens.hud.HudEditorScreen;
import wtf.dupers.dupersunited.features.screens.macroscreen.ChatMacroScreen;
import wtf.dupers.dupersunited.features.screens.mainmenu.KeybindScreen;
import wtf.dupers.dupersunited.features.screens.ui.DuScreen;
import wtf.dupers.dupersunited.features.screens.ui.Theme;
import wtf.dupers.dupersunited.features.screens.ui.Ui;
import wtf.dupers.dupersunited.features.account.AccountsScreen;
import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.keybinds.KeybindManager;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.*;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.features.screens.ui.PrideTheme.*;

public class ClickGui extends DuScreen {

    private static final int PW = 130;
    private static final int HEADER_H = 16;
    private static final int MOD_H = 13;
    private static final int SET_H = 13;
    private static final int SLD_H = 4;
    private static final int PAD = 6;
    private static final int PANEL_TOP = 30;

    private final List<Panel> panels = new ArrayList<>();
    private HudPanel hudPanel;

    // <user customization>
    public static final Map<String, String> customCategories = new LinkedHashMap<>();
    public static final Map<String, Vector2i> categoryPositions = new HashMap<>();
    public static final Map<String, Set<String>> categoryExpandedModules = new HashMap<>();
    public static final Object2BooleanMap<String> categoryCollapsed = new Object2BooleanOpenHashMap<>();
    public static @Nullable Vector2i hudPanelPosition = null;
    // </user customization>

    private Panel focusedPanel = null;
    private String focusedMod = null;
    private String focusedSet = null;
    private int cursorPos = 0;
    private int selectionAnchor = -1;

    private String hoveredDescription = null;
    private String rebindingModule = null;
    private Keybind rebindingKeybind = null;
    private String searchQuery = "";
    private boolean searchFocused = false;

    private boolean hasSelection() {
        return selectionAnchor != -1 && selectionAnchor != cursorPos;
    }

    private void clearSelection() {
        selectionAnchor = -1;
    }

    private boolean isCtrlDown() {
        long handle = mc.getWindow().handle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private int selStart() {
        return Math.min(cursorPos, selectionAnchor);
    }

    private int selEnd() {
        return Math.max(cursorPos, selectionAnchor);
    }

    // there has to be something that already exists for this
    private interface TextBuffer {
        String get();
        void set(String value);
        int cap();
    }

    private boolean editKey(TextBuffer buf, int keyCode) {
        boolean ctrl = isCtrlDown();
        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            selectionAnchor = 0;
            cursorPos = buf.get().length();
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_C) {
            if (hasSelection()) mc.keyboardHandler.setClipboard(buf.get().substring(selStart(), selEnd()));
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            String cb = mc.keyboardHandler.getClipboard();
            if (!cb.isEmpty()) {
                String cur = buf.get();
                if (hasSelection()) {
                    buf.set(cur.substring(0, selStart()) + cur.substring(selEnd()));
                    cursorPos = selStart();
                    clearSelection();
                    cur = buf.get();
                }
                cursorPos = Math.min(cursorPos, cur.length());
                int room = buf.cap() - cur.length();
                if (room > 0) {
                    String paste = cb.substring(0, Math.min(room, cb.length()));
                    buf.set(cur.substring(0, cursorPos) + paste + cur.substring(cursorPos));
                    cursorPos = Math.min(cursorPos + paste.length(), buf.get().length());
                }
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            String cur = buf.get();
            cursorPos = Math.min(cursorPos, cur.length());
            if (hasSelection()) {
                buf.set(cur.substring(0, selStart()) + cur.substring(selEnd()));
                cursorPos = selStart();
                clearSelection();
            } else if (cursorPos > 0) {
                buf.set(cur.substring(0, cursorPos - 1) + cur.substring(cursorPos));
                cursorPos--;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            String cur = buf.get();
            cursorPos = Math.min(cursorPos, cur.length());
            if (hasSelection()) {
                buf.set(cur.substring(0, selStart()) + cur.substring(selEnd()));
                cursorPos = selStart();
                clearSelection();
            } else if (cursorPos < cur.length()) {
                buf.set(cur.substring(0, cursorPos) + cur.substring(cursorPos + 1));
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (hasSelection()) { cursorPos = selStart(); clearSelection(); }
            else if (cursorPos > 0) cursorPos--;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (hasSelection()) { cursorPos = selEnd(); clearSelection(); }
            else if (cursorPos < buf.get().length()) cursorPos++;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPos = 0;
            clearSelection();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPos = buf.get().length();
            clearSelection();
            return true;
        }
        return true;
    }

    private void editChar(TextBuffer buf, String ins) {
        if (hasSelection()) {
            String cur = buf.get();
            buf.set(cur.substring(0, selStart()) + cur.substring(selEnd()));
            cursorPos = selStart();
            clearSelection();
        }
        String cur = buf.get();
        cursorPos = Math.min(cursorPos, cur.length());
        if (cur.length() + ins.length() <= buf.cap()) {
            buf.set(cur.substring(0, cursorPos) + ins + cur.substring(cursorPos));
            cursorPos++;
        }
    }

    private TextBuffer searchBuffer() {
        return new TextBuffer() {
            public String get() { return searchQuery; }
            public void set(String v) { searchQuery = v; }
            public int cap() { return Integer.MAX_VALUE; }
        };
    }

    private TextBuffer settingBuffer(StringSetting ss) {
        return new TextBuffer() {
            public String get() { return ss.getValue(); }
            public void set(String v) { ss.setValue(v); }
            public int cap() { return ss.getMaxLength(); }
        };
    }

    private void setFocus(Panel panel, String modName, String setName) {
        focusedPanel = panel;
        focusedMod = modName;
        focusedSet = setName;
        searchFocused = false;
    }

    private boolean isFocused(Panel panel, String modName, String setName) {
        return panel == focusedPanel && modName.equals(focusedMod) && setName.equals(focusedSet);
    }

    private String getKeyName(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        if (key >= 0 && key <= 7) return "MB" + (key + 1);
        String name = GLFW.glfwGetKeyName(key, 0);
        return name != null ? name.toUpperCase() : "K" + key;
    }

    private class HudPanel {
        int x, y;
        boolean dragging;
        int dox, doy;

        HudPanel(int x, int y) {
            if (hudPanelPosition == null) hudPanelPosition = new Vector2i(x, y);
            this.x = hudPanelPosition.x();
            this.y = hudPanelPosition.y();
        }

        void draw(GuiGraphicsExtractor graphics, int mx, int my) {
            int ph = HEADER_H + MOD_H * 3;
            Ui.shadow(graphics, x, y, PW, ph);
            Ui.box(graphics, x, y, PW, ph);
            Ui.header(graphics, x, y, PW, HEADER_H);
            if (PRIDE) graphics.text(font, prideStyle("Configs"), x + PAD, y + (HEADER_H - 7) / 2, -1);
            else graphics.text(font, "Configs", x + PAD, y + (HEADER_H - 7) / 2, Theme.text);
            int ry = y + HEADER_H;

            hudRow(graphics, "Edit HUD", Theme.info, ry, mx, my);
            ry += MOD_H;
            hudRow(graphics, "Keybinds", Theme.info, ry, mx, my);
            ry += MOD_H;
            hudRow(graphics, "ChatMacros", Theme.info, ry, mx, my);

            Keybind kb = KeybindManager.getRegisteredKeybinds().get("keybinds");
            if (kb != null) {
                String kbStr = (kb == rebindingKeybind) ? "..." : "[" + getKeyName(kb.getKeyCode()) + "]";
                int kbW = font.width(kbStr);
                graphics.text(font, kbStr, x + PW - PAD - kbW, ry + 3, Theme.dim, false);
            }
        }

        void hudRow(GuiGraphicsExtractor graphics, String label, int color, int ry, int mx, int my) {
            boolean hov = mx >= x && mx < x + PW && my >= ry && my < ry + MOD_H;
            if (hov) graphics.fill(x, ry, x + PW, ry + MOD_H, Theme.hover);
            graphics.fill(x, ry, x + 2, ry + MOD_H, PRIDE ? C_PRIDE_1 : color);
            if (PRIDE) graphics.text(font, transStyle(label), x + PAD + 2, ry + 3, -1, false);
            else graphics.text(font, label, x + PAD + 2, ry + 3, color, false);
        }

        boolean mouseClicked(int mx, int my, int btn) {
            if (mx < x || mx >= x + PW) return false;
            if (my >= y && my < y + HEADER_H && btn == 0) {
                dragging = true;
                dox = mx - x;
                doy = my - y;
                return true;
            }
            int ry = y + HEADER_H;
            if (my >= ry && my < ry + MOD_H && btn == 0) {
                mc.gui.setScreen(new HudEditorScreen());
                return true;
            }
            ry += MOD_H;
            if (my >= ry && my < ry + MOD_H) {
                if (btn == 0) {
                    mc.gui.setScreen(new KeybindScreen(ClickGui.this));
                    return true;
                }
                if (btn == 2 || btn == 1) {
                    rebindingKeybind = KeybindManager.getRegisteredKeybinds().get("keybinds");
                    return true;
                }
            }
            ry += MOD_H;
            if (my >= ry && my < ry + MOD_H && btn == 0) {
                mc.gui.setScreen(new ChatMacroScreen(ClickGui.this));
                return true;
            }
            return false;
        }

        void mouseDragged(int mx, int my) {
            if (dragging) {
                x = mx - dox;
                y = my - doy;
                if (hudPanelPosition != null) hudPanelPosition.set(x, y);
            }
        }

        void mouseReleased() {
            dragging = false;
        }
    }

    private class Panel {
        final String category;
        final List<Module> modules;
        int x, y;
        boolean dragging;
        int dox, doy;
        boolean collapsed;
        final Set<String> expanded;
        String sliderMod, sliderSet;
        final List<Setting<?>> settings = new ObjectArrayList<>();

        Panel(String category, List<Module> modules, int x, int y) {
            this.category = category;
            this.modules = modules;

            Vector2i position = categoryPositions.computeIfAbsent(category, _ -> new Vector2i(x, y));
            this.x = position.x();
            this.y = position.y();
            this.expanded = categoryExpandedModules.computeIfAbsent(category, _ -> new ObjectOpenHashSet<>());
            this.collapsed = categoryCollapsed.computeIfAbsent(category, _ -> false);
        }

        private List<Module> getFilteredModules() {
            if (searchQuery.isEmpty()) return modules;
            List<Module> filtered = new ArrayList<>();
            String query = searchQuery.toLowerCase(Locale.ROOT);
            for (Module m : modules) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(query) || m.getIdentifier().toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(m);
                }
            }
            return filtered;
        }

        private void populateSettings(Module module) {
            settings.clear();
            for (Setting<?> s : module.getSettings()) {
                if (s.visible.getAsBoolean()) settings.add(s);
            }
        }

        int height() {
            if (collapsed) return HEADER_H;
            int h = HEADER_H;
            List<Module> filtered = getFilteredModules();
            if (filtered.isEmpty() && !searchQuery.isEmpty()) return HEADER_H + MOD_H;
            for (Module m : filtered) {
                h += MOD_H;
                if (expanded.contains(m.getIdentifier())) {
                    populateSettings(m);
                    if (!settings.isEmpty()) h += settBlockH(settings);
                }
            }
            return h;
        }

        int settBlockH(List<Setting<?>> ss) {
            int h = 4;
            for (Setting<?> s : ss) h += rowHeight(s);
            return h;
        }

        // all settings row height are here, don't use other values
        boolean stringTwoLines(Setting<?> s) {
            int sw = PW - PAD * 2 - 2;
            return s instanceof StringSetting && font.width(s.getName() + ": ") + 30 > sw;
        }

        int rowHeight(Setting<?> s) {
            if (s instanceof FloatSetting || s instanceof IntSetting) return SET_H + SLD_H + 3;
            if (stringTwoLines(s)) return SET_H * 2;
            return SET_H;
        }

        void drawSliderValue(GuiGraphicsExtractor graphics, int sx, int sy, String name, String value, float pct, int sw) {
            String lbl = name + ": ";
            graphics.text(font, lbl, sx, sy, Theme.dim, false);
            graphics.text(font, value, sx + font.width(lbl), sy, Theme.info, false);
            Ui.slider(graphics, sx, sy + SET_H - 2, sw, SLD_H, pct);
        }

        boolean sliderHit(int mx, int my, int sx, int sy, int sw, int btn) {
            return btn == 0 && my >= sy && my <= sy + SET_H - 2 + SLD_H + 2;
        }

        float sliderPct(int mx, int sx, int sw) {
            return (float) (mx - sx) / sw;
        }

        void draw(GuiGraphicsExtractor graphics, int mx, int my) {
            int pw = PW, ph = height();
            Ui.shadow(graphics, x, y, pw, ph);
            Ui.box(graphics, x, y, pw, ph);
            Ui.header(graphics, x, y, pw, HEADER_H);
            if (PRIDE) graphics.text(font, prideStyle(category), x + PAD, y + (HEADER_H - 7) / 2, -1);
            else graphics.text(font, category, x + PAD, y + (HEADER_H - 7) / 2, Theme.text);
            graphics.text(font, collapsed ? "+" : "—", x + pw - 10, y + (HEADER_H - 7) / 2, Theme.faint, false);

            if (collapsed) return;

            int ry = y + HEADER_H;
            List<Module> filtered = getFilteredModules();
            if (filtered.isEmpty() && !searchQuery.isEmpty()) {
                graphics.text(font, "No results", x + PAD, ry + 3, Theme.muted, false);
                return;
            }

            for (Module mod : filtered) {
                boolean hov = mx >= x && mx < x + pw && my >= ry && my < ry + MOD_H;
                boolean on = mod.isEnabled();
                boolean exp = expanded.contains(mod.getIdentifier());
                if (hov) {
                    graphics.fill(x, ry, x + pw, ry + MOD_H, Theme.hover);
                    hoveredDescription = mod.getDescription();
                }
                if (on) graphics.fill(x, ry, x + 2, ry + MOD_H, PRIDE ? C_PRIDE_1 : Theme.accent);
                int tx = x + PAD + (on ? 2 : 0);
                if (on) {
                    if (PRIDE) graphics.text(font, transStyle(mod.getName()), tx, ry + 3, -1);
                    else graphics.text(font, mod.getName(), tx, ry + 3, Theme.accent);
                }
                else graphics.text(font, mod.getName(), tx, ry + 3, Theme.muted, false);

                populateSettings(mod);
                if (!settings.isEmpty())
                    graphics.text(font, exp ? "▾" : "▸", x + pw - 9, ry + 3, Theme.dim, false);
                ry += MOD_H;

                if (exp) {
                    List<Setting<?>> ss = settings;
                    if (!ss.isEmpty()) {
                        int bh = settBlockH(ss);
                        graphics.fill(x, ry, x + pw, ry + bh, Theme.inset);
                        graphics.fill(x, ry, x + 1, ry + bh, Theme.line);
                        graphics.fill(x + pw - 1, ry, x + pw, ry + bh, Theme.line);
                        graphics.fill(x, ry + bh - 1, x + pw, ry + bh, Theme.line);

                        int sy = ry + 3;
                        int sx = x + PAD + 2;
                        int sw = pw - PAD * 2 - 2;

                        for (Setting<?> s : ss) {
                            if (s instanceof FloatSetting fs) {
                                float val = fs.getValue();
                                drawSliderValue(graphics, sx, sy, s.getName(),
                                    (val == (int) val) ? String.valueOf((int) val) : String.format("%.1f", val),
                                    (val - fs.getMin()) / (fs.getMax() - fs.getMin()), sw);
                                sy += rowHeight(s);

                            } else if (s instanceof IntSetting is) {
                                int val = is.getValue();
                                drawSliderValue(graphics, sx, sy, s.getName(), String.valueOf(val),
                                    (float) (val - is.getMin()) / (is.getMax() - is.getMin()), sw);
                                sy += rowHeight(s);

                            } else if (s instanceof BooleanSetting bs) {
                                boolean v = bs.getValue();
                                String lbl = s.getName() + ": ";
                                graphics.text(font, lbl, sx, sy, Theme.dim, false);
                                graphics.text(font, Boolean.toString(v), sx + font.width(lbl), sy, v ? Theme.good : Theme.bad, false);
                                sy += rowHeight(s);

                            } else if (s instanceof ModeSetting ms) {
                                String lbl = s.getName() + ": ";
                                graphics.text(font, lbl, sx, sy, Theme.dim, false);
                                graphics.text(font, ms.getValue(), sx + font.width(lbl), sy, Theme.value, false);
                                sy += rowHeight(s);

                            } else if (s instanceof EnumSetting<?> es) {
                                String lbl = s.getName() + ": ";
                                graphics.text(font, lbl, sx, sy, Theme.dim, false);
                                graphics.text(font, es.getValue().toString(), sx + font.width(lbl), sy, Theme.value, false);
                                sy += rowHeight(s);


                            } else if (s instanceof ButtonSetting bs) {
                                boolean hov2 = mx >= x && mx < x + pw && my >= sy && my < sy + SET_H;
                                if (hov2) graphics.fill(x, sy, x + pw, sy + SET_H, Theme.hover);
                                graphics.fill(x, sy, x + 2, sy + SET_H, Theme.info);
                                graphics.text(font, bs.getName(), sx + 2, sy + 3, Theme.info, false);
                                sy += rowHeight(s);

                            } else if (s instanceof BindSetting bs) {
                                boolean isRebinding = mod.getIdentifier().equals(rebindingModule) && s.getName().equals(focusedSet);
                                String lbl = s.getName() + ": ";
                                String val = isRebinding ? "..." : "[" + bs.getKeyName() + "]";
                                graphics.text(font, lbl, sx, sy, Theme.dim, false);
                                graphics.text(font, val, sx + font.width(lbl), sy, Theme.value, false);
                                sy += rowHeight(s);

                            } else if (s instanceof StringSetting ss2) {
                                boolean focused = isFocused(this, mod.getIdentifier(), s.getName());
                                String lbl = s.getName() + ": ";
                                String val = ss2.getValue();
                                String renderedVal = val.replace('&', '§');
                                int clipRight = sx + sw;
                                boolean twoLines = stringTwoLines(s);
                                int fieldX = twoLines ? sx : sx + font.width(lbl);
                                int fieldY = twoLines ? sy + SET_H : sy;
                                graphics.text(font, lbl, sx, sy, Theme.dim, false);
                                if (focused) {
                                    int clampedCursor = Math.min(cursorPos, val.length());
                                    int avail = Math.max(1, clipRight - fieldX - font.width("|"));
                                    int start = 0;
                                    while (start < clampedCursor && font.width(renderedVal.substring(start, clampedCursor)) > avail) start++;
                                    graphics.fill(fieldX - 1, fieldY - 1, clipRight + 1, fieldY + SET_H - 1, Theme.field);
                                    graphics.enableScissor(fieldX, fieldY - 1, clipRight, fieldY + SET_H);
                                    if (hasSelection()) {
                                        int s1 = fieldX + font.width(renderedVal.substring(start, Math.max(start, Math.min(selStart(), val.length()))));
                                        int s2 = fieldX + font.width(renderedVal.substring(start, Math.max(start, Math.min(selEnd(), val.length()))));
                                        graphics.fill(s1, fieldY - 1, s2, fieldY + SET_H - 1, Theme.selection);
                                    }
                                    graphics.text(font, renderedVal.substring(start), fieldX, fieldY, Theme.text, false);
                                    long now = System.currentTimeMillis();
                                    if (!hasSelection() && (now / 500) % 2 == 0) {
                                        int cursorDrawX = fieldX + font.width(renderedVal.substring(start, clampedCursor));
                                        graphics.text(font, "|", cursorDrawX, fieldY, Theme.accent, false);
                                    }
                                    graphics.disableScissor();
                                } else {
                                    graphics.enableScissor(fieldX, fieldY - 1, clipRight, fieldY + SET_H);
                                    graphics.text(font, renderedVal, fieldX, fieldY, Theme.text, false);
                                    graphics.disableScissor();
                                }
                                sy += rowHeight(s);
                            }
                        }
                        ry += bh;
                    }
                }
            }
        }

        boolean mouseClicked(int mx, int my, int btn) {
            if (mx < x || mx >= x + PW) return false;

            if (my >= y && my < y + HEADER_H && btn == 0) {
                if (mx >= x + PW - 12) {
                    collapsed = !collapsed;
                    categoryCollapsed.put(category, collapsed);
                    return true;
                }
                dragging = true;
                dox = mx - x;
                doy = my - y;
                return true;
            }

            if (collapsed) return false;

            int ry = y + HEADER_H;
            List<Module> filtered = getFilteredModules();
            for (Module mod : filtered) {
                populateSettings(mod);
                if (my >= ry && my < ry + MOD_H) {
                    if (btn == 0) {
                        mod.toggle();
                        return true;
                    }
                    if (btn == 1) {
                        if (!settings.isEmpty()) {
                            toggleExpand(mod.getIdentifier());
                            return true;
                        }
                    }
                    return false;
                }
                ry += MOD_H;

                if (expanded.contains(mod.getIdentifier())) {
                    List<Setting<?>> ss = settings;
                    if (!ss.isEmpty()) {
                        int bh = settBlockH(ss);
                        if (my >= ry && my < ry + bh) {
                            int sy = ry + 3;
                            int sx = x + PAD + 2;
                            int sw = PW - PAD * 2 - 2;
                            for (Setting<?> s : ss) {
                                if (s instanceof FloatSetting fs) {
                                    if (sliderHit(mx, my, sx, sy, sw, btn)) {
                                        fs.setValue(fs.getMin() + sliderPct(mx, sx, sw) * (fs.getMax() - fs.getMin()));
                                        sliderMod = mod.getIdentifier();
                                        sliderSet = fs.getName();
                                        clearFocus();
                                        return true;
                                    }
                                    sy += rowHeight(s);
                                } else if (s instanceof IntSetting is) {
                                    if (sliderHit(mx, my, sx, sy, sw, btn)) {
                                        is.setValue(Math.round(is.getMin() + sliderPct(mx, sx, sw) * (is.getMax() - is.getMin())));
                                        sliderMod = mod.getIdentifier();
                                        sliderSet = is.getName();
                                        clearFocus();
                                        return true;
                                    }
                                    sy += rowHeight(s);
                                } else if (s instanceof BooleanSetting bs) {
                                    if (my >= sy && my < sy + SET_H && (btn == 0 || btn == 1)) {
                                        bs.toggle();
                                        clearFocus();
                                        return true;
                                    }
                                    sy += rowHeight(s);
                                } else if (s instanceof ModeSetting ms) {
                                    if (my >= sy && my < sy + SET_H && (btn == 0 || btn == 1)) {
                                        List<String> opts = ms.getOptions();
                                        int idx = opts.indexOf(ms.getValue());
                                        if (btn == 0) idx = (idx + 1) % opts.size();
                                        else idx = (idx - 1 + opts.size()) % opts.size();
                                        ms.setValue(opts.get(idx));
                                        clearFocus();
                                        return true;
                                    }
                                    sy += rowHeight(s);
                                } else if (s instanceof EnumSetting<?> es) {
                                    if (my >= sy && my < sy + SET_H && (btn == 0 || btn == 1)) {
                                        es.cycle(btn == 0);
                                        clearFocus();
                                        return true;
                                    }
                                    sy += rowHeight(s);
                                } else if (s instanceof ButtonSetting bs) {
                                    if (my >= sy && my < sy + SET_H && (btn == 0 || btn == 1)) {
                                        bs.press();
                                        clearFocus();
                                        return true;
                                    }
                                    sy += rowHeight(s);
                                } else if (s instanceof BindSetting) {
                                    if (my >= sy && my < sy + SET_H && btn == 0) {
                                        setFocus(this, mod.getIdentifier(), s.getName());
                                        rebindingModule = mod.getIdentifier();
                                        return true;
                                    }
                                    sy += rowHeight(s);
                                } else if (s instanceof StringSetting ss2) {
                                    int rowH = rowHeight(s);
                                    if (my >= sy && my < sy + rowH && btn == 0) {
                                        setFocus(this, mod.getIdentifier(), s.getName());
                                        clearSelection();
                                        String val = ss2.getValue();
                                        int fieldX = stringTwoLines(s) ? sx : sx + font.width(s.getName() + ": ");
                                        int avail = Math.max(1, sx + sw - fieldX - font.width("|"));
                                        int cur = Math.min(cursorPos, val.length());
                                        int scrollPx = 0;
                                        while (scrollPx < cur && font.width(val.substring(scrollPx, cur)) > avail) scrollPx++;
                                        scrollPx = font.width(val.substring(0, scrollPx));
                                        int bestPos = val.length(), bestDist = Integer.MAX_VALUE;
                                        for (int i = 0; i <= val.length(); i++) {
                                            int cx = fieldX + font.width(val.substring(0, i)) - scrollPx;
                                            int dist = Math.abs(mx - cx);
                                            if (dist < bestDist) {
                                                bestDist = dist;
                                                bestPos = i;
                                            }
                                        }
                                        cursorPos = bestPos;
                                        return true;
                                    }
                                    sy += rowH;
                                }
                            }
                            clearFocus();
                            return true;
                        }
                        ry += bh;
                    }
                }
            }
            return false;
        }

        void mouseDragged(int mx, int my) {
            if (dragging) {
                x = mx - dox;
                y = my - doy;
                categoryPositions.get(category).set(x, y);
            }
            if (sliderMod != null) {
                Module mod = MainClient.MODULE_MANAGER.getModuleByName(sliderMod);
                if (mod != null) {
                    Setting<?> s = mod.getSettingByName(sliderSet);
                    int sx = x + PAD + 2, sw = PW - PAD * 2 - 2;
                    float pct = (float) (mx - sx) / sw;
                    if (s instanceof FloatSetting fs) fs.setValue(fs.getMin() + pct * (fs.getMax() - fs.getMin()));
                    else if (s instanceof IntSetting is)
                        is.setValue(Math.round(is.getMin() + pct * (is.getMax() - is.getMin())));
                }
            }
        }

        void mouseReleased() {
            dragging = false;
            sliderMod = null;
            sliderSet = null;
        }

        void toggleExpand(String name) {
            if (!expanded.add(name)) expanded.remove(name);
        }
    }

    public ClickGui(Screen parent) {
        super(Component.literal("ClickGUI"), parent instanceof ClickGui ? null : parent, "Modules");
        AccountsScreen.preloadAccounts();
    }

    @Override
    protected Screen tabParent() {
        return this;
    }

    @Override
    protected void init() {
        panels.clear();
        Map<String, List<Module>> cats = new LinkedHashMap<>();
        for (Module m : MainClient.MODULE_MANAGER.modules())
            cats.computeIfAbsent(customCategories.getOrDefault(m.getIdentifier(), customCategories.getOrDefault(m.getName(), m.getCategory())), _ -> new ArrayList<>()).add(m);

        int maxWidth = mc.getWindow().getGuiScaledWidth();
        int col = 0;
        Int2IntMap heights = new Int2IntOpenHashMap();

        for (Map.Entry<String, List<Module>> e : cats.entrySet()) {
            int x = 6 + col * (PW + 4);
            if (x + PW > maxWidth) {
                col = 0;
                x = 6;
            }
            int y = PANEL_TOP + heights.getOrDefault(col, 2);

            Panel panel = new Panel(e.getKey(), e.getValue(), x, y);
            panels.add(panel);

            heights.put(col, panel.y + panel.height());
            col++;
        }

        if (hudPanel == null) {
            int x = 6 + col * (PW + 4);
            if (x + PW > maxWidth) {
                col = 0;
                x = 6;
            }
            int y = PANEL_TOP + heights.getOrDefault(col, 2);

            hudPanel = new HudPanel(x, y);
        }
    }

    private void drawSearchBar(GuiGraphicsExtractor graphics, int mx, int my) {
        int w = PW + 20;
        int h = HEADER_H;
        int x = (this.width - w) / 2;
        int y = this.height - 30;

        graphics.fill(x, y, x + w, y + h, Theme.surface);
        graphics.fill(x, y, x + w, y + 1, Theme.border);
        graphics.fill(x, y + h - 1, x + w, y + h, Theme.border);
        graphics.fill(x, y, x + 1, y + h, Theme.border);
        graphics.fill(x + w - 1, y, x + w, y + h, Theme.border);

        // scroll long queries so the cursor stays inside the bar instead of running off-screen
        int searchCursor = Math.min(cursorPos, searchQuery.length());
        int searchStart = 0;
        while (searchStart < searchCursor && font.width(searchQuery.substring(searchStart, searchCursor)) > w - PAD * 2 - font.width("|")) searchStart++;
        String searchVisible = searchFocused ? searchQuery.substring(searchStart) : searchQuery;

        if (searchFocused) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, Theme.field);
            if (hasSelection()) {
                int s1 = x + PAD + font.width(searchQuery.substring(searchStart, Math.max(searchStart, Math.min(selStart(), searchQuery.length()))));
                int s2 = x + PAD + font.width(searchQuery.substring(searchStart, Math.max(searchStart, Math.min(selEnd(), searchQuery.length()))));
                graphics.fill(s1, y + 2, s2, y + h - 2, Theme.selection);
            }
        }

        String display = searchQuery.isEmpty() && !searchFocused ? "Search Modules..." : searchVisible;
        int color = searchQuery.isEmpty() && !searchFocused ? Theme.muted : Theme.text;
        graphics.enableScissor(x + PAD, y + 1, x + w - PAD, y + h - 1);
        graphics.text(font, display, x + PAD, y + 4, color, false);

        if (searchFocused) {
            long now = System.currentTimeMillis();
            if (!hasSelection() && (now / 500) % 2 == 0) {
                int cursorX = x + PAD + font.width(searchQuery.substring(searchStart, searchCursor));
                graphics.text(font, "|", cursorX, y + 4, Theme.accent, false);
            }
        }
        graphics.disableScissor();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawStructure(graphics, mouseX, mouseY);
        drawSearchBar(graphics, mouseX, mouseY);
        hoveredDescription = null;
        Panel top = null;
        for (Panel p : panels) if (p.dragging) top = p;
        for (Panel p : panels) if (p != top) p.draw(graphics, mouseX, mouseY);
        if (top != null) top.draw(graphics, mouseX, mouseY);
        hudPanel.draw(graphics, mouseX, mouseY);
        if (hoveredDescription != null && !hoveredDescription.isEmpty()) {
            int tw = font.width(hoveredDescription);
            int tx = mouseX + 10, ty = mouseY + 10;
            Ui.box(graphics, tx - 3, ty - 3, tw + 6, 16, Theme.header, Theme.border);
            graphics.text(font, hoveredDescription, tx, ty + 1, Theme.text, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        int btn = click.button();

        if (rebindingKeybind != null) {
            rebindingKeybind.setKeyCode(btn);
            rebindingKeybind = null;
            return true;
        }

        if (rebindingModule != null && focusedSet != null) {
            Module mod = MainClient.MODULE_MANAGER.getModuleByName(rebindingModule);
            if (mod != null) {
                Setting<?> s = mod.getSettingByName(focusedSet);
                if (s instanceof BindSetting bs) {
                    boolean hitAnyPanel = mx >= hudPanel.x && mx <= hudPanel.x + PW && my >= hudPanel.y && my <= hudPanel.y + (HEADER_H + MOD_H * 2);

                    if (!hitAnyPanel) {
                        for (Panel p : panels) {
                            if (mx >= p.x && mx <= p.x + PW && my >= p.y && my <= p.y + p.height()) {
                                hitAnyPanel = true;
                                break;
                            }
                        }
                    }

                    if (!hitAnyPanel) {
                        bs.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
                    } else {
                        bs.setKeyCode(btn);
                    }

                    ConfigManager.save();
                    clearFocus();
                    return true;
                }
            }
            clearFocus();
            return true;
        }

        int sw = PW + 20, sh = HEADER_H;
        int sx = (this.width - sw) / 2;
        int sy = this.height - 30;
        if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + sh) {
            clearFocus();
            searchFocused = true;
            clearSelection();
            int bestPos = searchQuery.length(), bestDist = Integer.MAX_VALUE;
            int startX = sx + PAD;
            for (int i = 0; i <= searchQuery.length(); i++) {
                int cx = startX + font.width(searchQuery.substring(0, i));
                int dist = Math.abs(mx - cx);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestPos = i;
                }
            }
            cursorPos = bestPos;
            return true;
        }

        if (hudPanel.mouseClicked(mx, my, btn)) return true;

        for (int i = panels.size() - 1; i >= 0; i--) {
            if (panels.get(i).mouseClicked(mx, my, btn)) {
                panels.add(panels.remove(i));
                return true;
            }
        }

        clearFocus();
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        hudPanel.mouseDragged((int) click.x(), (int) click.y());
        panels.forEach(p -> p.mouseDragged((int) click.x(), (int) click.y()));
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent click) {
        hudPanel.mouseReleased();
        panels.forEach(Panel::mouseReleased);
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.input();

        if (rebindingKeybind != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                rebindingKeybind.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                rebindingKeybind.setKeyCode(keyCode);
            }
            rebindingKeybind = null;
            return true;
        }

        if (rebindingModule != null) {
            if (focusedSet != null) {
                Module mod = MainClient.MODULE_MANAGER.getModuleByName(rebindingModule);
                if (mod != null) {
                    Setting<?> s = mod.getSettingByName(focusedSet);
                    if (s instanceof BindSetting bs) {
                        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                            bs.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
                        } else {
                            bs.setKeyCode(keyCode);
                        }
                        ConfigManager.save();
                    }
                }
            }

            rebindingModule = null;
            clearFocus();
            return true;
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
            return editKey(searchBuffer(), keyCode);
        }

        if (focusedMod != null && focusedSet != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                clearFocus();
                this.onClose();
                return true;
            }
            Module mod = MainClient.MODULE_MANAGER.getModuleByName(focusedMod);
            if (mod != null) {
                Setting<?> s = mod.getSettingByName(focusedSet);
                if (s instanceof StringSetting ss) {
                    if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                        clearFocus();
                        return true;
                    }
                    return editKey(settingBuffer(ss), keyCode);
                }
            }
            clearFocus();
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent input) {
        if (searchFocused && input.isAllowedChatCharacter()) {
            editChar(searchBuffer(), input.codepointAsString());
            return true;
        }

        if (focusedMod != null && focusedSet != null) {
            Module mod = MainClient.MODULE_MANAGER.getModuleByName(focusedMod);
            if (mod != null) {
                Setting<?> s = mod.getSettingByName(focusedSet);
                if (s instanceof StringSetting ss) {
                    if (input.isAllowedChatCharacter()) editChar(settingBuffer(ss), input.codepointAsString());
                    return true;
                }
            }
            clearFocus();
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }
}