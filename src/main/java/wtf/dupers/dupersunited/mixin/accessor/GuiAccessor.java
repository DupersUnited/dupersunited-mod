package wtf.dupers.dupersunited.mixin.accessor;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Mutable @Accessor("splashManager")
    void dupersunited$setSplashTextLoader(SplashManager splashTextLoader);
}