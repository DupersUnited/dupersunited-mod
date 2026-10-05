package wtf.dupers.dupersunited.features.screens.mainmenu;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.ConfigManager;
import wtf.dupers.dupersunited.features.chatmacros.ChatMacro;
import wtf.dupers.dupersunited.features.chatmacros.ChatMacroManager;
import wtf.dupers.dupersunited.api.keybind.Keybind;
import wtf.dupers.dupersunited.keybinds.KeybindManager;
import wtf.dupers.dupersunited.features.screens.ui.DuScreen;
import wtf.dupers.dupersunited.api.module.Module;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static wtf.dupers.dupersunited.features.screens.ui.Theme.*;

public class KeybindScreen extends DuScreen {
    private int scrollOffset = 0;

    private Module listeningModule = null;
    private Keybind listeningKeybind = null;
    private String listeningMacro = null;
    private String listeningChatMacro = null;

    private String searchQuery = "";
    private EditBox searchField;

    private static final int ROW_HEIGHT = 26;
    private static final int START_Y = 66;
    private static final int BTN_WIDTH  = 90;
    private static final int BTN_HEIGHT = 18;

    private sealed interface Row permits Row.Category, Row.ModuleRow, Row.KeybindRow, Row.MacroRow, Row.ChatMacroRow {
        record Category(String label) implements Row {
        }

        record ModuleRow(Module module) implements Row {
        }

        record KeybindRow(Keybind keybind) implements Row {
        }

        record MacroRow(String name, int key) implements Row {
        }

        record ChatMacroRow(String name, int key) implements Row {
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public KeybindScreen(Screen parent) {
        super(Component.literal("Keybinds"), parent, "Keybinds");
    }

    private int panelLeft() {
        return this.width / 2 - 160;
    }

    private int panelRight() {
        return this.width / 2 + 160;
    }

    private int panelBottom()  {
        return this.height - 36;
    }

    private int visibleHeight() {
        return panelBottom() - START_Y;
    }

    private int visibleRows() {
        return visibleHeight() / ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, filteredRows().size() - visibleRows());
    }

    private boolean isListening() {
        return listeningModule != null || listeningKeybind != null || listeningMacro != null || listeningChatMacro != null;
    }


    private List<Row> filteredRows() {
        if (searchQuery.isEmpty()) return rows;
        List<Row> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row instanceof Row.Category) {
                boolean hasMatch = false;
                for (int j = i + 1; j < rows.size(); j++) {
                    Row child = rows.get(j);
                    if (child instanceof Row.Category) break;
                    if (rowMatchesSearch(child)) { hasMatch = true; break; }
                }
                if (hasMatch) result.add(row);
            } else if (rowMatchesSearch(row)) {
                result.add(row);
            }
        }
        return result;
    }

    private boolean rowMatchesSearch(Row row) {
        return switch (row) {
            case Row.ModuleRow(Module m) -> m.getName().toLowerCase(Locale.ROOT).contains(searchQuery) || m.getIdentifier().toLowerCase(Locale.ROOT).contains(searchQuery);
            case Row.KeybindRow(Keybind kb) -> kb.getName().toLowerCase(Locale.ROOT).contains(searchQuery);
            case Row.MacroRow(String name, _) -> name.toLowerCase(Locale.ROOT).contains(searchQuery);
            case Row.ChatMacroRow(String name, _) -> name.toLowerCase(Locale.ROOT).contains(searchQuery);
            default -> false;
        };
    }

    @Override
    protected void init() {
        rows.clear();
        scrollOffset = 0;
        searchQuery = "";
        listeningModule = null;
        listeningKeybind = null;
        listeningMacro = null;
        listeningChatMacro = null;

        Map<String, List<Module>> grouped = new LinkedHashMap<>();
        for (Module m : MainClient.MODULE_MANAGER.modules()) {
            grouped.computeIfAbsent(m.getCategory(), _ -> new ArrayList<>()).add(m);
        }
        for (var entry : grouped.entrySet()) {
            rows.add(new Row.Category(entry.getKey()));
            for (Module m : entry.getValue()) {
                rows.add(new Row.ModuleRow(m));
            }
        }

        Set<String> chatMacroIds = ChatMacroManager.getMacros().keySet();
        List<Keybind> filteredKeybinds = KeybindManager.getRegisteredKeybinds().values().stream()
            .filter(kb -> !chatMacroIds.contains(kb.getName()))
            .toList();
        if (!filteredKeybinds.isEmpty()) {
            rows.add(new Row.Category("Keybinds"));
            for (Keybind kb : filteredKeybinds) rows.add(new Row.KeybindRow(kb));
        }

        if (!ChatMacroManager.getMacros().isEmpty()) {
            rows.add(new Row.Category("Chat Macros"));
            for (ChatMacro cm : ChatMacroManager.getMacros().values()) {
                rows.add(new Row.ChatMacroRow(cm.getName(), cm.getKeyCode()));
            }
        }

        int searchX = panelLeft() + 4;
        int searchW = panelRight() - panelLeft() - 8;
        searchField = new EditBox(font, searchX, 41, searchW, 18, Component.literal("Search..."));
        searchField.setMaxLength(64);
        searchField.setSuggestion("Search keybinds...");
        searchField.setResponder(text -> {
            searchQuery = text.toLowerCase(Locale.ROOT);
            searchField.setSuggestion(text.isEmpty() ? "Search keybinds..." : "");
            scrollOffset = 0;
            rebuildButtons();
        });
        this.addRenderableWidget(searchField);

        rebuildButtons();
    }


    private void rebuildButtons() {
        this.clearWidgets();

        if (searchField != null) this.addRenderableWidget(searchField);

        List<Row> fr = filteredRows();
        int visible  = visibleRows();

        for (int i = 0; i < fr.size(); i++) {
            Row row  = fr.get(i);
            int screenI = i - scrollOffset;
            if (screenI < 0 || screenI >= visible) continue;

            int y = START_Y + screenI * ROW_HEIGHT;
            int btnY = y + (ROW_HEIGHT - BTN_HEIGHT) / 2;
            int btnX = panelRight() - BTN_WIDTH - 6;
            int clearX = panelRight() - BTN_WIDTH - 28;

            if (row instanceof Row.ModuleRow(Module module)) {
                boolean listening = module == listeningModule;
                this.addRenderableWidget(Button.builder(
                    listening ? Component.literal("[ press a key ]") : getKeyText(module.getKeybind()),
                    _ -> {
                        listeningModule = (listeningModule == module) ? null : module;
                        listeningKeybind = null;
                        listeningMacro = null;
                        listeningChatMacro = null;
                        rebuildButtons();
                    }
                ).bounds(btnX, btnY, BTN_WIDTH, BTN_HEIGHT).build());

                if (module.getKeybind() != GLFW.GLFW_KEY_UNKNOWN && !listening) {
                    this.addRenderableWidget(Button.builder(Component.literal("x").withStyle(ChatFormatting.RED), _ -> {
                        module.setKeybind(GLFW.GLFW_KEY_UNKNOWN);
                        ConfigManager.save();
                        rebuildButtons();
                    }).bounds(clearX, btnY, 18, BTN_HEIGHT).build());
                }

            } else if (row instanceof Row.KeybindRow(Keybind keybind)) {
                boolean listening = keybind == listeningKeybind;
                this.addRenderableWidget(Button.builder(
                    listening ? Component.literal("[ press a key ]") : getKeyText(keybind.getKeyCode()),
                    _ -> {
                        listeningKeybind = (listeningKeybind == keybind) ? null : keybind;
                        listeningModule = null;
                        listeningMacro = null;
                        listeningChatMacro = null;
                        rebuildButtons();
                    }
                ).bounds(btnX, btnY, BTN_WIDTH, BTN_HEIGHT).build());

                if (keybind.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN && !listening) {
                    this.addRenderableWidget(Button.builder(Component.literal("x").withStyle(ChatFormatting.RED), _ -> {
                        keybind.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
                        ConfigManager.save();
                        rebuildButtons();
                    }).bounds(clearX, btnY, 18, BTN_HEIGHT).build());
                }

            } else if (row instanceof Row.MacroRow(String name, int key)) {
                boolean listening = name.equals(listeningMacro);
                this.addRenderableWidget(Button.builder(
                    listening ? Component.literal("[ press a key ]") : getKeyText(key),
                    _ -> {
                        listeningMacro = listening ? null : name;
                        listeningModule = null;
                        listeningKeybind = null;
                        listeningChatMacro = null;
                        rebuildButtons();
                    }
                ).bounds(btnX, btnY, BTN_WIDTH, BTN_HEIGHT).build());

                if (key != GLFW.GLFW_KEY_UNKNOWN && !listening) {
                    this.addRenderableWidget(Button.builder(Component.literal("x").withStyle(ChatFormatting.RED), _ -> {
                        //saveMacroKey(name, GLFW.GLFW_KEY_UNKNOWN);
                        init();
                    }).bounds(clearX, btnY, 18, BTN_HEIGHT).build());
                }

            } else if (row instanceof Row.ChatMacroRow(String name, int key)) {
                boolean listening = name.equals(listeningChatMacro);
                this.addRenderableWidget(Button.builder(
                    listening ? Component.literal("[ press a key ]") : getKeyText(key),
                    _ -> {
                        listeningChatMacro = listening ? null : name;
                        listeningModule = null;
                        listeningKeybind = null;
                        listeningMacro = null;
                        rebuildButtons();
                    }
                ).bounds(btnX, btnY, BTN_WIDTH, BTN_HEIGHT).build());

                if (key != GLFW.GLFW_KEY_UNKNOWN && !listening) {
                    this.addRenderableWidget(Button.builder(Component.literal("x").withStyle(ChatFormatting.RED), _ -> {
                        ChatMacroManager.rebind(name, GLFW.GLFW_KEY_UNKNOWN);
                        init();
                    }).bounds(clearX, btnY, 18, BTN_HEIGHT).build());
                }
            }
        }
    }


    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - (int) vertical));
        rebuildButtons();
        return true;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean bl) {
        if (isListening()) {
            int mx = (int) click.x();
            int my = (int) click.y();
            int button = click.button();
            boolean insidePanel = mx >= panelLeft() && mx <= panelRight()
                && my >= START_Y && my <= panelBottom();

            if (!insidePanel) {
                listeningModule = null;
                listeningKeybind = null;
                listeningMacro = null;
                listeningChatMacro = null;
                rebuildButtons();
                return true;
            }

            if (listeningModule != null) {
                listeningModule.setKeybind(button);
                ConfigManager.save();
                listeningModule = null;
            } else if (listeningKeybind != null) {
                listeningKeybind.setKeyCode(button);
                ConfigManager.save();
                listeningKeybind = null;
            } else if (listeningChatMacro != null) {
                ChatMacroManager.rebind(listeningChatMacro, button);
                listeningChatMacro = null;
            }

            init();
            return true;
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent input) {
        if (listeningModule != null) {
            int keyCode = input.key();
            listeningModule.setKeybind(keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            ConfigManager.save();
            listeningModule = null;
            rebuildButtons();
            return true;
        }
        if (listeningKeybind != null) {
            int keyCode = input.key();
            listeningKeybind.setKeyCode(keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            ConfigManager.save();
            listeningKeybind = null;
            rebuildButtons();
            return true;
        }
        if (listeningChatMacro != null) {
            int keyCode = input.key();
            ChatMacroManager.rebind(listeningChatMacro,
                keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            listeningChatMacro = null;
            init();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        drawStructure(graphics, mouseX, mouseY);

        graphics.fill(panelLeft(), 38, panelRight(), 62, border);
        graphics.fill(panelLeft(), 62, panelRight(), 63, edge);

        graphics.fill(panelLeft(), START_Y, panelRight(), panelBottom(), header);

        List<Row> fr = filteredRows();
        int visible = visibleRows();

        for (int i = 0; i < fr.size(); i++) {
            Row row = fr.get(i);
            int screenI = i - scrollOffset;
            if (screenI < 0 || screenI >= visible) continue;

            int y = START_Y + screenI * ROW_HEIGHT;

            if (row instanceof Row.Category(String label)) {
                graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, border);
                graphics.fill(panelLeft(), y + ROW_HEIGHT - 1, panelRight(), y + ROW_HEIGHT, edge);
                graphics.text(this.font,
                    Component.literal("▸ " + label.toUpperCase()),
                    panelLeft() + 8, y + (ROW_HEIGHT - 9) / 2, primary);

            } else if (row instanceof Row.ModuleRow(Module module)) {
                boolean listening = module == listeningModule;
                boolean hovered   = mouseX >= panelLeft() && mouseX <= panelRight()
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;

                if (hovered || listening) graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, border);
                else if (i % 2 == 0) graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, 0x08FFFFFF);

                graphics.text(this.font,
                    Component.literal(module.getName()),
                    panelLeft() + 10, y + (ROW_HEIGHT - 9) / 2,
                    listening ? value : text);

                boolean hasBind = module.getKeybind() != GLFW.GLFW_KEY_UNKNOWN;
                graphics.fill(panelLeft() + 3, y + ROW_HEIGHT / 2 - 2,
                    panelLeft() + 5, y + ROW_HEIGHT / 2 + 2,
                    listening ? value : hasBind ? accent : edge);
                graphics.fill(panelLeft(), y + ROW_HEIGHT - 1, panelRight(), y + ROW_HEIGHT, border);

            } else if (row instanceof Row.KeybindRow(Keybind keybind)) {
                boolean listening = keybind == listeningKeybind;
                boolean hovered   = mouseX >= panelLeft() && mouseX <= panelRight()
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;

                if (hovered || listening) graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, border);
                else if (i % 2 == 0) graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, 0x08FFFFFF);

                graphics.text(this.font,
                    Component.literal(keybind.getName()),
                    panelLeft() + 10, y + (ROW_HEIGHT - 9) / 2,
                    listening ? value : text);

                boolean hasBind = keybind.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN;
                graphics.fill(panelLeft() + 3, y + ROW_HEIGHT / 2 - 2,
                    panelLeft() + 5, y + ROW_HEIGHT / 2 + 2,
                    listening ? value : hasBind ? accent : edge);
                graphics.fill(panelLeft(), y + ROW_HEIGHT - 1, panelRight(), y + ROW_HEIGHT, border);

            } else if (row instanceof Row.MacroRow(String name, int key)) {
                boolean listening = name.equals(listeningMacro);
                boolean hovered   = mouseX >= panelLeft() && mouseX <= panelRight()
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;

                if (hovered || listening) graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, border);
                else if (i % 2 == 0) graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, 0x08FFFFFF);

                graphics.text(this.font,
                    Component.literal(name),
                    panelLeft() + 10, y + (ROW_HEIGHT - 9) / 2,
                    listening ? value : text);

                boolean hasBind = key != GLFW.GLFW_KEY_UNKNOWN;
                graphics.fill(panelLeft() + 3, y + ROW_HEIGHT / 2 - 2,
                    panelLeft() + 5, y + ROW_HEIGHT / 2 + 2,
                    listening ? value : hasBind ? accent : edge);
                graphics.fill(panelLeft(), y + ROW_HEIGHT - 1, panelRight(), y + ROW_HEIGHT, border);

            } else if (row instanceof Row.ChatMacroRow(String name, int key)) {
                boolean listening = name.equals(listeningChatMacro);
                boolean hovered   = mouseX >= panelLeft() && mouseX <= panelRight()
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;

                if (hovered || listening) graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, border);
                else if (i % 2 == 0) graphics.fill(panelLeft(), y, panelRight(), y + ROW_HEIGHT, 0x08FFFFFF);

                graphics.text(this.font,
                    Component.literal(name),
                    panelLeft() + 10, y + (ROW_HEIGHT - 9) / 2,
                    listening ? value : text);

                boolean hasBind = key != GLFW.GLFW_KEY_UNKNOWN;
                graphics.fill(panelLeft() + 3, y + ROW_HEIGHT / 2 - 2,
                    panelLeft() + 5, y + ROW_HEIGHT / 2 + 2,
                    listening ? value : hasBind ? accent : edge);
                graphics.fill(panelLeft(), y + ROW_HEIGHT - 1, panelRight(), y + ROW_HEIGHT, border);
            }
        }

        if (maxScroll() > 0) {
            int totalH = visibleHeight();
            int barH   = Math.max(24, totalH * visible / fr.size());
            int barY   = START_Y + (totalH - barH) * scrollOffset / maxScroll();
            graphics.fill(panelRight() + 2, START_Y, panelRight() + 4, panelBottom(), border);
            graphics.fill(panelRight() + 2, barY, panelRight() + 4, barY + barH, secondary);
        }

        if (fr.isEmpty() && !searchQuery.isEmpty()) {
            graphics.centeredText(this.font,
                Component.literal("No keybinds match \"" + searchField.getValue() + "\""),
                this.width / 2, START_Y + visibleHeight() / 2, dim);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }


    private Component getKeyText(int key) {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return Component.literal("unbound").withStyle(ChatFormatting.WHITE);
        if (key >= GLFW.GLFW_MOUSE_BUTTON_1 && key <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
            String name = switch (key) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "MOUSE LEFT";
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "MOUSE RIGHT";
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MOUSE MIDDLE";
                default -> "MOUSE " + (key + 1);
            };
            return Component.literal(name);
        }

        if (key < 0) {
            int btn = (-key) - 100;
            if (btn >= GLFW.GLFW_MOUSE_BUTTON_1 && btn <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                String name = switch (btn) {
                    case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "MOUSE LEFT";
                    case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "MOUSE RIGHT";
                    case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MOUSE MIDDLE";
                    default -> "MOUSE " + (btn + 1);
                };
                return Component.literal(name);
            }
            return Component.literal("UNKNOWN");
        }
        return Component.literal(InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString().toUpperCase());
    }
}