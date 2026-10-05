package wtf.dupers.dupersunited.mixin.render;

import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.injection.Redirect;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.NoRenderModule;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Font.class)
public class TextRendererMixin {
    @Redirect(
        method = "getGlyph",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;isObfuscated()Z")
    )
    private boolean dupersunited$plainObfuscatedText(Style instance) {
        NoRenderModule mod = MainClient.MODULE_MANAGER.getModule(NoRenderModule.class);
        if (mod != null && mod.isEnabled() && mod.plainObfuscatedText.getValue()) {
            return false;
        }
        return instance.isObfuscated();
    }
}