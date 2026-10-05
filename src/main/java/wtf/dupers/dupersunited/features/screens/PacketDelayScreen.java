package wtf.dupers.dupersunited.features.screens;

import wtf.dupers.dupersunited.modules.glitcha.PacketDelayModule;
import wtf.dupers.dupersunited.api.module.settings.BooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

import java.util.*;
import java.util.stream.Collectors;

import static wtf.dupers.dupersunited.MainClient.mc;
import static wtf.dupers.dupersunited.utils.ColorUtil.*;

public class PacketDelayScreen extends Screen {
    private final Screen parent;
    private final PacketDelayModule module;
    private int scrollOffset = 0;
    private EditBox searchField;

    private record PacketPair(BooleanSetting s1, Class<?> c1, BooleanSetting s2, Class<?> c2) {}
    private final List<PacketPair> rows = new ArrayList<>();

    public PacketDelayScreen(Screen parent, PacketDelayModule module) {
        super(Component.literal("DelayPacket Settings"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        searchField = new EditBox(this.font, width / 2 - 150, 62, 300, 16, Component.literal("Search packets..."));
        searchField.setResponder(s -> {
            scrollOffset = 0;
            updateRows();
        });

        this.addRenderableWidget(searchField);
        searchField.setFocused(true);

        updateRows();
    }

    private void updateRows() {
        rows.clear();
        String query = searchField.getValue().toLowerCase(Locale.ROOT);
        Map<BooleanSetting, Class<? extends Packet<?>>> fullMap = module.getPacketSettings();

        List<BooleanSetting> filtered = fullMap.entrySet().stream()
                .filter(entry -> {
                    String displayName = entry.getKey().getName().toLowerCase(Locale.ROOT);
                    String className = entry.getValue().getSimpleName().toLowerCase(Locale.ROOT);
                    return displayName.contains(query) || className.contains(query);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (int i = 0; i < filtered.size(); i += 2) {
            BooleanSetting s1 = filtered.get(i);
            BooleanSetting s2 = (i + 1 < filtered.size()) ? filtered.get(i + 1) : null;
            rows.add(new PacketPair(s1, fullMap.get(s1), s2, s2 != null ? fullMap.get(s2) : null));
        }
        rebuild();
    }

    private void rebuild() {
        this.clearWidgets();
        this.addRenderableWidget(searchField);
        int midX = width / 2;

        this.addRenderableWidget(Button.builder(
                        Component.literal(module.isSelectiveMode() ? "SELECT PACKETS" : "DELAY ALL PACKETS").withColor(module.isSelectiveMode() ? GREEN : RED),
                        btn -> {
                            module.toggleSelectiveMode();
                            if (!module.isSelectiveMode()) {
                                module.getPacketSettings().keySet().forEach(setting -> setting.setValue(false));
                            }
                            updateRows();
                        }
                ).bounds(midX - 105, 40, 130, 18)
                .tooltip(Tooltip.create(module.isSelectiveMode()
                        ? Component.literal("Selective Mode\n").copy().append(Component.literal("Only packets marked PAUSE are held.\nEverything else sends normally.").withStyle(ChatFormatting.GREEN))
                        : Component.literal("Blink All Mode\n").copy().append(Component.literal("Every packet is held regardless of\nthe ALLOW / PAUSE settings below.").withStyle(ChatFormatting.RED))
                )).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Clear Packets").withColor(SUBTEXT),
                btn -> { module.resetSettings(); updateRows(); }
        ).bounds(midX + 30, 40, 100, 18).build());

        int startY = 85;
        int visibleCount = (height - 120) / 22;

        for (int i = 0; i < rows.size(); i++) {
            int screenI = i - scrollOffset;
            if (screenI < 0 || screenI >= visibleCount) continue;

            int y = startY + (screenI * 22);
            PacketPair e = rows.get(i);

            addBtn(midX - 150, y, e.s1);
            if (e.s2 != null) addBtn(midX + 10, y, e.s2);
        }

        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> onClose())
                .bounds(midX - 50, height - 28, 100, 20).build());
    }

    private void addBtn(int x, int y, BooleanSetting s) {
        this.addRenderableWidget(Button.builder(
                Component.literal(s.getValue() ? "ALLOW" : "PAUSE").withColor(s.getValue() ? GREEN : YELLOW),
                btn -> {
                    s.setValue(!s.getValue());
                    rebuild();
                }).bounds(x + 95, y + 2, 45, 14).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        this.extractMenuBackground(ctx);

        ctx.fill(0, 0, width, height, DEEP_SAPPHIRE);
        ctx.fill(0, 0, width, 38, MANTLE);

        ctx.centeredText(font, Component.literal("DELAY PACKETS").withStyle(ChatFormatting.BOLD), width / 2, 8, LAVENDER);

        ctx.centeredText(font,
                Component.literal("Configure which packets are held while blink is active").withColor(SUBTEXT),
                width / 2, 20, SUBTEXT);

        ctx.fill(width / 2 - 160, 80, width / 2 + 160, height - 36, MANTLE);

        searchField.extractRenderState(ctx, mouseX, mouseY, delta);
        int startY = 85;
        int visibleCount = (height - 120) / 22;

        for (int i = 0; i < rows.size(); i++) {
            int screenI = i - scrollOffset;
            if (screenI < 0 || screenI >= visibleCount) continue;

            int y = startY + (screenI * 22);
            PacketPair e = rows.get(i);

            renderPacketLabel(ctx, width / 2 - 145, y, e.s1, e.c1, mouseX, mouseY);
            if (e.s2 != null) renderPacketLabel(ctx, width / 2 + 15, y, e.s2, e.c2, mouseX, mouseY);
        }

        super.extractRenderState(ctx, mouseX, mouseY, delta);
    }

    private void renderPacketLabel(GuiGraphicsExtractor ctx, int x, int y, BooleanSetting s, Class<?> clazz, int mx, int my) {
        ctx.text(font, s.getName(), x, y + 5, s.getValue() ? PALE_NAVY : SUBTEXT);
        if (mx >= x && mx <= x + 90 && my >= y && my <= y + 20) {
            ctx.setComponentTooltipForNextFrame(font, Collections.singletonList(Component.literal(clazz.getSimpleName()).withColor(MAUVE)), mx, my);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int visibleCount = (height - 120) / 22;
        scrollOffset = Math.max(0, Math.min(rows.size() - visibleCount, scrollOffset - (int) v));
        rebuild();
        return true;
    }

    @Override
    public void onClose() {
        module.syncTargets();
        mc.gui.setScreen(parent);
    }
}