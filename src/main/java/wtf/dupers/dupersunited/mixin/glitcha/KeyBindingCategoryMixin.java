package wtf.dupers.dupersunited.mixin.glitcha;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wtf.dupers.dupersunited.SharedVariables;

@Mixin(KeyMapping.Category.class)
public class KeyBindingCategoryMixin {
    // hardcode string to avoid having translation entries
    @Inject(method = "label", at = @At("HEAD"), cancellable = true)
    private void replaceLabel(CallbackInfoReturnable<Component> cir) {
        if ((Object) this == SharedVariables.CATEGORY) {
            cir.setReturnValue(Component.literal("DupersUnited"));
        }
    }
}
