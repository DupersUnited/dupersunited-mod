package wtf.dupers.dupersunited.mixin.misc;

import net.minecraft.client.input.KeyEvent;
import wtf.dupers.dupersunited.keybinds.ClickGuiKeybind;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void dupersunited$onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        ClickGuiKeybind.onKey(event.key(), action);
    }
}