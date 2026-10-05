package wtf.dupers.dupersunited.mixin.glitcha;

import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.render.NickModule;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StringDecomposer.class)
public class TextVisitFactoryMixin {
    @ModifyArg(at = @At(value = "INVOKE",
        target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        ordinal = 0),
        method = {
            "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z"},
        index = 0)
    private static String adjustText(String text) {
        if (MainClient.MODULE_MANAGER == null) return text;
        NickModule mod = MainClient.MODULE_MANAGER.getModule(NickModule.class);
        if (mod == null) return text;
        if (mod.onlyIngame.getValue() && Minecraft.getInstance().player == null) return text;
        return mod.replaceName(text);
    }
}