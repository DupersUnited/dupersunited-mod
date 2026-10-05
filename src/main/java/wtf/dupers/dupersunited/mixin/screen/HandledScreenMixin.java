package wtf.dupers.dupersunited.mixin.screen;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ContainerInput;
import wtf.dupers.dupersunited.MainClient;
import wtf.dupers.dupersunited.features.macrogui.GuiMacro;
import wtf.dupers.dupersunited.modules.glitcha.GuiUtilsModule;
import wtf.dupers.dupersunited.modules.misc.PropagandaModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Inject(
        method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
        at = @At("HEAD")
    )
    private void dupersunited$onMouseClick(
        Slot slot,
        int slotId,
        int button,
        ContainerInput input,
        CallbackInfo ci
    ) {
        if (!GuiMacro.getInstance().isRecording) return;
        if (slot == null) return;

        GuiMacro.getInstance().recordClick(slot.index, input, button);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void dupersunited$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.input() == 256) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null) {
            for (GuiEventListener child : client.gui.screen().children()) {
                if (child instanceof EditBox tf && tf.isFocused()) {
                    tf.keyPressed(event);
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void onDrawSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        GuiUtilsModule mod = MainClient.MODULE_MANAGER.getModule(GuiUtilsModule.class);
        if (mod == null) return;

        if (mod.isEnabled() && (boolean) mod.getSettingByName("SlotIds").getValue()) {
            mod.drawSlotId(graphics, slot);
            // CommandCat.sendMessage("drawing the slot id twin", true); it did draw
        }
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void dupersunited$renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        @Nullable PropagandaModule propagandaModule = MainClient.MODULE_MANAGER.getModule(PropagandaModule.class);
        if (propagandaModule != null && propagandaModule.isEnabled()) {
            if (propagandaModule.renderTooltip(graphics, mouseX, mouseY)) {
                ci.cancel();
            }
        }
    }
}