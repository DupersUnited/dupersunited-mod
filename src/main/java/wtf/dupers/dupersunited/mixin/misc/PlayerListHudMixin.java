package wtf.dupers.dupersunited.mixin.misc;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.modules.misc.BetterTabModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

import static wtf.dupers.dupersunited.MainClient.mc;

@Mixin(value = PlayerTabOverlay.class, priority = 900)
public abstract class PlayerListHudMixin {
    @Shadow
    protected abstract List<PlayerInfo> getPlayerInfos();

    @WrapOperation(method = "getPlayerInfos", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;limit(J)Ljava/util/stream/Stream;"))
    private <T extends PlayerInfo> Stream<T> replaceLimit(Stream<T> instance, long l, Operation<Stream<T>> original) {
        BetterTabModule module = MainClient.MODULE_MANAGER.getModule(BetterTabModule.class);
        return original.call(instance, module.isEnabled() ? (long) module.tabSize.getValue() : l);
    }

    @ModifyReturnValue(method = "getPlayerInfos", at = @At("RETURN"))
    private List<PlayerInfo> applyScrollOffset(List<PlayerInfo> original) {
        BetterTabModule module = MainClient.MODULE_MANAGER.getModule(BetterTabModule.class);
        if (!module.isEnabled() || original.isEmpty()) return original;

        int max = module.tabSize.getValue();
        int total = original.size();

        module.scrollOffset = Mth.clamp(module.scrollOffset, 0, Math.max(0, total - 1));

        int start = module.scrollOffset;
        int end = Math.min(start + max, total);

        return original.subList(start, end);
    }

    @Inject(method = "getNameForDisplay", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(PlayerInfo entry, CallbackInfoReturnable<Component> info) {
        BetterTabModule module = MainClient.MODULE_MANAGER.getModule(BetterTabModule.class);
        if (module.isEnabled()) info.setReturnValue(module.getPlayerName(entry));
    }

    @ModifyArg(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"), index = 0)
    private int modifyWidth(int width) {
        BetterTabModule module = MainClient.MODULE_MANAGER.getModule(BetterTabModule.class);
        return module.isEnabled() && module.showPing.getValue() ? width + 30 : width;
    }

    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BEFORE))
    private void modifyHeight(CallbackInfo ci, @Local(ordinal = 5) LocalIntRef o, @Local(ordinal = 6) LocalIntRef p) {
        BetterTabModule module = MainClient.MODULE_MANAGER.getModule(BetterTabModule.class);
        if (!module.isEnabled()) return;

        int total = this.getPlayerInfos().size();
        int rows = total;
        int cols = 1;
        while (rows > module.columnHeight.getValue()) {
            rows = (total + ++cols - 1) / cols;
        }

        o.set(rows);
        p.set(cols);
    }

    @Inject(method = "extractPingIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderLatencyIcon(GuiGraphicsExtractor graphics, int width, int x, int y, PlayerInfo entry, CallbackInfo ci) {
        BetterTabModule module = MainClient.MODULE_MANAGER.getModule(BetterTabModule.class);
        if (!module.isEnabled()) return;

        if (!module.showPingIcon.getValue()) ci.cancel();

        if (module.showPing.getValue()) {
            int latency = Mth.clamp(entry.getLatency(), 0, 9999);
            int color = latency < 150 ? 0xFF00E970 :
                    latency < 300 ? 0xFFE7D020 : 0xFFD74238;
            String text = latency + "ms";
            graphics.text(mc.font, text, x + width - mc.font.width(text), y, color);
            ci.cancel();
        }
    }
}