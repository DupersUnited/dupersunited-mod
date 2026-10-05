package wtf.dupers.dupersunited.features.screens.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import static wtf.dupers.dupersunited.MainClient.mc;

public abstract class DuScreen extends Screen {
    protected final Screen parent;
    private final String tab;

    protected DuScreen(Component title, Screen parent, String tab) {
        super(title);
        this.parent = parent;
        this.tab = tab;
    }

    protected Screen tabParent() {
        return parent;
    }

    protected void drawStructure(GuiGraphicsExtractor g, int mx, int my) {
        Ui.dim(g, width, height);
        if (tab != null) Ui.drawTabs(g, font, width, mx, my, tab);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent click, boolean doubled) {
        if (tab != null && Ui.handleTabClick(width, tabParent(), (int) click.x(), (int) click.y(), tab)) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onClose() {
        if (mc != null) mc.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
