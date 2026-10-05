package wtf.dupers.dupersunited.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Accessor("leftPos") int dupersunited$getGuiX();
    @Accessor("topPos") int dupersunited$getGuiY();
    @Accessor("imageWidth") int dupersunited$getBackgroundWidth();
    @Accessor("imageHeight") int dupersunited$getBackgroundHeight();
}