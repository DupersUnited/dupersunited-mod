package wtf.dupers.dupersunited.mixin.accessor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiLineEditBox.class)
public interface EditBoxWidgetAccessor {

    @Invoker("<init>")
    static MultiLineEditBox create(
        Font textRenderer,
        int x,
        int y,
        int width,
        int height,
        Component placeholder,
        Component message,
        int textColor,
        boolean textShadow,
        int cursorColor,
        boolean hasBackground,
        boolean hasOverlay
    ) {
        throw new AssertionError();
    }
}