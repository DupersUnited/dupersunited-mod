package wtf.dupers.dupersunited.mixin.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import wtf.dupers.dupersunited.modules.misc.PropagandaModule;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin extends AbstractContainerEventHandler {

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        GuiMacro.getInstance().checkScreen((Screen)(Object)this);
    }

    /* Propaganda Module */

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", shift = At.Shift.AFTER))
    private void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        @Nullable PropagandaModule propagandaModule = MainClient.MODULE_MANAGER.getModule(PropagandaModule.class);
        if (propagandaModule != null && propagandaModule.isEnabled()) {
            graphics.nextStratum();
            propagandaModule.render(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        @Nullable PropagandaModule propagandaModule = MainClient.MODULE_MANAGER.getModule(PropagandaModule.class);
        if (propagandaModule != null && propagandaModule.isEnabled()) {
            if (propagandaModule.mouseClicked(click, doubled)) {
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }
}